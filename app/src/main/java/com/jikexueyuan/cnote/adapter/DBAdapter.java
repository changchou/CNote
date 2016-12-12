package com.jikexueyuan.cnote.adapter;

import android.content.Context;
import android.database.Cursor;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.jikexueyuan.cnote.R;

/**
 * Created by Mr.Z on 2016/4/27 0027.
 */
public class DBAdapter extends BaseAdapter {

    private Context context;
    private Cursor cursor;

    public DBAdapter(Context context, Cursor cursor) {
        this.context = context;
        this.cursor = cursor;
    }

    @Override
    public int getCount() {
        return cursor.getCount();
    }

    @Override
    public Object getItem(int position) {
        return position;
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        if (convertView == null) {
            holder = new ViewHolder();
            convertView = View.inflate(context, R.layout.note_list_cell, null);
            holder.tvNoteTitle = (TextView) convertView.findViewById(R.id.tvNoteTitle);
            holder.tvNoteDate = (TextView) convertView.findViewById(R.id.tvNoteDate);
            holder.tvNoteTag = (TextView) convertView.findViewById(R.id.tvNoteTag);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        cursor.moveToPosition(position);
        holder.tvNoteTitle.setText(cursor.getString(3));
        holder.tvNoteDate.setText(cursor.getString(5));
        if (cursor.getString(6).equals("sync")) {
            holder.tvNoteTag.setText("");
        } else {
            holder.tvNoteTag.setText("未同步");
        }


        return convertView;
    }

    private static class ViewHolder {
        TextView tvNoteTitle;
        TextView tvNoteDate;
        TextView tvNoteTag;
    }
}
