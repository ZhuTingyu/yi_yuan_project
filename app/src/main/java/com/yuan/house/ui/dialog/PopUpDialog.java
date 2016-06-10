package com.yuan.house.ui.dialog;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.Window;

import com.yuan.house.R;

import timber.log.Timber;

/**
 * Created by Alsor Zhou on 5/7/15.
 */
public class PopUpDialog extends Dialog {
    private PopUpDialog(Context context) {
        super(context);
    }

    public static PopUpDialog newInstance(Context context) {
        return new PopUpDialog(context);
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_NO_TITLE);

        setContentView(R.layout.popup_publish_selection);

        Timber.v("onCreateView PopUpDialog");
    }

    @Override
    public void onStop() {
        super.onStop();
    }
}
