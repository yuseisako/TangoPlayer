package me.yusei.tangoplayer.task;

import android.os.AsyncTask;

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
            word = translator.callUrlAndParseResult("en", params[0], params[1]);
        } catch (Exception e) {
            e.printStackTrace();
        }
        if(!word.isEmpty()){
            return word;
        }
        return null;
    }
}
