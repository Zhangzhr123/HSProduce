package com.hsproduce.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import com.hsproduce.R;
import com.hsproduce.bean.HomeBtnBean;

import java.util.ArrayList;
import java.util.List;

public class HomeGridAdapter extends BaseAdapter {

    private Context context;
    private List<HomeBtnBean> list = new ArrayList<>();

    public HomeGridAdapter(Context context, List<HomeBtnBean> list) {
        this.context = context;
        this.list = list;
    }

    @Override
    public int getCount() {
        return list.size();
    }

    @Override
    public HomeBtnBean getItem(int position) {
        return list.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        convertView = LayoutInflater.from(context).inflate(R.layout.itemhome, null);
        final HomeBtnBean homeBtnBean = list.get(position);
        ImageView  mImageView = (ImageView) convertView.findViewById(R.id.img_homeitem);
        mImageView.setBackgroundResource(homeBtnBean.getImaResId());
        ((TextView) convertView.findViewById(R.id.tv_homename)).setText(homeBtnBean.getBtnName());
        return convertView;
    }
}