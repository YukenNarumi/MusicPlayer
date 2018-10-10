package com.example.yuken.musicplayer;

import android.app.Dialog;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;

import java.util.List;

public class TrackDialogFragment extends DialogFragment {

    public enum DialogSceneType {
        HOME,
        ARTIST,
        ALBUM,
        TRACK,
        END
    };

    private DialogSceneType prevSceneType;
    private DialogSceneType sceneType;
    private Button backButton;
    private ListView infoList;

    private void UpdateScene(DialogSceneType type) {
        backButton.setVisibility(type == DialogSceneType.HOME ? View.INVISIBLE : View.VISIBLE);

        infoList.setAdapter(null);
        if(type == DialogSceneType.HOME){
            UpdateSceneHome();
        }
        else if(type == DialogSceneType.ARTIST){
            UpdateSceneArtist();
        }
        else if(type == DialogSceneType.ALBUM){
            UpdateSceneAlbum();
        }
        else if(type == DialogSceneType.TRACK){
            UpdateSceneTracks();
        }

        prevSceneType = sceneType;
        sceneType = type;
    }

    /**
     * ホーム画面
     */
    private void UpdateSceneHome() {
/*
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(getActivity(),
                android.R.layout.simple_list_item_1);

        // 要素の追加（1）
        adapter.add("a");
        adapter.add("b");
        adapter.add("c");
*/

        List<Root> root = Root.getItems(getActivity());
        ListRootAdapter adapter = new ListRootAdapter(getActivity(), root);
        infoList.setAdapter(adapter);
    }

    /**
     * アーティスト一覧表示
     */
    private void UpdateSceneArtist() {
        List<Artist> artist = Artist.getItems(getActivity());
        ListArtistAdapter adapter = new ListArtistAdapter(getActivity(), artist);
        infoList.setAdapter(adapter);
    }

    /**
     * アルバム一覧表示
     */
    private void UpdateSceneAlbum() {
        List<Album> albums = Album.getItems(getActivity());
        ListAlbumAdapter adapter = new ListAlbumAdapter(getActivity(), albums);
        infoList.setAdapter(adapter);
    }

    /**
     * トラック一覧表示
     */
    private void UpdateSceneTracks() {
        List<Track> tracks = Track.getItems(getActivity());
        ListTrackAdapter adapter = new ListTrackAdapter(getActivity(), tracks);
        infoList.setAdapter(adapter);
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Dialog dialog = new Dialog(getActivity());
        dialog.setContentView(R.layout.dialog_track_list);
//        ListView trackList = dialog.findViewById(R.id.listTrack);

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
/*
        // TODO:アーティスト一覧表示用
        List<Artist> artist = Artist.getItems(getActivity());
        ListArtistAdapter adapter = new ListArtistAdapter(getActivity(), artist);

        trackList.setAdapter(adapter);
*/
        backButton = dialog.findViewById(R.id.backkBtn);
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                UpdateScene(DialogSceneType.HOME);
            }
        });

        infoList = dialog.findViewById(R.id.listTrack);

        UpdateScene(DialogSceneType.HOME);

        return dialog;
    }
}