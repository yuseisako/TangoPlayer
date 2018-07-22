package me.yusei.common;


import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.LinkedHashMap;

public class VideoPlayerConfig {
    private static final String VIDEO_FILE_PATH = "VIDEO_FILE_PATH";
    private static final String LAST_PLAYED_VIDEO_FILE_PATH = "LAST_PLAYED_VIDEO_FILE_PATH";
    private static final String MUSIC_VOLUME = "MUSIC_VOLUME";
    public static final String LOCATION = "VideoPlayerActivity.location";

    /* ===============================================================
       Getter & Setter
     =============================================================== */

    public static Boolean isPlayedFilePath(@NonNull Context context, @NonNull String mFilePath){
        LinkedHashMap<String, PlayVideoInformation> linkedHashMap = getVideoFilePathHashMap(context);
        return !(linkedHashMap==null || linkedHashMap.isEmpty()) && linkedHashMap.containsKey(mFilePath);
    }

    public static void setPlayVideoInformation(@NonNull Context context,@NonNull String mFilePath){
        setPlayVideoInformation(context, mFilePath, 0, 0);
    }

    public static void setPlayVideoInformation(@NonNull Context context,@NonNull  String mFilePath, long position, int delay) {
        setLastPlayedVideoFilePath(context, mFilePath);

        LinkedHashMap<String, PlayVideoInformation> linkedHashMap = getVideoFilePathHashMap(context);
        if(linkedHashMap==null){
            linkedHashMap = new LinkedHashMap<>();
        }
        PlayVideoInformation playVideoInformation = new PlayVideoInformation();
        playVideoInformation.position = position;
        playVideoInformation.delay = delay;
        linkedHashMap.put(mFilePath, playVideoInformation);
        Gson gson = new Gson();
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(VIDEO_FILE_PATH, gson.toJson(linkedHashMap));
        editor.apply();
    }

    @Nullable
    public static PlayVideoInformation getPlayVideoInformation(@NonNull Context context,@NonNull  String filePath){
        LinkedHashMap<String, PlayVideoInformation> linkedHashMap = getVideoFilePathHashMap(context);
        if(linkedHashMap==null || linkedHashMap.isEmpty() || ! linkedHashMap.containsKey(filePath))
            return null;
        return linkedHashMap.get(filePath);
    }

    private static void setLastPlayedVideoFilePath(@NonNull Context context,@NonNull String mFilePath){
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(LAST_PLAYED_VIDEO_FILE_PATH, mFilePath);
        editor.apply();
    }

    @Nullable
    public static String getLastPlayedVideoFilePath(@NonNull Context context) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        return sharedPreferences.getString(LAST_PLAYED_VIDEO_FILE_PATH,null);
    }

    @Nullable
    private static LinkedHashMap<String, PlayVideoInformation> getVideoFilePathHashMap(@NonNull Context context){
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        String filePath = sharedPreferences.getString(VIDEO_FILE_PATH, "");
        if(filePath.isEmpty())
            return null;
        Gson gson = new Gson();
        Type listType = new TypeToken<LinkedHashMap<String, PlayVideoInformation>>() { }. getType();
        LinkedHashMap<String, PlayVideoInformation> linkedHashMap = gson.fromJson(filePath, listType);

        if(linkedHashMap==null || linkedHashMap.isEmpty())
            return null;

        return linkedHashMap;
    }

    public static void setMusicVolume(@NonNull Context context, int volume){
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putInt(MUSIC_VOLUME, volume);
        editor.apply();
    }

    public static int getMusicVolume(@NonNull Context context) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        return sharedPreferences.getInt(MUSIC_VOLUME,-1);
    }

}
