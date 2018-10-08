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
/*
        // TODO:トラック一覧表示用
        Dialog dialog = new Dialog(getActivity());
        dialog.setContentView(R.layout.dialog_track_list);

        List<Track> tracks = Track.getItems(getActivity());
        ListView trackList = dialog.findViewById(R.id.listTrack);
        ListTrackAdapter adapter = new ListTrackAdapter(getActivity(), tracks);
        trackList.setAdapter(adapter);

        Button setButton = dialog.findViewById(R.id.trackCloseBtn);
        setButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // TODO:ダイアログ閉じる
            }
        });
*/
        // TODO:アルバム一覧表示用
        Dialog dialog = new Dialog(getActivity());
        dialog.setContentView(R.layout.dialog_track_list);
        List<Album> albums = Album.getItems(getActivity());
        ListView trackList = dialog.findViewById(R.id.listTrack);
        ListAlbumAdapter adapter = new ListAlbumAdapter(getActivity(), albums);
        trackList.setAdapter(adapter);

        return dialog;
    }
}