package me.yusei.tangoplayer.anki;

import android.content.Context;
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
    private static final String MODEL_NAME = "tangoplayer";
    private static final Set<String> TAGS = new HashSet<>(Collections.singletonList("TangoPlayer"));
    // List of field names that will be used in AnkiDroid model
    public static final String[] FIELDS = {"Word", "Meaning", "Sentence", "SentenceMeaning"};
    // Template for the question of each card
    public static final String QFMT1 = "<div class=big>{{Word}}</div><br>{{Sentence}}";
    public static final String QFMT2 = "{{Meaning}}<br><br><div class=small><br>({{SentenceMeaning}})</div>";
    public static final String[] QFMT = {QFMT1, QFMT2};
    // Template for the answer (use identical for both sides)
    static final String AFMT1 = "{{Meaning}}\n" +
            "<br><br>\n" +
            "<a href=\"#\" onclick=\"document.getElementById('hint').style.display='block';return false;\">Sentence Translation</a>\n" +
            "<div id=\"hint\" style=\"display: none\">{{SentenceMeaning}}</div>\n" +
            "<br><br>\n" +
            "<div class=small>{{Tags}}</div>";
    public static final String[] AFMT = {AFMT1, AFMT1};

    public static final String[] CARD_NAMES = {"Japanese>English", "English>Japanese"};
    // CSS to share between all the cards (optional). User will need to install the NotoSans font by themselves
    public static final String CSS = ".card {\n" +
            " font-size: 24px;\n" +
            " text-align: center;\n" +
            " color: black;\n" +
            " background-color: white;\n" +
            " word-wrap: break-word;\n" +
            "}\n" +
            "\n" +
            ".big { font-size: 48px; }\n" +
            ".small { font-size: 18px;}\n";
    public static final long ANKI_DROID_ILLIGAL_STATE_ERROR = -1;


    public AnkiDroidController(Context context, AnkiDroidHelper ankiDroidHelper){
        mContext = context;
        mAnkiDroid = ankiDroidHelper;
    }

    public boolean addCardsToAnkiDroid(String deckName, List<Map<String, String>> data){
        long deckId =getDeckId(deckName);
        if(deckId == ANKI_DROID_ILLIGAL_STATE_ERROR){
            //Avoid crash cause by AnkiDroid doesn't have permission of read/write the storage,
            Toast.makeText(mContext, mContext.getResources().getText(R.string.error_msg_no_acccess_to_anki_db), Toast.LENGTH_SHORT).show();
            return false;
        }
        long modelId = getModelId(deckName);
        String[] fieldNames = mAnkiDroid.getApi().getFieldList(modelId);
        // Build list of fields and tags
        LinkedList<String []> fields = new LinkedList<>();
        LinkedList<Set<String>> tags = new LinkedList<>();
        for (Map<String, String> fieldMap: data) {
            // Build a field map accounting for the fact that the user could have changed the fields in the model
            String[] flds = new String[fieldNames.length];
            for (int i = 0; i < flds.length; i++) {
                // Fill up the fields one-by-one until either all fields are filled or we run out of fields to send
                if (i < FIELDS.length) {
                    flds[i] = fieldMap.get(FIELDS[i]);
                }
            }
            tags.add(TAGS);
            fields.add(flds);
        }
        // Remove any duplicates from the LinkedLists and then add over the API
        mAnkiDroid.removeDuplicates(fields, tags, modelId);
        if(mAnkiDroid.getApi().addNotes(modelId, deckId, fields, tags) != 1){
            return false;
        }
        Toast.makeText(mContext, mContext.getResources().getString(R.string.info_msg_anki_card_is_added), Toast.LENGTH_SHORT).show();
        return true;
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

    private long getModelId(String deckName) {
        Long mid;

        try{
            mid = mAnkiDroid.findModelIdByName(MODEL_NAME, MODEL_NAME.length());
        }catch (IllegalStateException ise){
            return ANKI_DROID_ILLIGAL_STATE_ERROR;
        }

        if (mid == null) {
            mid = mAnkiDroid.getApi().addNewCustomModel(MODEL_NAME, FIELDS,
                    CARD_NAMES, QFMT, AFMT, CSS, getDeckId(deckName), null);
            mAnkiDroid.storeModelReference(MODEL_NAME, mid);
        }
        return mid;
    }

}
