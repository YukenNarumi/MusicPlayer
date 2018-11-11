package com.example.yuken.musicplayer;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import java.util.HashMap;

public class ImageCache {
    private static HashMap<String, Bitmap> cache = new HashMap<>();

    /**
     * デフォルト画像のID取得
     *
     * @return ID
     */
    private static int GetDefaultID() {
        return R.drawable.dummy_album_art_slim;
    }

    /**
     * 反転した色のデフォルト画像のID取得
     *
     * @return ID
     */
    public static int GetGrayDefaultID() {
        return R.drawable.dummy_album_art_slim_gray;
    }

    /**
     * 文字列に変換したデフォルト画像のID取得
     *
     * @return 文字列に変換したID
     */
    public static String GetDefaultPath() {
        return String.valueOf(GetDefaultID());
    }

    /**
     * デフォルト画像がキャッシュされていない場合キャッシュする
     *
     * @param res イメージデータを含むリソースオブジェクト
     */
    public static void CacheDefault(Resources res) {
        String path   = GetDefaultPath();
        Bitmap bitmap = ImageCache.getImage(path);

        if (bitmap != null) {
            return;
        }

        bitmap = BitmapFactory.decodeResource(res, GetDefaultID());
        ImageCache.setImage(path, bitmap);
    }

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
