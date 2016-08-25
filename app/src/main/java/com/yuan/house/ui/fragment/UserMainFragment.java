package com.yuan.house.ui.fragment;

import android.app.Activity;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.baidu.location.BDLocation;
import com.yuan.house.R;
import com.yuan.house.activities.MainActivity;
import com.yuan.house.application.DMApplication;
import com.yuan.house.application.Injector;
import com.yuan.house.common.Constants;
import com.yuan.house.event.LocationEvent;
import com.yuan.house.helper.AuthHelper;

import org.json.JSONException;
import org.json.JSONObject;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

/**
 * Created by KevinLee on 2016/4/21.
 */
public class UserMainFragment extends WebViewBaseFragment {
    @BindView(R.id.rl_center)
    LinearLayout center;
    @BindView(R.id.address)
    TextView address;
    @BindView(R.id.rightItem)
    ImageView rightItem;

    private BDLocation location;

    public static UserMainFragment newInstance() {
        UserMainFragment fragment = new UserMainFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = createView(inflater, R.layout.fragment_main_user, container, savedInstanceState);

        Injector.inject(this);

        ButterKnife.bind(this, view);

        return view;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        BDLocation location = DMApplication.getInstance().getLastActivatedLocation();
        if (location != null) {
            String city = TextUtils.isEmpty(location.getCity()) ? "成都市" : location.getCity();
            String district = TextUtils.isEmpty(location.getDistrict()) ? "" : location.getDistrict();

            address.setText(city + " " + district);
        }

        String url = Constants.kWebPageUserIndex;

        String loginInfo = AuthHelper.getInstance().getUserLoginInfo();
        try {
            JSONObject object = new JSONObject(loginInfo);
            boolean hasAgencyFriends = object.optBoolean("has_agency_friend");

            if (hasAgencyFriends) {
                rightItem.setVisibility(View.VISIBLE);
            } else {
                url = Constants.kWebPageUserIndexFirst;
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        redirectToLoadUrl(url);

    }

    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public void onStop() {
        super.onStop();
    }

    @OnClick({R.id.rl_center, R.id.position, R.id.btn_arrow_down})
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.rl_center:

                JSONObject data = new JSONObject();
                try {
                    if (location != null) {
                        data.put("lat", location.getLatitude());
                        data.put("lng", location.getLongitude());
                        data.put("addr", location.getAddrStr());
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                mBridgeListener.onBridgeSelectMapLocation(data.toString());
                break;
            case R.id.position:
                ((MainActivity) getActivity()).getBottomNavigationBar().selectTab(2);
                break;
            case R.id.btn_arrow_down:
                String url = "resources.html?history";

                JSONObject object = new JSONObject();
                JSONObject innerObject = new JSONObject();
                try {
                    innerObject.put("title", "全网房源");
                    innerObject.put("hasBackButton", true);
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                try {
                    object.put("params", innerObject);
                    mBridgeListener.onBridgeOpenNewLink(url, object);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                break;
        }
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        try {
            mBridgeListener = (OnBridgeInteractionListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    public void onEvent(LocationEvent event) {
        if (event.getEventType() == LocationEvent.LocationEventEnum.UPDATED) {
            location = event.getHolder();

            String city = TextUtils.isEmpty(location.getCity()) ? "成都市" : location.getCity();
            String district = TextUtils.isEmpty(location.getDistrict()) ? "" : location.getDistrict();

            address.setText(city + " " + district);
        }
    }
}
