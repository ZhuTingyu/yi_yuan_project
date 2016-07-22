package com.yuan.house.activities;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.baidu.location.Address;
import com.baidu.location.BDLocation;
import com.baidu.location.BDLocationListener;
import com.baidu.location.LocationClient;
import com.baidu.location.LocationClientOption;
import com.baidu.mapapi.map.BaiduMap;
import com.baidu.mapapi.map.MapStatus;
import com.baidu.mapapi.map.MapStatusUpdate;
import com.baidu.mapapi.map.MapStatusUpdateFactory;
import com.baidu.mapapi.map.MapView;
import com.baidu.mapapi.map.MyLocationConfiguration;
import com.baidu.mapapi.map.MyLocationData;
import com.baidu.mapapi.model.LatLng;
import com.baidu.mapapi.search.core.SearchResult;
import com.baidu.mapapi.search.geocode.GeoCodeOption;
import com.baidu.mapapi.search.geocode.GeoCodeResult;
import com.baidu.mapapi.search.geocode.GeoCoder;
import com.baidu.mapapi.search.geocode.OnGetGeoCoderResultListener;
import com.baidu.mapapi.search.geocode.ReverseGeoCodeOption;
import com.baidu.mapapi.search.geocode.ReverseGeoCodeResult;
import com.yuan.house.R;
import com.yuan.house.application.Injector;
import com.yuan.house.common.Constants;
import com.yuan.house.event.LocationEvent;

import org.json.JSONException;
import org.json.JSONObject;

import butterknife.BindView;
import butterknife.ButterKnife;
import de.greenrobot.event.EventBus;
import timber.log.Timber;

/**
 * Created by KevinLee on 2016/5/2.
 */
public class MapActivity extends WebViewBasedActivity implements OnGetGeoCoderResultListener {
    public TCLocationListener locationListener = new TCLocationListener();
    protected MapView mMapView;
    protected BaiduMap baiduMap;
    protected LocationClient locClient;
    boolean isFirstLoc = true;// 是否首次定位
    BDLocation bdLocation;

    @Nullable
    @BindView(R.id.tv_location_field)
    TextView tvLocationField;

    @Nullable
    @BindView(R.id.search_button)
    Button search_button;

    @Nullable
    @BindView(R.id.search_edit)
    EditText searchText;

    private GeoCoder mSearch;
    private LatLng center;
    private double latitude = 0.0;
    private double longitude = 0.0;
    private String city;    //当前城市
    private JSONObject mLocation;
    private MyLocationConfiguration.LocationMode mCurrentMode = MyLocationConfiguration.LocationMode.NORMAL;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.map_activity, true);

        Injector.inject(this);
        ButterKnife.bind(this);

        this.mContext = this;

        bdLocation = new BDLocation();

        Bundle bundle = getIntent().getExtras();

        if (bundle != null) {
            String extra = bundle.getString("location");
            try {
                mLocation = new JSONObject(extra);

                mLocation.optString("lat");
                mLocation.optString("lng");
                mLocation.optString("addr");
            } catch (JSONException e) {
                e.printStackTrace();
            }

        }

        setLeftItem(R.drawable.btn_back, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        setTitleItem("位置");

        setRightItem("选定", new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!TextUtils.isEmpty(bdLocation.getAddrStr())) {
                    EventBus.getDefault().post(new LocationEvent(LocationEvent.LocationEventEnum.UPDATED, bdLocation));
                    Intent intent = new Intent();
                    JSONObject data = new JSONObject();
                    try {
                        String city = bdLocation.getCity();
                        String district = bdLocation.getDistrict();
                        data.put("addr", city + district + bdLocation.getStreet());
                        data.put("city", city);
                        data.put("district", district);
                        data.put("lat", bdLocation.getLatitude());
                        data.put("lng", bdLocation.getLongitude());
                        data.put("province", bdLocation.getProvince());
                        data.put("street", bdLocation.getStreet());
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    intent.putExtra(Constants.kActivityParamFinishSelectLocationOnMap, data.toString());
                    setResult(0, intent);
                    finish();
                }
            }
        });

        this.mMapView = (MapView) findViewById(R.id.bmap);
        this.baiduMap = mMapView.getMap();

        initLocation();
        initMapConfig();
        initViewConfig();

        baiduMap.setOnMapLoadedCallback(new BaiduMap.OnMapLoadedCallback() {
            @Override
            public void onMapLoaded() {
                // TODO: 16/7/20 direct to lat/lng if already has address passin

                locClient.start();
            }
        });
    }

    private void initLocation() {
        baiduMap.setMyLocationEnabled(true);
        LatLng cenpt = new LatLng(mLocation.optDouble("lat"), mLocation.optDouble("lng"));
        //定义地图状态
        MapStatus mMapStatus = new MapStatus.Builder()
                .target(cenpt)
                .zoom(18)
                .build();
        //定义MapStatusUpdate对象，以便描述地图状态将要发生的变化


        MapStatusUpdate mMapStatusUpdate = MapStatusUpdateFactory.newMapStatus(mMapStatus);
        //改变地图状态
        baiduMap.setMapStatus(mMapStatusUpdate);

        //获取当前位置
        locClient = new LocationClient(this);
        locClient.registerLocationListener(locationListener);
        LocationClientOption option = new LocationClientOption();
        option.setOpenGps(true);
        option.setScanSpan(5000);
        option.setCoorType("bd09ll");
        option.setIsNeedAddress(true);
        option.setLocationMode(LocationClientOption.LocationMode.Hight_Accuracy);
        locClient.setLocOption(option);
    }

    private void initMapConfig() {
        // 初始化搜索模块，注册事件监听
        mSearch = GeoCoder.newInstance();
        mSearch.setOnGetGeoCodeResultListener(this);
        //监听地图状态,完成时获取中心点经纬度
        baiduMap.setOnMapStatusChangeListener(new BaiduMap.OnMapStatusChangeListener() {
            @Override
            public void onMapStatusChangeStart(MapStatus mapStatus) {

            }

            @Override
            public void onMapStatusChange(MapStatus mapStatus) {
                //发起搜索状态改变后更新反地理编码
                center = mapStatus.target;
                LatLng ptCenter = new LatLng(center.latitude, center.longitude);
                mSearch.reverseGeoCode(new ReverseGeoCodeOption()
                        .location(ptCenter));
            }

            @Override
            public void onMapStatusChangeFinish(MapStatus mapStatus) {
                center = mapStatus.target;
                latitude = center.latitude;
                longitude = center.longitude;
                LatLng ptCenter = new LatLng(center.latitude, center.longitude);
                mSearch.reverseGeoCode(new ReverseGeoCodeOption().location(ptCenter));
            }
        });

    }

    private void initViewConfig() {
        search_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mSearch.geocode(new GeoCodeOption().city(city).address(searchText.getText().toString()));
            }
        });
    }

    @Override
    public void onGetGeoCodeResult(GeoCodeResult geoCodeResult) {
        if (geoCodeResult == null || geoCodeResult.error != SearchResult.ERRORNO.NO_ERROR) {
            Toast.makeText(mContext, "抱歉，未能找到结果", Toast.LENGTH_LONG)
                    .show();
            return;
        }

        baiduMap.setMapStatus(MapStatusUpdateFactory.newLatLng(geoCodeResult.getLocation()));
    }

    @Override
    public void onGetReverseGeoCodeResult(ReverseGeoCodeResult reverseGeoCodeResult) {
        if (reverseGeoCodeResult == null || reverseGeoCodeResult.error != SearchResult.ERRORNO.NO_ERROR) {
            Toast.makeText(mContext, "抱歉，未能找到结果", Toast.LENGTH_LONG).show();
            return;
        }

        if (reverseGeoCodeResult.getAddressDetail() != null) {
            city = reverseGeoCodeResult.getAddressDetail().city;
        }

        tvLocationField.setText(reverseGeoCodeResult.getAddress());

        bdLocation.setLatitude(latitude);
        bdLocation.setLongitude(longitude);
        Address address = new Address.Builder().province(reverseGeoCodeResult.getAddressDetail().province)
                .city(reverseGeoCodeResult.getAddressDetail().city)
                .district(reverseGeoCodeResult.getAddressDetail().district)
                .street(reverseGeoCodeResult.getAddressDetail().street)
                .build();
        bdLocation.setAddr(address);
        bdLocation.setAddrStr(reverseGeoCodeResult.getAddress());
    }

    @Override
    protected void onResume() {
        super.onResume();
        mMapView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mMapView.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        baiduMap.setMyLocationEnabled(false);
        locClient.stop();
    }

    public class TCLocationListener implements BDLocationListener {
        @Override
        public void onReceiveLocation(BDLocation location) {
            latitude = location.getLatitude();
            longitude = location.getLongitude();

            // map view 销毁后不在处理新接收的位置
            if (location == null) {
                return;
            }
            int locType = location.getLocType();
            if (locType == BDLocation.TypeGpsLocation
                    || locType == BDLocation.TypeNetWorkLocation) {
                MyLocationData locData = new MyLocationData.Builder()
                        .accuracy(70).direction(0)
                        .latitude(location.getLatitude())
                        .longitude(location.getLongitude()).build();
                baiduMap.setMyLocationData(locData);

                MyLocationConfiguration config = new MyLocationConfiguration(
                        mCurrentMode, true, null);
                baiduMap.setMyLocationConfigeration(config);

                LatLng ll = new LatLng(location.getLatitude(), location.getLongitude());

                if (isFirstLoc) {
                    isFirstLoc = false;
                    MapStatusUpdate u = MapStatusUpdateFactory.newLatLngZoom(ll, 17);
                    baiduMap.animateMapStatus(u);

//                    LatLng ptCenter = new LatLng(latitude, longitude);
//                    mSearch.reverseGeoCode(new ReverseGeoCodeOption()
//                            .location(ptCenter));
                }
            }

            Timber.v("onReceiveLocation latitude=" + latitude + " longitude=" + longitude
                    + " locType=" + locType + " address=" + location.getAddrStr());
        }
    }
}
