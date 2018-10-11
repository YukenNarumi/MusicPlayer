package com.example.yuken.musicplayer;

import android.app.Dialog;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;

import java.util.List;

public class TrackDialogFragment extends DialogFragment implements AdapterView.OnItemClickListener {

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

    /**
     * @param savedInstanceState
     * @return
     */
    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Dialog dialog = new Dialog(getActivity());
        dialog.setContentView(R.layout.dialog_track_list);

        backButton = dialog.findViewById(R.id.backkBtn);
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                UpdateScene(DialogSceneType.HOME);
            }
        });

        infoList = dialog.findViewById(R.id.listTrack);
        infoList.setOnItemClickListener((AdapterView.OnItemClickListener) this);

        UpdateScene(DialogSceneType.HOME);

        return dialog;
    }

    /**
     * リスト項目タップ時の挙動
     *
     * @param parent
     * @param v
     * @param position
     * @param id
     */
    @Override
    public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
        if(sceneType == DialogSceneType.HOME){
            UpdateOnItemClickRoot(position);
        }
        else if(sceneType == DialogSceneType.ARTIST){
        }
        else if(sceneType == DialogSceneType.ALBUM){
        }
        else if(sceneType == DialogSceneType.TRACK){
            UpdateOnItemClickTrack(parent, position);
        }
    }

    /**
     * ダイアログのルート画面のリスト項目タップ時の更新処理
     *
     * @param position タップしたリスト項目の位置
     */
    private void UpdateOnItemClickRoot(int position) {
        DialogSceneType _type = DialogSceneType.HOME;
        if(position == Root.RootMenu.ARTIST.ordinal()) {
            _type = DialogSceneType.ARTIST;
        }
        else if(position == Root.RootMenu.ALBUM.ordinal()) {
            _type = DialogSceneType.ALBUM;
        }
        else if(position == Root.RootMenu.TRACK.ordinal()) {
            _type = DialogSceneType.TRACK;
        }
        UpdateScene(_type);
    }

    /**
     * 曲リスト表示中のリスト項目タップ時の更新処理
     * 曲の読み込みを行う
     *
     * @param parent
     * @param position
     */
    private void UpdateOnItemClickTrack(AdapterView<?> parent, int position) {
        ListView listView = (ListView) parent;
        Track item = (Track) listView.getItemAtPosition(position);
        MainActivity mainActivity = (MainActivity)getActivity();
        if(mainActivity.LoadBGM(item.uri)){
            mainActivity.ClearMediaPlayerInfo();
        }
    }
}