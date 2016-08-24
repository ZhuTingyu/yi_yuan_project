package com.yuan.house.ui.fragment;

import android.app.Activity;
import android.content.SharedPreferences;
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

import org.json.JSONException;
import org.json.JSONObject;

import butterknife.ButterKnife;
import timber.log.Timber;

/**
 * Created by Alsor Zhou on 16/6/6.
 */

public class FragmentBBS extends WebViewBaseFragment {
    protected OnBBSInteractionListener mBridgeListener;

    public FragmentBBS() {
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @return A new instance of fragment WebViewFragment.
     */
    public static FragmentBBS newInstance() {
        FragmentBBS fragment = new FragmentBBS();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }


    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = createView(inflater, R.layout.fragment_webview, container, savedInstanceState);

        Injector.inject(this);

        ButterKnife.bind(this, view);

        Timber.v("onCreateView");

        if (AuthHelper.getInstance().iAmUser()) {
            redirectToLoadUrl(Constants.kWebPageUserBBS);
        } else {
            redirectToLoadUrl(Constants.kWebPageAgencyBBS);
        }

        return view;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        registerHandle();
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mFragmentListener = (OnFragmentInteractionListener) activity;
            mBridgeListener = (OnBBSInteractionListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    protected void registerHandle() {
        super.registerHandle();

        bridge.registerHandler("showSampleMessageBoard", new WebViewJavascriptBridge.WVJBHandler() {
            @Override
            public void handle(String data, WebViewJavascriptBridge.WVJBResponseCallback jsCallback) {
                if (mBridgeListener != null) {
                    mBridgeListener.onShowSampleMessageBoard();
                }
            }
        });

        bridge.registerHandler("showHalfMessageBoard", new WebViewJavascriptBridge.WVJBHandler() {

            @Override
            public void handle(String data, WebViewJavascriptBridge.WVJBResponseCallback jsCallback) {
                if (mBridgeListener != null) {
                    mBridgeListener.onShowHalfMessageBoard();
                }
            }
        });

        bridge.registerHandler("showFullMessageBoard", new WebViewJavascriptBridge.WVJBHandler() {

            @Override
            public void handle(String data, WebViewJavascriptBridge.WVJBResponseCallback jsCallback) {
                if (mBridgeListener != null) {
                    mBridgeListener.onShowFullMessageBoard();
                }
            }
        });

        bridge.registerHandler("webChangeHouse", new WebViewJavascriptBridge.WVJBHandler() {
            @Override
            public void handle(String data, WebViewJavascriptBridge.WVJBResponseCallback jsCallback) {
                if (mBridgeListener != null) {
                    mBridgeListener.onWebChangeHouse(data);
                }
            }
        });

        bridge.registerHandler("setCouponButton", new WebViewJavascriptBridge.WVJBHandler() {
            @Override
            public void handle(String data, WebViewJavascriptBridge.WVJBResponseCallback jsCallback) {
                if (mBridgeListener != null) {
                    mBridgeListener.onSetCouponButton(data);
                }
            }
        });

        bridge.registerHandler("setContractButton", new WebViewJavascriptBridge.WVJBHandler() {
            @Override
            public void handle(String data, WebViewJavascriptBridge.WVJBResponseCallback jsCallback) {
                if (mBridgeListener != null) {
                    mBridgeListener.onSetContractButton(data);
                }
            }
        });

        bridge.registerHandler("setPreConditionButton", new WebViewJavascriptBridge.WVJBHandler() {
            @Override
            public void handle(String data, WebViewJavascriptBridge.WVJBResponseCallback jsCallback) {
                if (mBridgeListener != null) {
                    mBridgeListener.onSetPreConditionButton(data);
                }
            }
        });

        bridge.registerHandler("showBBSView", new WebViewJavascriptBridge.WVJBHandler() {
            @Override
            public void handle(String data, WebViewJavascriptBridge.WVJBResponseCallback jsCallback) {
                Timber.v("showBBSView got:" + data);

                boolean shown = "1".equals(data);

                if (mBridgeListener != null) {
                    mBridgeListener.onShowBBSView(shown);
                }
            }
        });

        bridge.registerHandler("getFirstHouseInfo", new WebViewJavascriptBridge.WVJBHandler() {
            @Override
            public void handle(String data, WebViewJavascriptBridge.WVJBResponseCallback callback) {
                Timber.v("getFirstHouseInfo got:" + data);

                if (mBridgeListener != null) {
                    mBridgeListener.onGetFirstHouseInfo(data, callback);
                }
            }
        });

        bridge.registerHandler("setData", new WebViewJavascriptBridge.WVJBHandler() {
            @Override
            public void handle(String data, WebViewJavascriptBridge.WVJBResponseCallback callback) {
                Timber.v("setData got:" + data);
                JSONObject originObject = null;
                try {
                    originObject = new JSONObject(data);
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                String key = originObject.optString("key");
                String value = originObject.optString("value");

                SharedPreferences.Editor editor = prefs.edit();
                if (value == null || value.equals("null")) {
                    editor.remove(key);
                } else {
                    editor.putString(key, value);
                }
                editor.apply();

                mBridgeListener.onShowSampleMessageBoard();
            }
        });

    }

    public interface OnBBSInteractionListener extends WebViewBaseFragment.OnBridgeInteractionListener {
        void onWebChangeHouse(String data);

        void onShowSampleMessageBoard();

        void onShowHalfMessageBoard();

        void onShowFullMessageBoard();

        void onSetContractButton(String data);

        void onSetPreConditionButton(String data);

        void onGetFirstHouseInfo(String data, WebViewJavascriptBridge.WVJBResponseCallback callback);

        void onShowBBSView(boolean data);

        void onSetCouponButton(String data);
    }
}
