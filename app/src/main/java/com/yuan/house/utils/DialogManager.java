package com.yuan.house.utils;

import android.app.AlertDialog;
import android.content.Context;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;

import com.yuan.skeleton.R;

/**
 * Created by KevinLee on 2016/4/25.
 */
public class DialogManager {

    private AlertDialog.Builder builder;

    private TextView info;
    private Context mContext;

    private AlertDialog dialog;//用于取消AlertDialog.Builder
    /**
     * 构造方法 传入上下文
     */
    public DialogManager(Context context) {
        this.mContext = context;
    }

    // 显示录音的对话框
    public void showRecordingDialog() {

        builder = new AlertDialog.Builder(mContext, R.style.AudioDialog);
        LayoutInflater inflater = LayoutInflater.from(mContext);
        View view = inflater.inflate(R.layout.dialog_recorder,null);

        info = (TextView) view.findViewById(R.id.info);
        builder.setView(view);
        dialog = builder.create();
        Window window = dialog.getWindow();
        WindowManager.LayoutParams layoutParams = window.getAttributes();
        window.setGravity(Gravity.TOP);
        layoutParams.y = 130;
        window.setAttributes(layoutParams);
        dialog.show();
    }

    public void recording(){
        if(dialog != null && dialog.isShowing()){ //显示状态
            info.setText("手指上滑，取消发送");
        }
    }

    // 显示想取消的对话框
    public void wantToCancel() {
        if(dialog != null && dialog.isShowing()){ //显示状态
            info.setText("松开手指，取消发送");
        }
    }

    // 显示时间过短的对话框
    public void tooShort() {
        if(dialog != null && dialog.isShowing()){ //显示状态
            info.setText("录音时间过短");
        }
    }

    // 显示取消的对话框
    public void dimissDialog() {
        if(dialog != null && dialog.isShowing()){ //显示状态
            dialog.dismiss();
            dialog = null;
        }
    }

    /*
    public void updateVoiceLevel(int level) {
        if(dialog != null && dialog.isShowing()){ //显示状态
//          mIcon.setVisibility(View.VISIBLE);
//          mVoice.setVisibility(View.VISIBLE);
//          mLable.setVisibility(View.VISIBLE);

            //设置图片的id
            int resId = mContext.getResources().getIdentifier(v+level, drawable, mContext.getPackageName());
            mVoice.setImageResource(resId);
        }
    }*/

}
