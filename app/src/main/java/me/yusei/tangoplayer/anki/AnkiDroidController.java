package me.yusei.tangoplayer.anki;

import android.content.Context;
import android.support.annotation.NonNull;
import android.widget.Toast;

import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import me.yusei.tangoplayer.R;

public class AnkiDroidController {
    //AnkiDroid API
    private AnkiDroidHelper mAnkiDroid;
    private Context mContext;
    private static final String MODEL_NAME = "TangoPlayer";

    private static final Set<String> TAGS = new HashSet<>(Collections.singletonList("TangoPlayer"));
    // List of field names that will be used in AnkiDroid model -- Translation ON
    public static final String[] FIELDS_Translation = {"Word", "Meaning", "Sentence", "SentenceMeaning", "Note"};

    // List of field names that will be used in AnkiDroid model -- Translation OFF
    public static final String[] FIELDS_NonTranslation = {"Word", "Meaning", "Sentence", "Note"};

    // Template for the question of each card -- Translation ON
    private static final String QFMT1 = "<div class=big>{{Word}}</div><br>{{Sentence}}";
    private static final String QFMT2 = "<div class=big>{{Meaning}}</div><br>{{SentenceMeaning}}<br>{{Note}}";
    private static final String[] QFMT_Translation = {QFMT1, QFMT2};

    // Templete for the question of each card -- Translation OFF
    private static final String QFMT3 = "<div class=big>{{Meaning}}</div><br>{{Note}}";
    private static final String[] QFMT_NonTranslation = {QFMT1, QFMT3};

    // Template for the answer (use identical for both sides) -- Translation ON
    private static final String AFMT1 = "<div class=big>{{Word}}</div>{{Meaning}}<br><br>\n" +
            "<div class=small>{{Sentence}}<br>{{SentenceMeaning}}</div>" +
            "<br>{{Note}}<br>\n" +
            "<div class=small>TAG: {{Tags}}</div>";
    private static final String[] AFMT_Translation = {AFMT1, AFMT1};

    // Template for the answer (use identical for both sides) -- Translation OFF
    private static final String AFMT2 = "<div class=big>{{Word}}</div>{{Meaning}}<br><br>\n" +
            "<div class=small>{{Sentence}}</div>" +
            "<br>{{Note}}<br>\n" +
            "<div class=small>TAG: {{Tags}}</div>";
    private static final String[] AFMT_NonTranslation = {AFMT2, AFMT2};

    private static String[] cardName = {"Answer the meaning", "Answer the new vocabulary"};

    // CSS to share between all the cards (optional). User will need to install the NotoSans font by themselves
    private static final String CSS = ".card {\n" +
            " font-size: 24px;\n" +
            " text-align: center;\n" +
            " color: black;\n" +
            " background-color: white;\n" +
            " word-wrap: break-word;\n" +
            "}\n" +
            "\n" +
            ".big { font-size: 48px; }\n" +
            ".small { font-size: 18px;}\n";
    private static final long ANKI_DROID_ILLIGAL_STATE_ERROR = -1;


    public AnkiDroidController(Context context, AnkiDroidHelper ankiDroidHelper){
        mContext = context;
        mAnkiDroid = ankiDroidHelper;
    }

    /**
     *
     * @param deckName
     * @param data
     * @param translationLanguage put "None" if NonTranslation
     */
    public void addCardsToAnkiDroid(@NonNull String deckName, List<Map<String, String>> data, @NonNull String translationLanguage){
        if(deckName.isEmpty() || translationLanguage.isEmpty()){
            return;
        }
        long deckId =getDeckId(deckName);
        if(deckId == ANKI_DROID_ILLIGAL_STATE_ERROR){
            //Avoid crash cause by AnkiDroid doesn't have permission of read/write the storage,
            Toast.makeText(mContext, mContext.getResources().getText(R.string.error_msg_no_acccess_to_anki_db), Toast.LENGTH_SHORT).show();
            return;
        }

        Boolean isTranslation = false;
        if(translationLanguage.compareTo("None") != 0){
            isTranslation = true;
        }

//        //TODO: Limiting original language to English. Detect language.
//        cardName[0] = "English>" + translationLanguage;
//        cardName[1] = translationLanguage + ">English";

        long modelId = getModelId(deckName, isTranslation);
        String[] fieldNames = mAnkiDroid.getApi().getFieldList(modelId);
        // Build list of fields and tags
        LinkedList<String []> fields = new LinkedList<>();
        LinkedList<Set<String>> tags = new LinkedList<>();
        for (Map<String, String> fieldMap: data) {
            // Build a field map accounting for the fact that the user could have changed the fields in the model
            String[] flds = new String[fieldNames.length];
            for (int i = 0; i < flds.length; i++) {
                // Fill up the fields one-by-one until either all fields are filled or we run out of fields to send
                if(isTranslation){
                    if (i < FIELDS_Translation.length) {
                        flds[i] = fieldMap.get(FIELDS_Translation[i]);
                    }
                }else{
                    if (i < FIELDS_NonTranslation.length) {
                        flds[i] = fieldMap.get(FIELDS_NonTranslation[i]);
                    }
                }
            }
            tags.add(TAGS);
            fields.add(flds);
        }
        // Remove any duplicates from the LinkedLists and then add over the API
        mAnkiDroid.removeDuplicates(fields, tags, modelId);
        if(mAnkiDroid.getApi().addNotes(modelId, deckId, fields, tags) != 1){
            return;
        }
        Toast.makeText(mContext, mContext.getResources().getString(R.string.info_msg_anki_card_is_added), Toast.LENGTH_SHORT).show();
    }

    private long getDeckId(String deckName){
        Long did;

        try{
            did = mAnkiDroid.findDeckIdByName(deckName);
            if (did == null) {
                did = mAnkiDroid.getApi().addNewDeck(deckName);
                mAnkiDroid.storeDeckReference(deckName, did);
            }
        }catch (IllegalStateException ise){
            return ANKI_DROID_ILLIGAL_STATE_ERROR;
        }

        return did;
    }

    private long getModelId(String deckName, Boolean isTranslation) {
        Long mid;

        try{
            mid = mAnkiDroid.findModelIdByName(MODEL_NAME, MODEL_NAME.length());
        }catch (IllegalStateException ise){
            return ANKI_DROID_ILLIGAL_STATE_ERROR;
        }

        if (mid == null) {
            if(isTranslation) {
                mid = mAnkiDroid.getApi().addNewCustomModel(MODEL_NAME, FIELDS_Translation,
                        cardName, QFMT_Translation, AFMT_Translation, CSS, getDeckId(deckName), null);
                mAnkiDroid.storeModelReference(MODEL_NAME, mid);
            }else {
                mid = mAnkiDroid.getApi().addNewCustomModel(MODEL_NAME, FIELDS_NonTranslation,
                        cardName, QFMT_NonTranslation, AFMT_NonTranslation, CSS, getDeckId(deckName), null);
                mAnkiDroid.storeModelReference(MODEL_NAME, mid);
            }
        }
        return mid;
    }

}
