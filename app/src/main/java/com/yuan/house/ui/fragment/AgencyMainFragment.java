package com.yuan.house.ui.fragment;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.yuan.house.R;
import com.yuan.house.application.Injector;
import com.yuan.house.common.Constants;

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
}
