package com.example.yuken.musicplayer;

import android.util.Log;

public class DebugLog {
    /**
     * @return デバッグモードの有無
     */
    private static boolean IsDebugMode() {
        return BuildConfig.DEBUG;
    }

    /**
     * 難読化時のエラー対策:ログ出力
     *
     * @param tag タグ名
     * @param msg 本文
     */
    public static void v(String tag, String msg) {
        if (IsDebugMode()) {
            Log.v(tag, msg);
        }
    }

    /**
     * 難読化時のエラー対策:ログ出力
     *
     * @param tag タグ名
     * @param msg 本文
     */
    public static void d(String tag, String msg) {
        if (IsDebugMode()) {
            Log.d(tag, msg);
        }
    }

    /**
     * 難読化時のエラー対策:ログ出力
     *
     * @param tag タグ名
     * @param msg 本文
     */
    public static void i(String tag, String msg) {
        if (IsDebugMode()) {
            Log.i(tag, msg);
        }
    }

    /**
     * 難読化時のエラー対策:ログ出力
     *
     * @param tag タグ名
     * @param msg 本文
     */
    public static void w(String tag, String msg) {
        if (IsDebugMode()) {
            Log.w(tag, msg);
        }
    }

    /**
     * 難読化時のエラー対策:ログ出力
     *
     * @param tag タグ名
     * @param msg 本文
     */
    public static void e(String tag, String msg) {
        if (IsDebugMode()) {
            Log.e(tag, msg);
        }
    }

    /**
     * 難読化時のエラー対策:ログ出力
     *
     * @param tag タグ名
     * @param msg 本文
     */
    public static void wtf(String tag, String msg) {
        if (IsDebugMode()) {
            Log.wtf(tag, msg);
        }
    }
}
