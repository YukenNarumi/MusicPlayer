package com.example.yuken.musicplayer;

import android.graphics.Bitmap;

import java.util.HashMap;

public class ImageCache {
    private static HashMap<String, Bitmap> cache = new HashMap<>();

    /**
     * キャッシュされている画像を取得
     *
     * @param key キャッシュのキー
     * @return キャッシュされている場合画像 / されていない場合 null
     */
    public static Bitmap getImage(String key) {
        if (cache.containsKey(key)) {
            return cache.get(key);
        }
        return null;
    }

    /**
     * 画像をキャッシュする
     *
     * @param key   キャッシュのキー
     * @param image キャッシュする画像
     */
    public static void setImage(String key, Bitmap image) {
        cache.put(key, image);
    }
}
