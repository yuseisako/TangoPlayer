package com.nbsp.materialfilepicker.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import java.io.File;
import java.io.FileFilter;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created by Dimorinny on 24.10.15.
 */
public class FileUtils {
    private static final String ACCESSED_VIDEO_FILE_PATH = "ACCESSED_VIDEO_FILE_PATH";

    public static List<File> getFileListByDirPath(String path, FileFilter filter) {
        File directory = new File(path);
        File[] files = directory.listFiles(filter);

        if (files == null) {
            return new ArrayList<>();
        }

        List<File> result = Arrays.asList(files);
        Collections.sort(result, new FileComparator());
        return result;
    }

    public static String cutLastSegmentOfPath(String path) {
        if (path.length() - path.replace("/", "").length() <= 1)
            return "/";
        String newPath = path.substring(0, path.lastIndexOf("/"));
        // We don't need to list the content of /storage/emulated
        if (newPath.equals("/storage/emulated"))
            newPath = "/storage";
        return newPath;
    }

    public static String getReadableFileSize(long size) {
        if (size <= 0) return "0";
        final String[] units = new String[]{"B", "KB", "MB", "GB", "TB"};
        int digitGroups = (int) (Math.log10(size) / Math.log10(1024));
        return new DecimalFormat("#").format(size / Math.pow(1024, digitGroups)) + " " + units[digitGroups];
    }


    public static Set<String> getAccessedVideoFilePath(Context context){
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        return sharedPreferences.getStringSet(ACCESSED_VIDEO_FILE_PATH, new HashSet<String>());
    }

    public static Boolean isAccessedVideoFilePath(Context context, String mFilePath){
        Set<String> accessedVideoFilePaths = getAccessedVideoFilePath(context);
        for(String accessedVideoFilePath: accessedVideoFilePaths){
            if(accessedVideoFilePath.compareTo(mFilePath) == 0 ){
                return true;
            }
        }
        return false;
    }
}
