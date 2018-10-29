package com.example.yuken.musicplayer;

import android.content.Context;

import java.util.ArrayList;
import java.util.List;

public class Root {
    // 項目タイプ
    public enum RootMenu {
        ARTIST,
        ALBUM,
        TRACK,
        END
    }

    public RootMenu menu;
    public String   title;

    /**
     * デフォルトコンストラクタ
     *
     * @param root 項目タイプ
     * @param name 項目名
     */
    private Root(RootMenu root, String name) {
        menu = root;
        title = name;
    }

    /**
     * ルート画面に表示する項目一覧取得
     *
     * @param activity ダイアログを開いているactivity
     * @return 表示する項目一覧
     */
    public static List getItems(Context activity) {
        List artists = new ArrayList();
        artists.add(new Root(RootMenu.ARTIST, "アーティスト"));
        artists.add(new Root(RootMenu.ALBUM, "アルバム"));
        artists.add(new Root(RootMenu.TRACK, "曲"));
        return artists;
    }
}
