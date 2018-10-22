package com.example.yuken.musicplayer;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.List;

public class ListRootAdapter extends ArrayAdapter<Root> {
    LayoutInflater mInflater;

    public ListRootAdapter(Context context, List item) {
        super(context, 0, item);
        mInflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
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

        holder.titleTextView.setText(item.title);

        return convertView;
    }

    static class ViewHolder {
        TextView titleTextView;
    }
}
