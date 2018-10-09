package com.example.yuken.musicplayer;

import android.app.Dialog;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;

import java.util.List;

public class TrackDialogFragment extends DialogFragment {

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Dialog dialog = new Dialog(getActivity());
        dialog.setContentView(R.layout.dialog_track_list);
        ListView trackList = dialog.findViewById(R.id.listTrack);

/*
        // TODO:トラック一覧表示用
        List<Track> tracks = Track.getItems(getActivity());
        ListTrackAdapter adapter = new ListTrackAdapter(getActivity(), tracks);

        Button setButton = dialog.findViewById(R.id.trackCloseBtn);
        setButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // TODO:ダイアログ閉じる
            }
        });
*/
/*
        // TODO:アルバム一覧表示用
        List<Album> albums = Album.getItems(getActivity());
        ListAlbumAdapter adapter = new ListAlbumAdapter(getActivity(), albums);
*/
        // TODO:アーティスト一覧表示用
        List<Artist> artist = Artist.getItems(getActivity());
        ListArtistAdapter adapter = new ListArtistAdapter(getActivity(), artist);

        trackList.setAdapter(adapter);

        return dialog;
    }
}