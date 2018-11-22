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

public class ListTrackAdapter extends ArrayAdapter<Track> {
    private LayoutInflater mInflater;

    /**
     * デフォルトコンストラクタ
     *
     * @param context contextオブジェクト
     * @param item    検索条件にヒットした曲一覧
     */
    ListTrackAdapter(Context context, List item) {
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
        Track      item = getItem(position);
        ViewHolder holder;

        if (convertView == null) {
            convertView = mInflater.inflate(R.layout.item_track, null);
            holder = new ViewHolder();
            holder.trackTextView = (TextView)convertView.findViewById(R.id.textBGMTitle);
            holder.trackTextView.setSelected(true);
            holder.artistTextView = (TextView)convertView.findViewById(R.id.textArtist);
            holder.artistTextView.setSelected(true);
            holder.durationTextView = (TextView)convertView.findViewById(R.id.textDuration);
            holder.durationTextView.setSelected(true);
            convertView.setTag(holder);
        }
        else {
            holder = (ViewHolder)convertView.getTag();
        }

        if (item == null) {
            return convertView;
        }

        long dm = item.duration / 60000;
        long ds = (item.duration - (dm * 60000)) / 1000;

        holder.artistTextView.setText(item.artist);
        holder.trackTextView.setText(item.title);
        holder.durationTextView.setText(String.format(Locale.US, "%d:%02d", dm, ds));

        return convertView;
    }

    /**
     * 曲項目用レイアウトクラス
     */
    static class ViewHolder {
        TextView trackTextView;
        TextView artistTextView;
        TextView durationTextView;
    }
}
