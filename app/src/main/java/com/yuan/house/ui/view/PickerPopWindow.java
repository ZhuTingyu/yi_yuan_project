package com.yuan.house.ui.view;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.widget.Button;
import android.widget.PopupWindow;
import android.widget.TextView;

import com.wx.wheelview.adapter.BaseWheelAdapter;
import com.wx.wheelview.widget.WheelView;
import com.yuan.house.R;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by LiFengYi on 16/5/18.
 */
public class PickerPopWindow extends PopupWindow implements View.OnClickListener, PickerView.OnPickedListener {

    private Context mContext; // 上下文
    private OnPickCompletedListener mListener;// 选择完成事件监听器
    private List<String> item1List, item2List, item3List;

    private View pickerContainerV;
    private View contentView;
    private Button cancelBtn;
    private Button confirmBtn;

    private WheelView item1PickerV, item2PickerV, item3PickerV;
    private String item1, item2, item3 = "";

    public PickerPopWindow(Context context,
                           ArrayList<String> item1List,
                           ArrayList<String> item2List,
                           ArrayList<String> item3List,
                           OnPickCompletedListener completedListener) {

        this.mContext = context;
        this.mListener = completedListener;
        this.item1List = item1List;
        this.item2List = item2List;
        this.item3List = item3List;

        initPicker();
    }

    private void initPicker() {
        contentView = LayoutInflater.from(mContext).inflate(R.layout.layout_item_picker, null);

        cancelBtn = (Button) contentView.findViewById(R.id.btn_cancel);
        confirmBtn = (Button) contentView.findViewById(R.id.btn_confirm);
        item1PickerV = (WheelView) contentView.findViewById(R.id.picker_year);
        item2PickerV = (WheelView) contentView.findViewById(R.id.picker_month);
        item3PickerV = (WheelView) contentView.findViewById(R.id.picker_day);

        pickerContainerV = contentView.findViewById(R.id.container_picker);

        initWheelViews();

        cancelBtn.setOnClickListener(this);
        confirmBtn.setOnClickListener(this);
        contentView.setOnClickListener(this);

        setTouchable(true);
        setFocusable(true);

        setBackgroundDrawable(new BitmapDrawable());
        setContentView(contentView);
        setWidth(ViewGroup.LayoutParams.MATCH_PARENT);
        setHeight(ViewGroup.LayoutParams.MATCH_PARENT);
    }

    private void initWheelViews() {
        WheelView.WheelViewStyle style = new WheelView.WheelViewStyle();
        style.backgroundColor = Color.parseColor("#f2f7fa");

        item1PickerV.setWheelAdapter(new ItemArrayWheel(mContext));
        item1PickerV.setWheelData(item1List);
        item1PickerV.setSkin(WheelView.Skin.Holo);
        item1PickerV.setStyle(style);

        if (item2List.size() != 0) {
            item2PickerV.setWheelAdapter(new ItemArrayWheel(mContext));
            item2PickerV.setWheelData(item2List);
            item2PickerV.setStyle(style);
            item2PickerV.setSkin(WheelView.Skin.Holo);
        } else {
            item2PickerV.setVisibility(View.GONE);
        }

        if (item2List.size() != 0) {
            item3PickerV.setWheelAdapter(new ItemArrayWheel(mContext));
            item3PickerV.setWheelData(item3List);
            item3PickerV.setSkin(WheelView.Skin.Holo);
            item3PickerV.setStyle(style);
        } else {
            item3PickerV.setVisibility(View.GONE);
        }

        item1PickerV.setOnWheelItemSelectedListener(new WheelView.OnWheelItemSelectedListener() {

            @Override
            public void onItemSelected(int position, Object o) {
                item1 = item1List.get(position);
            }
        });

        item2PickerV.setOnWheelItemSelectedListener(new WheelView.OnWheelItemSelectedListener() {
            @Override
            public void onItemSelected(int position, Object o) {
                item2 = item2List.get(position);
            }
        });

        item3PickerV.setOnWheelItemSelectedListener(new WheelView.OnWheelItemSelectedListener() {
            @Override
            public void onItemSelected(int position, Object o) {
                item3 = item3List.get(position);
            }
        });
    }

    /**
     * 显示地址选择器弹层
     *
     * @param activity
     */

    public void showPopWin(Activity activity) {
        if (null != activity) {
            TranslateAnimation trans = new TranslateAnimation(Animation.RELATIVE_TO_SELF, 0, Animation.RELATIVE_TO_SELF, 0, Animation.RELATIVE_TO_SELF, 1, Animation.RELATIVE_TO_SELF, 0);

            showAtLocation(activity.getWindow().getDecorView(), Gravity.BOTTOM, 0, 0);

            trans.setDuration(400);
            trans.setInterpolator(new AccelerateDecelerateInterpolator());

            pickerContainerV.startAnimation(trans);
        }
    }

    /**
     * 关闭地址选择器弹层
     */
    public void dismissPopWin() {
        TranslateAnimation trans = new TranslateAnimation(Animation.RELATIVE_TO_SELF, 0, Animation.RELATIVE_TO_SELF, 0, Animation.RELATIVE_TO_SELF, 0, Animation.RELATIVE_TO_SELF, 1);

        trans.setDuration(400);
        trans.setInterpolator(new AccelerateInterpolator());
        trans.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                dismiss();
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
            }
        });
        pickerContainerV.startAnimation(trans);
    }

    @Override
    public void onClick(View v) {
        if (v == contentView || v == cancelBtn) {
            dismissPopWin();
        } else if (v == confirmBtn) {
            if (null != mListener) mListener.onAddressPickCompleted(item1, item2, item3);

            dismissPopWin();
        }
    }

    @Override
    public void onPicked(int pickerId, String item) {
    }

    /**
     * 地址选择完成事件监听器接口
     */
    public interface OnPickCompletedListener {
        void onAddressPickCompleted(String item1, String item2, String item3);
    }

    private class ItemArrayWheel extends BaseWheelAdapter<String> {
        private Context mContext;

        public ItemArrayWheel(Context context) {
            this.mContext = context;
        }

        @Override
        public View bindView(int position, View convertView, ViewGroup parent) {
            ViewHolder holder;

            if (convertView == null) {
                holder = new ViewHolder();
                convertView = LayoutInflater.from(mContext).inflate(R.layout.item_list_adapter, null);
                holder.info = (TextView) convertView.findViewById(R.id.info);
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }

            holder.info.setText(mList.get(position));

            return convertView;
        }

        class ViewHolder {
            TextView info;
        }
    }
}
