package me.yusei.tangoplayer;

/**
 * Created by yuseisako on 2017/11/13.
 */

public interface AsyncCallback {

    void onPreExecute();
    void onPostExecute(TimedTextObject result);
    void onProgressUpdate(int progress);
    void onCancelled();

}