package com.yuan.skeleton.ui.view;

import android.app.Activity;
import android.content.Context;
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

import com.yuan.skeleton.R;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by LiFengYi on 16/5/18.
 */
public class PickerPopWindow extends PopupWindow implements View.OnClickListener,PickerView.OnPickedListener{

    private Context mContext; // 上下文
    private OnPickCompletedListener mListener;// 选择完成事件监听器
    private List<String> item1List,item2List,item3List;

    private View pickerContainerV;
    private View contentView;
    private Button cancelBtn;
    private Button confirmBtn;

    private PickerView item1PickerV,item2PickerV,item3PickerV;
    /**
     * g构造函数
     * @param cxt
     * @param l 选中监听
     */
    public PickerPopWindow(Context cxt, ArrayList<String> item1List,ArrayList<String> item2List,ArrayList<String> item3List, OnPickCompletedListener l) {

        this.mContext = cxt;
        this.mListener = l;
        this.item1List = item1List;
        this.item2List = item2List;
        this.item3List = item3List;

        init();
    }

    private void init() {
        contentView = LayoutInflater.from(mContext).inflate(R.layout.layout_item_picker, null);
        cancelBtn = (Button) contentView.findViewById(R.id.btn_cancel);
        confirmBtn = (Button) contentView.findViewById(R.id.btn_confirm);
        item1PickerV = (PickerView) contentView.findViewById(R.id.picker_year);
        item2PickerV = (PickerView) contentView.findViewById(R.id.picker_month);
        item3PickerV = (PickerView) contentView.findViewById(R.id.picker_day);
        pickerContainerV = contentView.findViewById(R.id.container_picker);
        contentView.findViewById(R.id.picker_day).setVisibility(View.GONE);

        initPickerViews();

        cancelBtn.setOnClickListener(this);
        confirmBtn.setOnClickListener(this);
        contentView.setOnClickListener(this);
        item1PickerV.setOnPickedListener(this);
        item2PickerV.setOnPickedListener(this);

        setTouchable(true);
        setFocusable(true);
        // setOutsideTouchable(true);
        setBackgroundDrawable(new BitmapDrawable());
        setAnimationStyle(R.style.FadeInPopWin);
        setContentView(contentView);
        setWidth(ViewGroup.LayoutParams.MATCH_PARENT);
        setHeight(ViewGroup.LayoutParams.MATCH_PARENT);
    }

    /**
     * 初始化选择器试图
     */
    private void initPickerViews() {
        item1PickerV.setData(item1List);
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

            if (null != mListener)
//                mListener.onAddressPickCompleted(mProvince, mProvinceId, mCity, mCityId);

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
}
