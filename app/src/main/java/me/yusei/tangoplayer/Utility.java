package me.yusei.tangoplayer;

import android.util.Log;

/**
 * Created by yuseisako on 2017/11/11.
 */


public class Utility {

    /**
     * Null checker
     * @param o Object
     * @param <T> Type
     * http://yuki312.blogspot.jp/2016/02/android-nonnull.html
     * @return
     */
    public static <T> T nonNull(T o) {
        if (o == null) {
            throw new NullPointerException("Require Non null object");
        }
        return o;
    }

    /**
     * Logger
     * @param text
     * https://qiita.com/niusounds/items/ec34dfcdb6eed448dc87
     */
    public static void errorLog(String text) {
        StackTraceElement elem = Thread.currentThread().getStackTrace()[2];
        String tag = elem.getFileName();
        Log.e(tag, text);
    }


    /**
     * Logger
     * @param text
     * https://qiita.com/niusounds/items/ec34dfcdb6eed448dc87
     */
    public static void infoLog(String text) {
        StackTraceElement elem = Thread.currentThread().getStackTrace()[2];
        String tag = elem.getFileName();
        Log.i(tag, text);
    }

}
