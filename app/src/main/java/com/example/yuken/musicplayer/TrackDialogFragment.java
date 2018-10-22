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

import java.util.ArrayList;
import java.util.List;

public class TrackDialogFragment extends DialogFragment implements AdapterView.OnItemClickListener {

    public enum DialogSceneType {
        HOME,
        ARTIST,
        ALBUM,
        TRACK,
        END
    }

    private ArrayList<DialogSceneType> prevSceneType;
    private DialogSceneType            sceneType;
    private Button                     backButton;
    private ListView                   infoList;
    private String                     selectWordArtist;
    private String                     selectWordAlbum;

    /**
     * アーティストが関係しているアルバム、
     * アルバム内の曲検索で使用する検索条件のクリア
     */
    private void ClearSelectWord() {
        selectWordArtist = null;
        selectWordAlbum = null;
    }

    /**
     * 戻るボタン押した際の遷移先制御
     */
    private void UpdateBackPrev() {
        int _size = prevSceneType.size();
        if (_size <= 0) {
            Log.d("debug", "要素が無い");
            return;
        }

        int _index = _size - 1;
        UpdateScene(prevSceneType.get(_index));

        // UpdateScene()実行時に現在のシーンがスタックされてしまうので2回削除してる
        prevSceneType.remove(_index);
        prevSceneType.remove(_index);
    }

    /**
     * シーン遷移
     *
     * @param type 遷移先
     */
    private void UpdateScene(DialogSceneType type) {
        backButton.setVisibility(type == DialogSceneType.HOME
                                 ? View.INVISIBLE
                                 : View.VISIBLE);

        infoList.setAdapter(null);
        if (type == DialogSceneType.HOME) {
            UpdateSceneHome();
        }
        else if (type == DialogSceneType.ARTIST) {
            UpdateSceneArtist();
        }
        else if (type == DialogSceneType.ALBUM) {
            UpdateSceneAlbum();
        }
        else if (type == DialogSceneType.TRACK) {
            UpdateSceneTracks();
        }

        prevSceneType.add(sceneType);
        sceneType = type;
    }

    /**
     * ホーム画面
     */
    private void UpdateSceneHome() {
        ClearSelectWord();

        List<Root>      root    = Root.getItems(getActivity());
        ListRootAdapter adapter = new ListRootAdapter(getActivity(), root);
        infoList.setAdapter(adapter);
    }

    /**
     * アーティスト一覧表示
     */
    private void UpdateSceneArtist() {
        List<Artist>      artist  = Artist.getItems(getActivity());
        ListArtistAdapter adapter = new ListArtistAdapter(getActivity(), artist);
        infoList.setAdapter(adapter);
    }

    /**
     * アルバム一覧表示
     */
    private void UpdateSceneAlbum() {
        List<Album>      albums  = Album.getItems(getActivity(), selectWordArtist);
        ListAlbumAdapter adapter = new ListAlbumAdapter(getActivity(), albums);
        infoList.setAdapter(adapter);
    }

    /**
     * トラック一覧表示
     */
    private void UpdateSceneTracks() {
        List<Track>      tracks  = Track.getItems(getActivity(), selectWordAlbum);
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
        prevSceneType = new ArrayList<>();

        Dialog dialog = new Dialog(getActivity());
        dialog.setContentView(R.layout.dialog_track_list);

        backButton = dialog.findViewById(R.id.backkBtn);
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                UpdateBackPrev();
            }
        });

        infoList = dialog.findViewById(R.id.listTrack);
        infoList.setOnItemClickListener((AdapterView.OnItemClickListener)this);

        UpdateScene(DialogSceneType.HOME);

        return dialog;
    }

    /**
     * リスト項目タップ時の挙動
     *
     * @param parent   イベントが起きたListView
     * @param v        選択されたリスト項目
     * @param position 選択されたリスト項目の位置(0～)
     * @param id       選択されたリスト項目のIDを示す値(0～)
     */
    @Override
    public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
        if (sceneType == DialogSceneType.HOME) {
            UpdateOnItemClickRoot(position);
        }
        else if (sceneType == DialogSceneType.ARTIST) {
            UpdateOnItemClickArtist(parent, position);
        }
        else if (sceneType == DialogSceneType.ALBUM) {
            UpdateOnItemClickAlbum(parent, position);
        }
        else if (sceneType == DialogSceneType.TRACK) {
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
        if (position == Root.RootMenu.ARTIST.ordinal()) {
            _type = DialogSceneType.ARTIST;
        }
        else if (position == Root.RootMenu.ALBUM.ordinal()) {
            _type = DialogSceneType.ALBUM;
        }
        else if (position == Root.RootMenu.TRACK.ordinal()) {
            _type = DialogSceneType.TRACK;
        }
        UpdateScene(_type);
    }

    /**
     * アーティストリスト表示中のリスト項目タップ時の更新処理
     * アーティスト名からアルバム検索を行う
     *
     * @param parent   イベントが起きたListView
     * @param position 選択されたリスト項目の位置(0～)
     */
    private void UpdateOnItemClickArtist(AdapterView<?> parent, int position) {
        ListView listView = (ListView)parent;
        Artist   item     = (Artist)listView.getItemAtPosition(position);
        selectWordArtist = "artist='" + item.artist + "'";
        UpdateScene(DialogSceneType.ALBUM);
    }

    /**
     * アルバムリスト表示中のリスト項目タップ時の更新処理
     * アルバム名から曲検索を行う
     *
     * @param parent   イベントが起きたListView
     * @param position 選択されたリスト項目の位置(0～)
     */
    private void UpdateOnItemClickAlbum(AdapterView<?> parent, int position) {
        ListView listView = (ListView)parent;
        Album    item     = (Album)listView.getItemAtPosition(position);
        selectWordAlbum = "album='" + item.album + "'";
        UpdateScene(DialogSceneType.TRACK);
    }

    /**
     * 曲リスト表示中のリスト項目タップ時の更新処理
     * 曲の読み込みを行う
     *
     * @param parent   イベントが起きたListView
     * @param position 選択されたリスト項目の位置(0～)
     */
    private void UpdateOnItemClickTrack(AdapterView<?> parent, int position) {
        ListView     listView     = (ListView)parent;
        Track        item         = (Track)listView.getItemAtPosition(position);
        MainActivity mainActivity = (MainActivity)getActivity();
        if (mainActivity.LoadBGM(item.uri)) {
            mainActivity.ClearMediaPlayerInfo();
            mainActivity.LoadLoopPointDate(item.title);
        }
    }
}