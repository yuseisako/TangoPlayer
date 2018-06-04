package me.yusei.tangoplayer;

import android.os.AsyncTask;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Map;

public class TranslateTask extends AsyncTask<String, String, String> {
    private TranslateTaskCallback mTranslateTaskCallback = null;

    public TranslateTask(TranslateTaskCallback readSrtFileTaskCallback){
        mTranslateTaskCallback = readSrtFileTaskCallback;
    }

    @Override
    protected void onPostExecute(String result) {
        super.onPostExecute(result);
        this.mTranslateTaskCallback.onPostExecute(result);
    }

    @Override
    protected String doInBackground(String... params) {
        Translator translator = new Translator();
        String word = "";
        try {
            word = translator.callUrlAndParseResult("en", "ja", params[0]);
        } catch (Exception e) {
            e.printStackTrace();
        }
        if(!word.isEmpty()){
            return word;
        }
        return null;
    }
}
