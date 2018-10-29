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

public class ListRootAdapter extends ArrayAdapter<Root> {
    private LayoutInflater mInflater;

    /**
     * デフォルトコンストラクタ
     *
     * @param context contextオブジェクト
     * @param item    ルート画面に表示する項目一覧
     */
    ListRootAdapter(Context context, List item) {
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
        Root                       item = getItem(position);
        ListRootAdapter.ViewHolder holder;

        if (convertView == null) {
            convertView = mInflater.inflate(R.layout.item_root, null);
            holder = new ListRootAdapter.ViewHolder();
            holder.titleTextView = (TextView)convertView.findViewById(R.id.title);
            convertView.setTag(holder);
        }
        else {
            holder = (ListRootAdapter.ViewHolder)convertView.getTag();
        }

        if (item == null) {
            return convertView;
        }

        holder.titleTextView.setText(item.title);

        return convertView;
    }

    /**
     * ルート画面の項目用レイアウトクラス
     */
    static class ViewHolder {
        TextView titleTextView;
    }
}
