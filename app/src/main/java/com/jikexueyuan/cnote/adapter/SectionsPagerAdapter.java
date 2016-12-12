package com.jikexueyuan.cnote.adapter;

import android.content.Context;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.jikexueyuan.cnote.activity.NoteFragment;
import com.jikexueyuan.cnote.R;
import com.jikexueyuan.cnote.activity.SettingFragment;

/**
 * Created by Mr.Z on 2016/3/29 0029.
 */
public class SectionsPagerAdapter extends FragmentPagerAdapter {

    private Context context;


    public SectionsPagerAdapter(FragmentManager fm,Context context) {
        super(fm);
        this.context = context;
    }

    private int[] imageResId = {
            R.drawable.note,
            R.drawable.setting,
    };

    public View getTabView(int position) {
        View v = LayoutInflater.from(context).inflate(R.layout.custom_tab, null);
        TextView tv = (TextView) v.findViewById(R.id.tv);
        tv.setText(getPageTitle(position));
        ImageView img = (ImageView) v.findViewById(R.id.image);
        img.setImageResource(imageResId[position]);
        return v;
    }



    @Override
    public Fragment getItem(int position) {
        switch (position){
            case 0:
                return new NoteFragment();
            case 1:
                return new SettingFragment();
        }
        return null;
    }

    @Override
    public int getCount() {
        return 2;
    }

    @Override
    public CharSequence getPageTitle(int position) {
        switch (position) {
            case 0:
                return context.getString(R.string.note);
            case 1:
                return context.getString(R.string.setting);
        }
        return null;
    }
}
