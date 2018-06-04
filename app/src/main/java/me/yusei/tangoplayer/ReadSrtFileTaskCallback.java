package me.yusei.tangoplayer;

import me.yusei.tangoplayer.TimedTextObject;

/**
 * Created by yuseisako on 2017/11/13.
 */

public interface ReadSrtFileTaskCallback {

    void onPreExecute();
    void onPostExecute(TimedTextObject timedTextObject);
    void onProgressUpdate(int progress);
    void onCancelled();

}