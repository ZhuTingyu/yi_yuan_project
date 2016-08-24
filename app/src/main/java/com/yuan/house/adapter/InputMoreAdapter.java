package com.yuan.house.adapter;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.yuan.house.R;

import java.util.ArrayList;

/**
 * Created by edwardliu on 16/7/4.
 */
public class InputMoreAdapter extends BaseAdapter {

    private Context mContext;
    private ArrayList<String> mItems = new ArrayList<>();
    private ArrayList<Integer> mValues = new ArrayList<>();

    public InputMoreAdapter(Context context) {
        mContext = context;
    }

    public void addItem(String text, int value) {
        if (!mItems.contains(text)) {
            mItems.add(text);
            mValues.add(value);
        }
    }

    public int getValue(int position) {
        return mValues.get(position);
    }

    public void clearItems() {
        mItems.clear();
    }

    @Override
    public int getCount() {
        return mItems.size();
    }

    @Override
    public Object getItem(int position) {
        return mItems.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (null == convertView) {
            convertView = new TextView(mContext);
        }

        final String text = (String) getItem(position);
        if (!TextUtils.isEmpty(text)) {
            TextView tv = (TextView) convertView;
            int resId = R.drawable.btn_core;
            if (text.equals("中心合同")) {
                resId = R.drawable.btn_core;
                tv.setTag("contract");
            } else if (text.equals("买卖合同")) {
                resId = R.drawable.btn_deal;
                tv.setTag("contract");
            } else if (text.equals("补充协议")) {
                resId = R.drawable.btn_supplement;
                tv.setTag("contract");
            } else if (text.equals("前置留言板")) {
                resId = R.drawable.btn_leave_message;
            } else if (text.equals("优惠券")) {
                resId = R.drawable.btn_discount;
            } else if (text.equals("房源")) {
                resId = R.drawable.btn_need;
            } else if (text.equals("照片")) {
                resId = R.drawable.chat_bottom_add_picture_selector;
            }

            Drawable drawable = mContext.getResources().getDrawable(resId);
            int h = drawable.getIntrinsicHeight();
            int w = drawable.getIntrinsicWidth();
            drawable.setBounds(0, 0, w, h);

            tv.setCompoundDrawables(null, drawable, null, null);
            tv.setGravity(Gravity.CENTER);
            tv.setText(text);
        }

        return convertView;
    }
}
