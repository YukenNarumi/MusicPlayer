package com.example.yuken.musicplayer;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.provider.MediaStore;

import java.util.ArrayList;
import java.util.List;

public class Artist {
    public long   id;
    public String artist;
    public String artistKey;
    public int    albums;
    public int    tracks;

    /**
     * 検索時に使用する取得する列名(カラム名、フィールド名)の配列
     */
    private static final String[] FILLED_PROJECTION = {
        MediaStore.Audio.Artists._ID,
        MediaStore.Audio.Artists.ARTIST,
        MediaStore.Audio.Artists.ARTIST_KEY,
        MediaStore.Audio.Artists.NUMBER_OF_ALBUMS,
        MediaStore.Audio.Artists.NUMBER_OF_TRACKS
    };

    /**
     * デフォルトコンストラクタ
     *
     * @param cursor データベースクエリの検索結果
     */
    private Artist(Cursor cursor) {
        id = cursor.getLong(cursor.getColumnIndex(MediaStore.Audio.Artists._ID));
        artist = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Artists.ARTIST));
        artistKey = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Artists.ARTIST_KEY));
        albums = cursor.getInt(cursor.getColumnIndex(MediaStore.Audio.Artists.NUMBER_OF_ALBUMS));
        tracks = cursor.getInt(cursor.getColumnIndex(MediaStore.Audio.Artists.NUMBER_OF_TRACKS));
    }

    /**
     * 検索条件にヒットしアーティスト一覧取得
     *
     * @param activity ダイアログを開いているactivity
     * @return ヒットしたアーティスト一覧
     */
    public static List getItems(Context activity) {
        ContentResolver resolver = activity.getContentResolver();
        Cursor cursor = resolver.query(MediaStore.Audio.Artists.EXTERNAL_CONTENT_URI,
                                       Artist.FILLED_PROJECTION,
                                       null,
                                       null,
                                       "ARTIST  ASC"
        );

        List artists = new ArrayList();
        if (cursor == null) {
            return artists;
        }

        while (cursor.moveToNext()) {
            artists.add(new Artist(cursor));
        }
        cursor.close();
        return artists;
    }
}
