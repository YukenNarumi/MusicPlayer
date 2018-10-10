package com.example.yuken.musicplayer;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.provider.MediaStore;

import java.util.ArrayList;
import java.util.List;

public class Root {
    public String   title;

    public Root(String name) {
        title = name;
    }

    public static List getItems(Context activity) {
        List artists = new ArrayList();
        artists.add(new Root("アーティスト"));
        artists.add(new Root("アルバム"));
        artists.add(new Root("曲"));
        return artists;
    }
}
