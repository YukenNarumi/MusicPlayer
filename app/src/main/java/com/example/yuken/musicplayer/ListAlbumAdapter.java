package com.example.yuken.musicplayer;

import android.annotation.SuppressLint;
import android.content.Context;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;

public class ListAlbumAdapter extends ArrayAdapter<Album> {
    private        LayoutInflater mInflater;
    @SuppressLint("StaticFieldLeak")
    private static Context        Mcontext;

    /**
     * デフォルトコンストラクタ
     *
     * @param context contextオブジェクト
     * @param item    検索条件にヒットしたアルバム一覧
     */
    ListAlbumAdapter(Context context, List item) {
        super(context, 0, item);
        mInflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        Mcontext = context;
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
        Album      item = getItem(position);
        ViewHolder holder;

        if (convertView == null) {
            convertView = mInflater.inflate(R.layout.item_album, null);
            holder = new ViewHolder();
            holder.albumTextView = convertView.findViewById(R.id.title);
            holder.albumTextView.setSelected(true);
            holder.artistTextView = convertView.findViewById(R.id.artist);
            holder.artistTextView.setSelected(true);
            holder.tracksTextView = convertView.findViewById(R.id.tracks);
            holder.tracksTextView.setSelected(true);
            holder.artworkImageView = convertView.findViewById(R.id.albumart);
            convertView.setTag(holder);
        }
        else {
            holder = (ViewHolder)convertView.getTag();
        }

        if (item == null) {
            return convertView;
        }

        holder.albumTextView.setText(item.album);
        holder.artistTextView.setText(item.artist);
        String _tracksTextView = (String.valueOf(item.tracks) + "tracks");
        holder.tracksTextView.setText(_tracksTextView);

        holder.artworkImageView.setImageResource(ImageCache.GetGrayDefaultID());
        String path = item.albumArt;
        if (path == null) {
            path = ImageCache.GetDefaultPath();
            ImageCache.CacheDefault(Mcontext.getResources());
        }
        holder.artworkImageView.setTag(path);
        ImageGetTask        task   = new ImageGetTask(holder.artworkImageView);
        ImageGetTask.Params _param = new ImageGetTask.Params(path, ImageGetTask.Params.Type.SLIM);
        task.execute(_param);

        return convertView;
    }

    /**
     * アルバム項目用レイアウトクラス
     */
    static class ViewHolder {
        TextView  albumTextView;
        TextView  artistTextView;
        TextView  tracksTextView;
        ImageView artworkImageView;
    }
}
