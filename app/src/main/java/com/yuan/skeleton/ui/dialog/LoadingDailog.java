package com.yuan.skeleton.ui.dialog;

import android.app.ProgressDialog;
import android.content.Context;

/**
 * Created by yj on 2015/7/30.
 */
public class LoadingDailog extends ProgressDialog {

    public LoadingDailog(Context context) {
        super(context);
    }

    public static LoadingDailog newInstance(Context context) {
        return new LoadingDailog(context);
    }

}
