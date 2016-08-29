package com.yuan.house.ui.fragment;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.dimo.web.WebViewJavascriptBridge;
import com.yuan.house.R;
import com.yuan.house.application.Injector;
import com.yuan.house.common.Constants;
import com.yuan.house.helper.AuthHelper;

import butterknife.ButterKnife;
import butterknife.OnClick;
import timber.log.Timber;

/**
 * 用户端和中介端合并在一个Fragment里
 * Created by KevinLee on 2016/4/24.
 */
public class CouponFragment extends WebViewBaseFragment {
    public static CouponFragment newInstance() {
        CouponFragment fragment = new CouponFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    @OnClick(R.id.rightItem)
    public void onViewClick(View view) {
        getBridge().callHandler("onRightItemClick");
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = createView(inflater, R.layout.fragment_coupon, container, savedInstanceState);

        Injector.inject(this);

        ButterKnife.bind(this, view);

        if (AuthHelper.getInstance().iAmUser()) {
            redirectToLoadUrl(Constants.kWebPageUserCoupon);
        } else {
            redirectToLoadUrl(Constants.kWebPageAgencyCoupon);
        }

        return view;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        getBridge().registerHandler("setCouponSwitchEnable", new WebViewJavascriptBridge.WVJBHandler() {
            @Override
            public void handle(String data, WebViewJavascriptBridge.WVJBResponseCallback jsCallback) {
                Timber.v("setCouponSwitchEnable");


            }
        });
    }
}
