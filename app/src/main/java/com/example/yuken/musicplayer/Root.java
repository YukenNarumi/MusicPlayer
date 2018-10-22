package com.example.yuken.musicplayer;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.provider.MediaStore;

import java.util.ArrayList;
import java.util.List;

public class Root {

    public enum RootMenu {
        ARTIST,
        ALBUM,
        TRACK,
        END
    }

    public RootMenu menu;
    public String   title;

    public Root(RootMenu root, String name) {
        menu = root;
        title = name;
    }

    public static List getItems(Context activity) {
        List artists = new ArrayList();
        artists.add(new Root(RootMenu.ARTIST, "アーティスト"));
        artists.add(new Root(RootMenu.ALBUM, "アルバム"));
        artists.add(new Root(RootMenu.TRACK, "曲"));
        return artists;
    }
}
