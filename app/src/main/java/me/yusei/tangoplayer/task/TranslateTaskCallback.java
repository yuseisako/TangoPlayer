package me.yusei.tangoplayer.task;

/**
 * Created by yuseisako on 2017/11/13.
 */

public interface TranslateTaskCallback {

    void onPreExecute();
    void onPostExecute(String result);
    void onProgressUpdate(int progress);
    void onCancelled();

}