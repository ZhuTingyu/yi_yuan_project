package com.yuan.house.ui.fragment;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;

import com.yuan.house.common.Constants;
import com.yuan.house.R;
import com.yuan.house.application.Injector;

import butterknife.ButterKnife;

/**
 * Created by KevinLee on 2016/4/21.
 */
public class AgencyMainFragment extends WebViewBaseFragment {

    public static AgencyMainFragment newInstance() {
        AgencyMainFragment fragment = new AgencyMainFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = createView(inflater, R.layout.fragment_main_agency, container, savedInstanceState);

        Injector.inject(this);

        ButterKnife.bind(this, view);

        redirectToLoadUrl(Constants.kWebPageAgencyIndex);

        return view;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        EditText searchBar = ButterKnife.findById(view, R.id.search_bar);
        searchBar.setSingleLine();
        searchBar.setImeOptions(EditorInfo.IME_ACTION_SEARCH);

        searchBar.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                    String content = v.getText().toString();
                    if (!TextUtils.isEmpty(content)) {
                        getBridge().callHandler("searchContent", content);
                    }
                    return true;
                }
                return false;
            }
        });
    }
}
