package com.yuan.house.ui.fragment;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.baidu.location.BDLocation;
import com.baidu.location.BDLocationListener;
import com.baidu.location.LocationClient;
import com.baidu.location.LocationClientOption;
import com.dimo.utils.StringUtil;
import com.yuan.skeleton.R;
import com.yuan.house.activities.MainActivity;
import com.yuan.house.activities.MapActivity;
import com.yuan.house.application.DMApplication;
import com.yuan.house.application.Injector;
import com.yuan.house.utils.ToastUtil;

import org.json.JSONException;

import java.util.HashMap;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;
import timber.log.Timber;

/**
 * Created by KevinLee on 2016/4/21.
 */
public class UserMainFragment extends WebViewBaseFragment{

    @InjectView(R.id.rl_center)
    LinearLayout center;
    @InjectView(R.id.address)
    TextView address;
    public LocationClient locClient;
    public TCLocationListener locationListener;

    private static final int REQUEST_MAP_CODE = 0XFF01;

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

        ButterKnife.reset(this);
        ButterKnife.inject(this, view);

        redirectToLoadUrl("user_index.html");

        initBaiduLocClient();

        return view;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
    }

    @OnClick({R.id.rl_center,R.id.position,R.id.btn_arrow_down})
    public void onClick(View v){
        switch (v.getId()){
            case R.id.rl_center:
                Intent intent = new Intent(getContext(), MapActivity.class);
                startActivityForResult(intent,REQUEST_MAP_CODE);
                break;
            case R.id.position:
                ((MainActivity)getActivity()).getBottomNavigationBar().selectTab(2);
                break;
            case R.id.btn_arrow_down:
                String url = "resources.html";
                HashMap<String,String> map = new HashMap<String, String>();
                map.put("params","{\"title\":\"全网房源\",\"hasBackButton\":true}");
                ((MainActivity)getActivity()).openLinkInNewActivity(url,map);
                break;
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == REQUEST_MAP_CODE && resultCode == Activity.RESULT_OK){
            //获取地图返回的地理位置
            String mapJson = data.getStringExtra("mapJson");
            try {
                HashMap<String, String> hashMap = StringUtil.JSONString2HashMap(mapJson);
                address.setText(hashMap.get("street"));
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    private void initBaiduLocClient() {
        locClient = new LocationClient(getContext());
        locClient.setDebug(true);
        LocationClientOption option = new LocationClientOption();
        option.setLocationMode(LocationClientOption.LocationMode.Hight_Accuracy);
        option.setScanSpan(5000);
        option.setCoorType("bd09ll");
        option.setIsNeedAddress(true);
        locClient.setLocOption(option);

        locationListener = new TCLocationListener();
        locClient.registerLocationListener(locationListener);
        locClient.start();
    }

    public class TCLocationListener implements BDLocationListener {
        @Override
        public void onReceiveLocation(BDLocation location) {
            double latitude = location.getLatitude();
            double longitude = location.getLongitude();
            int locType = location.getLocType();

            Timber.v("onReceiveLocation latitude=" + latitude + " longitude=" + longitude
                    + " locType=" + locType + " address=" + location.getAddrStr());
            address.setText(location.getStreet());
            locClient.stop();
        }
    }
}
