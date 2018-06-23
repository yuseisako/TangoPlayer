package me.yusei.tangoplayer;


import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import java.io.File;

public class VideoPlayerConfig {
    private static final String VIDEO_FILE_PATH = "VIDEO_FILE_PATH";
    private static final String SUBTITLE_FILE_PATH = "SUBTITLE_FILE_PATH";
    private static final String VIDEO_DELAY = "VIDEO_DELAY";
    private static final String VIDEO_DURATION = "VIDEO_DURATION";
    public static final String LOCATION = "VideoPlayerActivity.location";

    public static final int OVERLAY_TIMEOUT = 4000;
    public static final int OVERLAY_INFINITE = 3600000;
    public static final int FADE_OUT = 1;
    public static final int SHOW_PROGRESS = 2;
    public static final int SURFACE_SIZE = 3;
    public static final int AUDIO_SERVICE_CONNECTION_SUCCESS = 5;
    public static final int AUDIO_SERVICE_CONNECTION_FAILED = 6;
    public static final int FADE_OUT_INFO = 4;

    /* ===============================================================
       Getter & Setter
     =============================================================== */

    static String getVideoFileName(Context context){
        String videoFilePath = getVideoFilePath(context);
        if(videoFilePath == null){
            return null;
        }
        File videoFile = new File(videoFilePath);
        return videoFile.getName();
    }

    /**
     * return video file path from SP.
     * @return video file path or "" if nothing.
     */
    static String getVideoFilePath(Context context) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        String filePath = sharedPreferences.getString(VIDEO_FILE_PATH, "");
        if (filePath.isEmpty()) {
            return null;
        } else {
            //TODO: try-catch?
            return filePath;
        }
    }

    /**
     * set video file path to SP.
     * @param mFilePath video file path
     */
    static void setVideoFilePath(Context context, String mFilePath) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(VIDEO_FILE_PATH, mFilePath);
        editor.apply();
    }

    /**
     * get subtitles file path from SP.
     * @return subtitles filePath or null if nothing.
     */
    static String getSubtitleFilePath(Context context) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        String filePath = sharedPreferences.getString(SUBTITLE_FILE_PATH, "");
        if (filePath.isEmpty()) {
            return null;
        } else {
            //TODO: try-catch?
            return filePath;
        }
    }

    /**
     * set video file path to SP
     * @param mFilePath video file
     */
    static void setSubtitleFilePath(Context context, String mFilePath) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(SUBTITLE_FILE_PATH, mFilePath);
        editor.apply();
    }

    /**
     * Get duration from SP
     * @return long duration
     */
    static long getVideoPosition(Context context) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        return sharedPreferences.getLong(VIDEO_DURATION, 0);
    }

    /**
     * Save duration to SP
     * @param duration video duration
     */
    static void setVideoPosition(Context context, long duration) {
        if (duration >= 0) {
            SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putLong(VIDEO_DURATION, duration);
            editor.apply();
        }
    }

    /**
     * Get duration from SP
     * @return long duration
     */
    static int getVideoDelay(Context context) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        return sharedPreferences.getInt(VIDEO_DELAY, 0);
    }

    /**
     * Save duration to SP
     * @param delay video duration
     */
    static void setVideoDelay(Context context, int delay) {
        if (delay >= 0) {
            SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putInt(VIDEO_DELAY, delay);
            editor.apply();
        }
    }
}
