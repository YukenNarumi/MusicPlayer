package com.example.yuken.musicplayer;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;

import java.util.ArrayList;
import java.util.List;

public class Track {
    // 検索結果に含める最小再生時間(ms)
    private static final long MIN_REQUIRED_DURATION = 6000;

    public long   id;           // コンテントプロバイダに登録されたID
    public long   albumId;      // 同じくトラックのアルバムのID
    public long   artistId;     // 同じくトラックのアーティストのID
    public String path;         // 実データのPATH
    public String title;        // トラックタイトル
    public String album;        // アルバムタイトル
    public String artist;       // アーティスト名
    public Uri    uri;          // URI
    public long   duration;     // 再生時間(ミリ秒)
    public int    trackNo;      // アルバムのトラックナンバ

    /**
     * 検索時に使用する取得する列名(カラム名、フィールド名)の配列
     */
    private static final String[] COLUMNS = {
        MediaStore.Audio.Media._ID,
        MediaStore.Audio.Media.DATA,
        MediaStore.Audio.Media.TITLE,
        MediaStore.Audio.Media.ALBUM,
        MediaStore.Audio.Media.ARTIST,
        MediaStore.Audio.Media.ALBUM_ID,
        MediaStore.Audio.Media.ARTIST_ID,
        MediaStore.Audio.Media.DURATION,
        MediaStore.Audio.Media.TRACK
    };

    /**
     * デフォルトコンストラクタ
     *
     * @param cursor データベースクエリの検索結果
     */
    private Track(Cursor cursor, Context activity) {
        id = cursor.getLong(cursor.getColumnIndex(MediaStore.Audio.Media._ID));
        path = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.DATA));
        title = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.TITLE));
        album = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM));
        artist = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST));
        albumId = cursor.getLong(cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM_ID));
        artistId = cursor.getLong(cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST_ID));
        duration = cursor.getLong(cursor.getColumnIndex(MediaStore.Audio.Media.DURATION));
        trackNo = cursor.getInt(cursor.getColumnIndex(MediaStore.Audio.Media.TRACK));
        uri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id);
    }

    /**
     * 検索条件にヒットした曲一覧取得
     *
     * @param activity ダイアログを開いているactivity
     * @param select   検索条件
     * @return ヒットしたアルバム一覧
     */
    public static List getItems(Context activity, String select) {
        ContentResolver resolver = activity.getContentResolver();
        Cursor cursor = resolver.query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                                       Track.COLUMNS,
                                       select,
                                       null,
                                       null
        );

        List tracks = new ArrayList();
        if (cursor == null) {
            return tracks;
        }

        while (cursor.moveToNext()) {
            if (cursor.getLong(cursor.getColumnIndex(MediaStore.Audio.Media.DURATION)) < MIN_REQUIRED_DURATION) {
                continue;
            }
            tracks.add(new Track(cursor, activity));
        }
        cursor.close();
        return tracks;
    }

    /**
     * アルバムアートのパス取得
     *
     * @param activity ダイアログを開いているactivity
     * @return アルバムアートのパス
     */
    public String GetAlbumArt(Context activity) {
        String _albumArt = ImageCache.GetDefaultPath();
        ImageCache.CacheDefault(activity.getResources());

        final String[] _COLUMNS = {
            MediaStore.Audio.Albums.ALBUM_ART
        };
        ContentResolver resolver = activity.getContentResolver();
        Cursor cursorAlbumArt = resolver.query(MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI,
                                               _COLUMNS,
                                               "album='" + album + "'",
                                               null,
                                               null
        );
        if (cursorAlbumArt == null) {
            return _albumArt;
        }
        while (cursorAlbumArt.moveToNext()) {
            String _path = cursorAlbumArt.getString(cursorAlbumArt.getColumnIndex(MediaStore.Audio.Albums.ALBUM_ART));
            if (_path == null) {
                continue;
            }
            _albumArt = _path;
            break;
        }
        cursorAlbumArt.close();
        return _albumArt;
    }
}