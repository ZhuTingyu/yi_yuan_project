package com.avoscloud.chat.ui.chat;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.avoscloud.chat.ui.entry.SerializableMap;
import com.dimo.utils.StringUtil;
import com.squareup.picasso.Picasso;
import com.yuan.skeleton.R;
import com.yuan.house.common.Constants;
import com.yuan.house.utils.OkHttpClientManager;
import com.zhy.http.okhttp.OkHttpUtils;
import com.zhy.http.okhttp.callback.StringCallback;

import org.json.JSONException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import okhttp3.Call;
import okhttp3.Request;

/**
 * Created by KevinLee on 2016/5/9.
 */
public class HouseInfosActivity extends FragmentActivity {
    private Context mContext;
    private ListView listView;
    private SharedPreferences prefs;
    private LinearLayout back;
    private List<Map<String,Object>> houseInfos;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.chat_house_info_layout);
        mContext = this;
        back = (LinearLayout) findViewById(R.id.back);
        listView = (ListView) findViewById(R.id.listview);
        prefs = PreferenceManager.getDefaultSharedPreferences(this);
        String json = prefs.getString("userLogin",null);
        Log.i("json",json);
        String userId = getUserId(json);
        String token = getToken(json);
        String url = null;
        if(isUserLogin(json))
            url = Constants.kWebServiceSwitchable + userId + "/" + prefs.getString("target_id",null);
        else
            url = Constants.kWebServiceSwitchable + prefs.getString("target_id",null) + "/" + userId;

        OkHttpUtils.get().url(url)
                .addHeader("Content-Type","application/json")
                .addHeader("token",token)
                .build()
                .execute(new StringCallback() {

                    @Override
                    public void onBefore(Request request) {
                        super.onBefore(request);
                        Log.i("onBefore","==================================================");
                    }

                    @Override
                    public void onError(Call call, Exception e) {
                        Log.i("onError",e.getMessage());
                    }

                    @Override
                    public void onResponse(String response) {
                        Log.i("onResponse",response);
                        List<Map<String,Object>> list = JSON.parseObject(response,new TypeReference<List<Map<String, Object>>>(){});
                        houseInfos = new ArrayList<>();
                        for (int i = 0; i < list.size(); i++) {
                            Map<String, Object> objectMap = list.get(i);
                            Map<String,Object> houseMap = JSON.parseObject(objectMap.get("house_info").toString(),new TypeReference<Map<String, Object>>(){});
                            houseInfos.add(houseMap);
                        }
                        Log.i("houseInfos",houseInfos.toString());
                        listView.setAdapter(new HouseInfoAdapter());
                        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                            @Override
                            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                                Intent intent = new Intent();
                                final SerializableMap myMap=new SerializableMap();
                                myMap.setMap(houseInfos.get(position));
                                intent.putExtra("data",myMap);
                                setResult(RESULT_OK,intent);
                                finish();
                            }
                        });

                    }
                });
        back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }

    private String getToken(String json){
        try {
            HashMap<String, String> params = StringUtil.JSONString2HashMap(json);
            return params.get("token");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }

    private String getUserId(String json){
        try {
            HashMap<String, String> params = StringUtil.JSONString2HashMap(json);
            if(params.get("user_info") != null)
                params = StringUtil.JSONString2HashMap(params.get("user_info"));
            else
                params = StringUtil.JSONString2HashMap(params.get("agency_info"));

            return params.get("user_id");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }

    private boolean isUserLogin(String json){
        HashMap<String, String> params = null;
        try {
            params = StringUtil.JSONString2HashMap(json);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        if(params.get("user_info") != null)
            return true;
        else
            return false;
    }

    private class HouseInfoAdapter extends BaseAdapter{

        @Override
        public int getCount() {
            return houseInfos.size();
        }

        @Override
        public Object getItem(int position) {
            return houseInfos.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder holder = null;
            if(convertView == null){
                holder = new ViewHolder();
                convertView = LayoutInflater.from(mContext).inflate(R.layout.chat_house_info_adapter,null);
                holder.img = (ImageView) convertView.findViewById(R.id.img);
                holder.title = (TextView) convertView.findViewById(R.id.title);
                holder.area = (TextView) convertView.findViewById(R.id.area);
                holder.house_params = (TextView) convertView.findViewById(R.id.house_params);
                convertView.setTag(holder);
            }else{
                holder = (ViewHolder) convertView.getTag();
            }

            if(houseInfos.get(position).get("images") != null) {
                List<String> images = JSON.parseObject(houseInfos.get(position).get("images").toString(), List.class);
                Picasso.with(mContext).load(images.get(0)).into(holder.img);
            }
            holder.title.setText(houseInfos.get(position).get("estate_name").toString());
            holder.area.setText(houseInfos.get(position).get("acreage").toString() + "㎡");
            StringBuffer sb = new StringBuffer();
            sb.append(houseInfos.get(position).get("room_count").toString());
            sb.append("室");
            sb.append(houseInfos.get(position).get("parlour_count").toString());
            sb.append("厅");
            holder.house_params.setText(sb.toString());


            return convertView;
        }
    }

    final class ViewHolder {
        public ImageView img;
        public TextView title;
        public TextView area;
        public TextView house_params;
    }
}
