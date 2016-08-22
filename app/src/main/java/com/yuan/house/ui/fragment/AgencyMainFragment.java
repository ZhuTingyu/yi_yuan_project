package com.yuan.house.ui.fragment;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.baidu.location.Address;
import com.baidu.location.BDLocation;
import com.yuan.house.R;
import com.yuan.house.application.Injector;
import com.yuan.house.common.Constants;
import com.yuan.house.event.LocationEvent;
import com.yuan.house.event.PageEvent;

import butterknife.BindView;
import butterknife.ButterKnife;
import de.greenrobot.event.EventBus;

/**
 * Created by KevinLee on 2016/4/21.
 */
public class AgencyMainFragment extends WebViewBaseFragment {

    @BindView(R.id.rightItem)
    Button rightItem;

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

        String city = prefs.getString(Constants.kPrefsLastSelectedCityFromMap, null);
        if (!TextUtils.isEmpty(city)) {
            rightItem.setText(city);
        }

        rightItem.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                EventBus.getDefault().post(new PageEvent(PageEvent.PageEventEnum.GET_LOCATION, null));
            }
        });
    }

    public void onEvent(LocationEvent event) {
        if (event.getEventType() == LocationEvent.LocationEventEnum.UPDATED) {
            BDLocation location = event.getHolder();
            Address address = location.getAddress();
            rightItem.setText(address.city);

            getWebView().reload();
        }
    }
}
