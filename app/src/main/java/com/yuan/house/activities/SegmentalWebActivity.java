package com.yuan.house.activities;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.View;
import android.widget.Button;

import com.yuan.house.R;
import com.yuan.house.ui.fragment.WebViewFragment;

import java.util.List;

import butterknife.BindViews;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.Optional;

/**
 * Created by Alsor Zhou on 16/7/4.
 */

/**
 * 在导航栏上需要有一个@"订房合同审核", @"买卖合同审核"的切换 segmentControl,
 * 切换后调用 web 那边的 segmentedControlValueChanged 函数回传 0，1 给页面
 */
public class SegmentalWebActivity extends WebViewBasedActivity {
    @Nullable @BindViews({R.id.segment_btn_1, R.id.segment_btn_2})
    List<Button> btnSegments;

    WebViewFragment fragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_segmental, false);

        fragment = new WebViewFragment();
        Bundle arguments = new Bundle();
        arguments.putString("url", mUrl);
        fragment.setArguments(arguments);
        mFragmentTransaction.replace(R.id.content_frame, fragment);
        mFragmentTransaction.commit();

        btnSegments.get(0).setSelected(true);
    }

    static final ButterKnife.Action<View> UNSELECTED = new ButterKnife.Action<View>() {
        @Override public void apply(View view, int index) {
            view.setSelected(false);
        }
    };

    @Optional
    @OnClick({R.id.segment_btn_1, R.id.segment_btn_2})
    public void segmentSelected(Button button) {
        ButterKnife.apply(btnSegments, UNSELECTED);

        button.setSelected(true);

        fragment.getBridge().callHandler("segmentedControlValueChanged", button.getId() == R.id.segment_btn_1 ? 0 : 1);
    }

    @Optional
    @OnClick(R.id.btn_back)
    public void close(Button button) {
        finish();
    }
}
