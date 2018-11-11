package com.example.yuken.musicplayer;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Context;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

public class TrackDetail {
    @SuppressLint("StaticFieldLeak")
    private static Context      Mcontext;
    private        View         separatorLine;
    private        LinearLayout conclusion;
    private        TextView     albumTextView;
    private        TextView     artistTextView;
    private        TextView     tracksTextView;
    private        ImageView    artworkImageView;

    /**
     * デフォルトコンストラクタ
     *
     * @param context contextオブジェクト
     * @param dialog  詳細情報を表示させるダイアログ
     */
    TrackDetail(Context context, Dialog dialog) {
        Mcontext = context;
        separatorLine = dialog.findViewById(R.id.addInformationSeparator);
        conclusion = dialog.findViewById(R.id.addInformation);
        albumTextView = dialog.findViewById(R.id.title);
        artistTextView = dialog.findViewById(R.id.artist);
        tracksTextView = dialog.findViewById(R.id.tracks);
        artworkImageView = dialog.findViewById(R.id.albumart);

        SetVisible(false);
    }

    /**
     * 詳細情報の表示設定
     *
     * @param visible 表示の有無 / 表示しない場合は詰める
     */
    private void SetVisible(boolean visible) {
        if (separatorLine == null || conclusion == null) {
            return;
        }
        int _type = visible
                    ? View.VISIBLE
                    : View.GONE;
        separatorLine.setVisibility(_type);
        conclusion.setVisibility(_type);
    }

    /**
     * 詳細情報を非表示にしたうえで詰める
     */
    public void Hidden() {
        if (separatorLine == null || conclusion == null) {
            return;
        }
        SetVisible(false);
    }

    /**
     * アルバムの詳細情報を更新する
     *
     * @param album 表示するアルバム情報
     */
    public void ShowAlbumInfo(Album album) {
        SetVisible(true);

        albumTextView.setText(album.album);
        artistTextView.setText(album.artist);
        String _tracksTextView = (String.valueOf(album.tracks) + "tracks");
        tracksTextView.setText(_tracksTextView);

        artworkImageView.setImageResource(ImageCache.GetGrayDefaultID());
        String path = album.albumArt;
        if (path == null) {
            path = ImageCache.GetDefaultPath();
            ImageCache.CacheDefault(Mcontext.getResources());
        }
        artworkImageView.setTag(path);
        ImageGetTask task = new ImageGetTask(artworkImageView);
        task.execute(path);
    }
}
