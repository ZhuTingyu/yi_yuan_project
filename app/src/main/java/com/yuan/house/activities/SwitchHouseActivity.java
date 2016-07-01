package com.yuan.house.activities;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentActivity;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.dimo.utils.StringUtil;
import com.squareup.picasso.Picasso;
import com.yuan.house.R;
import com.yuan.house.common.Constants;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Created by KevinLee on 2016/5/9.
 */
public class SwitchHouseActivity extends FragmentActivity {
    @Inject
    SharedPreferences prefs;

    Context mContext;
    ListView listView;
    LinearLayout back;
    List<JSONObject> houseInfos;
    JSONArray jsonFormatDatum;

    int anonyCounter = 0;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.chat_house_info_layout);

        mContext = this;

        Bundle bundle = getIntent().getExtras();

        if (bundle != null) {
            String raw = bundle.getString(Constants.kHouseSwitchParamsForChatRoom);
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
                intent.putExtra(Constants.kBundleKeyAfterSwitchHouseSelected, houseInfos.get(position).toString());
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
                convertView = LayoutInflater.from(mContext).inflate(R.layout.chat_house_info_adapter, parent, false);

                holder = new ViewHolder(convertView);

                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }

            try {
                JSONObject object = houseInfos.get(position);

                if (TextUtils.isEmpty(object.optString("estate_name"))) {
                    holder.title.setText(StringUtil.formatString(mContext, R.string.txt_required_house_name, ++anonyCounter));
                } else {
                    holder.title.setText(object.optString("estate_name"));
                }

                holder.area.setText(object.optString("acreage") + "㎡");
                StringBuffer sb = new StringBuffer();
                sb.append(object.getString("room_count"));
                sb.append("室");
                sb.append(object.getString("parlour_count"));
                sb.append("厅");
                holder.house_params.setText(sb.toString());

                JSONArray images = object.optJSONArray("images");
                if (images != null && images.length() != 0) {
                    String imageUrl = images.optString(0);
                    Picasso.with(mContext).load(imageUrl).placeholder(R.drawable.img_placeholder).into(holder.imageView);
                } else {
                    Picasso.with(mContext).load(R.drawable.img_placeholder).fit().into(holder.imageView);
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }

            return convertView;
        }
    }

    final class ViewHolder {
        @BindView(R.id.img)
        public ImageView imageView;

        @BindView(R.id.title)
        public TextView title;

        @BindView(R.id.area)
        public TextView area;

        @BindView(R.id.house_params)
        public TextView house_params;

        public ViewHolder(View view) {
            ButterKnife.bind(this, view);
        }
    }
}
