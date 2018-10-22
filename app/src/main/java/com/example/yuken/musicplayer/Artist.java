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

    public static final String[] FILLED_PROJECTION = {
        MediaStore.Audio.Artists._ID,
        MediaStore.Audio.Artists.ARTIST,
        MediaStore.Audio.Artists.ARTIST_KEY,
        MediaStore.Audio.Artists.NUMBER_OF_ALBUMS,
        MediaStore.Audio.Artists.NUMBER_OF_TRACKS
    };

    public Artist(Cursor cursor) {
        id = cursor.getLong(cursor.getColumnIndex(MediaStore.Audio.Artists._ID));
        artist = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Artists.ARTIST));
        artistKey = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Artists.ARTIST_KEY));
        albums = cursor.getInt(cursor.getColumnIndex(MediaStore.Audio.Artists.NUMBER_OF_ALBUMS));
        tracks = cursor.getInt(cursor.getColumnIndex(MediaStore.Audio.Artists.NUMBER_OF_TRACKS));
    }

    public static List getItems(Context activity) {
        ContentResolver resolver = activity.getContentResolver();
        Cursor cursor = resolver.query(MediaStore.Audio.Artists.EXTERNAL_CONTENT_URI,
                                       Artist.FILLED_PROJECTION,
                                       null,
                                       null,
                                       "ARTIST  ASC"
        );

        List artists = new ArrayList();
        while (cursor.moveToNext()) {
            artists.add(new Artist(cursor));
        }

        cursor.close();
        return artists;
    }
}
