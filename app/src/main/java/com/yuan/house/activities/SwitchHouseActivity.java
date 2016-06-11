package com.yuan.house.activities;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.squareup.picasso.Picasso;
import com.yuan.house.R;
import com.yuan.house.common.Constants;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

/**
 * Created by KevinLee on 2016/5/9.
 */
public class SwitchHouseActivity extends FragmentActivity {
    @Inject
    SharedPreferences prefs;

    private Context mContext;
    private ListView listView;
    private LinearLayout back;
    private List<JSONObject> houseInfos;
    private JSONArray jsonFormatDatum;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.chat_house_info_layout);

        mContext = this;

        Bundle bundle = getIntent().getExtras();

        if (bundle != null) {
            String raw = bundle.getString(Constants.kHouseParamsForChatRoom);
            try {
                jsonFormatDatum = new JSONArray(raw);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        listView = (ListView) findViewById(R.id.listview);

        back = (LinearLayout) findViewById(R.id.back);
        back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        prepareDatum(jsonFormatDatum);
    }

    private void prepareDatum(JSONArray data) {
        if (data.length() == 0) return;

        houseInfos = new ArrayList<>();
        for (int i = 0; i < data.length(); i++) {
            JSONObject objectMap;
            try {
                objectMap = data.getJSONObject(i);
                JSONObject houseMap = objectMap.getJSONObject("house_info");
                houseInfos.add(houseMap);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        listView.setAdapter(new HouseInfoAdapter());
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Intent intent = new Intent();
                intent.putExtra("data", houseInfos.get(position).toString());
                setResult(RESULT_OK, intent);
                finish();
            }
        });
    }

    private class HouseInfoAdapter extends BaseAdapter {
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
            ViewHolder holder;
            if (convertView == null) {
                holder = new ViewHolder();

                convertView = LayoutInflater.from(mContext).inflate(R.layout.chat_house_info_adapter, null);
                holder.img = (ImageView) convertView.findViewById(R.id.img);
                holder.title = (TextView) convertView.findViewById(R.id.title);
                holder.area = (TextView) convertView.findViewById(R.id.area);
                holder.house_params = (TextView) convertView.findViewById(R.id.house_params);

                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }

            try {
                JSONObject object = houseInfos.get(position);
                JSONArray images = object.getJSONArray("images");
                if (images != null && images.length() != 0) {
                    // use cover image as placeholder
                    Picasso.with(mContext).load(images.optString(0)).into(holder.img);
                }

                holder.title.setText(object.getString("estate_name"));
                holder.area.setText(object.getString("acreage") + "㎡");
                StringBuffer sb = new StringBuffer();
                sb.append(object.getString("room_count"));
                sb.append("室");
                sb.append(object.getString("parlour_count"));
                sb.append("厅");
                holder.house_params.setText(sb.toString());
            } catch (JSONException e) {
                e.printStackTrace();
            }

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
