package me.yusei.tangoplayer.task;

import me.yusei.tangoplayer.subtitle.TimedTextObject;

/**
 * Created by yuseisako on 2017/11/13.
 */

public interface ReadSubtitleFileTaskCallback {

    void onPreExecute();
    void onPostExecute(TimedTextObject timedTextObject);
    void onProgressUpdate(int progress);
    void onCancelled();

}