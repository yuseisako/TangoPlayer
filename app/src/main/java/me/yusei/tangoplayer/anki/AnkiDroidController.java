package me.yusei.tangoplayer.anki;

import android.content.Context;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class AnkiDroidController {
    //AnkiDroid API
    private AnkiDroidHelper mAnkiDroid;
    private Context mContext;
    private static final String MODEL_NAME = "tangoplayer";
    private static final Set<String> TAGS = new HashSet<>(Collections.singletonList("TangoPlayer"));
    // List of field names that will be used in AnkiDroid model
    private static final String[] FIELDS = {"Sentences","Meaning"};


    public AnkiDroidController(Context context, AnkiDroidHelper ankiDroidHelper){
        mContext = context;
        mAnkiDroid = ankiDroidHelper;
    }

    public boolean addCardsToAnkiDroid(String deckName, String cardFront, String cardBack){
        List<Map<String, String>> data = new ArrayList<>();
        Map<String, String> hm = new HashMap<>();
        hm.put(FIELDS[0], cardFront);
        hm.put(FIELDS[1], cardBack);
        data.add(hm);

        long deckId =getDeckId(deckName);
        long modelId = getModelId();
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
        int added = mAnkiDroid.getApi().addNotes(modelId, deckId, fields, tags);
        Toast.makeText(mContext, "card is added", Toast.LENGTH_SHORT).show();
        if(added != 1){
            return false;
        }
        return true;
    }

    private long getDeckId(String deckName) {
        Long did = mAnkiDroid.findDeckIdByName(deckName);
        if (did == null) {
            did = mAnkiDroid.getApi().addNewDeck(deckName);
            mAnkiDroid.storeDeckReference(deckName, did);
        }
        return did;
    }

    private long getModelId() {
        Long mid = mAnkiDroid.findModelIdByName(MODEL_NAME, MODEL_NAME.length());
        if (mid == null) {
            mid = mAnkiDroid.getApi().addNewBasicModel(MODEL_NAME);
            mAnkiDroid.storeModelReference(MODEL_NAME, mid);
        }
        return mid;
    }

}
