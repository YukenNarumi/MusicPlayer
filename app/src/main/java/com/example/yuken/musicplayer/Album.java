package com.example.yuken.musicplayer;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.provider.MediaStore;

import java.util.ArrayList;
import java.util.List;

public class Album {
    public long   id;
    public String album;
    public String albumArt;
    public long   albumId;
    public String albumKey;
    public String artist;
    public int    tracks;

    /**
     * 検索時に使用する取得する列名(カラム名、フィールド名)の配列
     */
    private static final String[] FILLED_PROJECTION = {
        MediaStore.Audio.Albums._ID,
        MediaStore.Audio.Albums.ALBUM,
        MediaStore.Audio.Albums.ALBUM_ART,
        MediaStore.Audio.Albums.ALBUM_KEY,
        MediaStore.Audio.Albums.ARTIST,
        MediaStore.Audio.Albums.NUMBER_OF_SONGS
    };

    /**
     * アーティストを指定して検索する際に使用する取得する列名(カラム名、フィールド名)の配列
     */
    private static final String[] FILLED_DESIGNATE_ARTIST = {
        MediaStore.Audio.Albums._ID,
        MediaStore.Audio.Albums.ALBUM,
        MediaStore.Audio.Albums.ALBUM_ART,
        MediaStore.Audio.Albums.ALBUM_KEY,
        MediaStore.Audio.Albums.ARTIST,
        MediaStore.Audio.Albums.NUMBER_OF_SONGS_FOR_ARTIST
    };

    /**
     * デフォルトコンストラクタ
     *
     * @param cursor データベースクエリの検索結果
     */
    private Album(Cursor cursor, boolean designateArtist) {
        id = cursor.getLong(cursor.getColumnIndex(MediaStore.Audio.Albums._ID));
        album = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Albums.ALBUM));
        albumArt = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Albums.ALBUM_ART));
        albumId = cursor.getLong(cursor.getColumnIndex(MediaStore.Audio.Media._ID));
        albumKey = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Albums.ALBUM_KEY));
        artist = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Albums.ARTIST));
        if (designateArtist) {
            tracks = cursor.getInt(cursor.getColumnIndex(MediaStore.Audio.Albums.NUMBER_OF_SONGS_FOR_ARTIST));
        }
        else {
            tracks = cursor.getInt(cursor.getColumnIndex(MediaStore.Audio.Albums.NUMBER_OF_SONGS));
        }
    }

    /**
     * 検索条件にヒットしたアルバム一覧取得
     *
     * @param activity ダイアログを開いているactivity
     * @param select   検索条件
     * @return ヒットしたアルバム一覧
     */
    public static List getItems(Context activity, String select, Long artistId) {
        ContentResolver resolver         = activity.getContentResolver();
        Cursor          cursor;
        boolean         _designateArtist = (select != null);

        if (_designateArtist) {
            cursor = resolver.query(MediaStore.Audio.Artists.Albums.getContentUri("external",
                                                                                  artistId
            ), Album.FILLED_DESIGNATE_ARTIST, null, null, "ALBUM  ASC");
        }
        else {
            cursor = resolver.query(MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI,
                                    Album.FILLED_PROJECTION,
                                    select,
                                    null,
                                    "ALBUM  ASC"
            );
        }

        List albums = new ArrayList();
        if (cursor == null) {
            return albums;
        }

        while (cursor.moveToNext()) {
            albums.add(new Album(cursor, _designateArtist));
        }
        cursor.close();
        return albums;
    }
}
