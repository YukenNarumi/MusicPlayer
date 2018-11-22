package com.example.yuken.musicplayer;

import android.annotation.SuppressLint;
import android.content.Context;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.List;
import java.util.Locale;

public class ListArtistAdapter extends ArrayAdapter<Artist> {
    private LayoutInflater mInflater;

    /**
     * デフォルトコンストラクタ
     *
     * @param context contextオブジェクト
     * @param item    検索条件にヒットしたアーティスト一覧
     */
    ListArtistAdapter(Context context, List item) {
        super(context, 0, item);
        mInflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    /**
     * 指定位置のデータを表示するViewの取得
     *
     * @param position    List番号
     * @param convertView ListのView情報
     * @param parent      Viewの親オブジェクト
     * @return 指定位置のView情報
     */
    @SuppressLint("InflateParams")
    @NonNull
    @Override
    public View getView(int position, View convertView, @NonNull ViewGroup parent) {
        Artist                       item = getItem(position);
        ListArtistAdapter.ViewHolder holder;

        if (convertView == null) {
            convertView = mInflater.inflate(R.layout.item_artist, null);
            holder = new ListArtistAdapter.ViewHolder();
            holder.artistTextView = convertView.findViewById(R.id.artist);
            holder.artistTextView.setSelected(true);
            holder.albumsTextView = convertView.findViewById(R.id.album);
            holder.albumsTextView.setSelected(true);
            holder.tracksTextView = convertView.findViewById(R.id.tracks);
            holder.tracksTextView.setSelected(true);
            convertView.setTag(holder);
        }
        else {
            holder = (ListArtistAdapter.ViewHolder)convertView.getTag();
        }

        if (item == null) {
            return convertView;
        }

        holder.artistTextView.setText(item.artist);
        holder.albumsTextView.setText(String.format(Locale.US, "%d Albums", item.albums));
        holder.tracksTextView.setText(String.format(Locale.US, "%d tracks", item.tracks));

        return convertView;
    }

    /**
     * アーティスト項目用レイアウトクラス
     */
    static class ViewHolder {
        TextView artistTextView;
        TextView albumsTextView;
        TextView tracksTextView;
    }
}
