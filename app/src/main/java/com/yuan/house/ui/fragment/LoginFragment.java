package com.yuan.house.ui.fragment;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.baidu.location.BDLocation;
import com.yuan.house.application.DMApplication;
import com.yuan.house.application.Injector;
import com.yuan.house.common.Constants;
import com.yuan.house.R;
import com.yuan.house.event.LocationEvent;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Created by KevinLee on 2016/4/22.
 */
public class LoginFragment extends WebViewBaseFragment {

    @BindView(R.id.fragment_login_address)
    TextView address;

    public static LoginFragment newInstance() {
        LoginFragment fragment = new LoginFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = createView(inflater, R.layout.fragment_login, container, savedInstanceState);

        Injector.inject(this);

        ButterKnife.bind(this, view);

        redirectToLoadUrl(Constants.kWebPageLogin);

        return view;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        BDLocation location = DMApplication.getInstance().getLastActivatedLocation();
        if (location != null) {
            address.setText(location.getCity()+location.getDistrict());
        }
    }

    public void onEvent(LocationEvent event) {
        if (event.getEventType() == LocationEvent.LocationEventEnum.UPDATED) {
            BDLocation location = event.getHolder();

            String city = TextUtils.isEmpty(location.getCity()) ? "成都市" : location.getCity();
            String district = TextUtils.isEmpty(location.getDistrict()) ? "" : location.getDistrict();

            address.setText(city + " " + district);
        }
    }

}
