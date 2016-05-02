package com.yuan.skeleton.ui.fragment;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.dimo.utils.StringUtil;
import com.yuan.skeleton.R;
import com.yuan.skeleton.activities.MainActivity;
import com.yuan.skeleton.activities.MapActivity;
import com.yuan.skeleton.application.Injector;
import com.yuan.skeleton.utils.ToastUtil;

import org.json.JSONException;

import java.util.HashMap;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;

/**
 * Created by KevinLee on 2016/4/21.
 */
public class UserMainFragment extends WebViewBaseFragment{

    @InjectView(R.id.rl_center)
    LinearLayout center;
    @InjectView(R.id.address)
    TextView address;

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
        return view;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
    }

    @OnClick({R.id.rl_center})
    public void onClick(View v){
        switch (v.getId()){
            case R.id.rl_center:
                Intent intent = new Intent(getContext(), MapActivity.class);
                startActivityForResult(intent,REQUEST_MAP_CODE);
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
}
