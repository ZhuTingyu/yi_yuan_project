package com.avoscloud.chat.ui.chat;

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

    public InputMoreAdapter(Context context) {
        mContext = context;
    }

    public void addItem(String text) {
        mItems.add(text);
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
        final String contractText = (String) getItem(position);
        if (!TextUtils.isEmpty(contractText)) {
            int resId = R.drawable.btn_core;
            if (contractText.equals("核心合同")) {
                resId = R.drawable.btn_core;
            } else if (contractText.equals("买卖合同")) {
                resId = R.drawable.btn_deal;
            } else if (contractText.equals("补充协议")) {
                resId = R.drawable.btn_supplement;
            } else if (contractText.equals("前置留言板")) {
                resId = R.drawable.btn_leave_message;
            } else if (contractText.equals("房源")) {
                resId = R.drawable.btn_need;
            }

            TextView tv = (TextView) convertView;
            Drawable drawable = mContext.getResources().getDrawable(resId);
            int h = drawable.getIntrinsicHeight();
            int w = drawable.getIntrinsicWidth();
            drawable.setBounds(0, 0, w, h);

            tv.setCompoundDrawables(null, drawable, null, null);
//            GridLayout.LayoutParams params = new GridLayout.LayoutParams();
//            params.rowSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
//            params.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
//            params.width = 0;
//            params.height = 0;
//            tv.setLayoutParams(params);
            tv.setGravity(Gravity.CENTER);
            tv.setText(contractText);

            tv.setTag("contract");
        }

        return convertView;
    }
}
