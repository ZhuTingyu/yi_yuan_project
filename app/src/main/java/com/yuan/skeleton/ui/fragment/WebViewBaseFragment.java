package com.yuan.skeleton.ui.fragment;

import android.app.Activity;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebChromeClient;
import android.webkit.WebStorage;
import android.webkit.WebView;

import com.dimo.utils.StringUtil;

import com.yuan.skeleton.R;
import com.yuan.skeleton.application.DMApplication;
import com.yuan.skeleton.application.Injector;

import java.util.HashMap;

import butterknife.ButterKnife;
import butterknife.InjectView;
import timber.log.Timber;

/**
 * Created by Alsor Zhou on 8/13/15.
 */
public class WebViewBaseFragment extends Fragment {
    protected OnFragmentInteractionListener mListener;
    protected String mUrl;

    @InjectView(R.id.webview)
    WebView webView;

    HashMap<String, String> additionalHttpHeaders;

    public HashMap<String, String> getAdditionalHttpHeaders() {
        return additionalHttpHeaders;
    }

    public WebView getWebView() {
        return webView;
    }


    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle arguments = getArguments();
        mUrl = arguments.getString("url");
    }

    @Override
    public void onDestroyView() {
        WebStorage.getInstance().deleteAllData();
        ViewGroup holder = ButterKnife.findById(getActivity(), R.id.webview_parent);
        if (holder != null) {
            holder.removeView(webView);
        }
        webView.removeAllViews();
        webView.destroy();

        super.onDestroyView();

        ButterKnife.reset(this);
    }

    public View createView(LayoutInflater inflater, int resId, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(resId, container, false);

        Injector.inject(this);

        ButterKnife.inject(this, view);

        Timber.v("onCreateView");

        if (mListener != null) {
            mListener.onFragmentInteraction(this);

            webView.setInitialScale(getResources().getDisplayMetrics().widthPixels * 100 / 360);

            webView.getSettings().setJavaScriptEnabled(true);
            webView.getSettings().setAllowFileAccess(true);
            webView.setWebChromeClient(new WebChromeClient());
            webView.setHorizontalScrollBarEnabled(false);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                WebView.setWebContentsDebuggingEnabled(true);
            }

            if (!TextUtils.isEmpty(mUrl)) {
                redirectToLoadUrl(mUrl, additionalHttpHeaders);
            }
        }

        return view;
    }

    /**
     * Open new url in webview
     *
     * @param url destination file url without base url
     */
    public void redirectToLoadUrl(String url, HashMap<String, String> additionalHttpHeaders) {
        if (webView == null) {
            mUrl = url;
            this.additionalHttpHeaders = additionalHttpHeaders;
            return;
        }

        String htmlExtractedFolder = DMApplication.getInstance().getHtmlExtractedFolder();

        mUrl = htmlExtractedFolder + "/" + url;

        Timber.i("URL - " + mUrl);

        if (StringUtil.isValidHTTPUrl(mUrl)) {
            // url is web link
            if (additionalHttpHeaders != null) {
                webView.loadUrl(mUrl, additionalHttpHeaders);
            } else {
                webView.loadUrl(mUrl);
            }
        } else {
            webView.loadUrl("file:///" + mUrl);
        }
    }

    public void redirectToLoadUrl(String url) {
        redirectToLoadUrl(url, null);
    }


    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mListener = (OnFragmentInteractionListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p/>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnFragmentInteractionListener {
        void onFragmentInteraction(WebViewBaseFragment fragment);
    }
}
