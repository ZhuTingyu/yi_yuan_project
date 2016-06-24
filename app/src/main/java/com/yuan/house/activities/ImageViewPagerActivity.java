package com.yuan.house.activities;

import android.content.Intent;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.yuan.house.R;
import com.yuan.house.utils.ImageUtil;

import java.util.ArrayList;

public class ImageViewPagerActivity extends AppCompatActivity implements ViewPager.OnPageChangeListener {

    public static final String KEY_IMAGES = "KEY_IMAGES";
    public static final String KEY_TITLE = "KEY_TITLE";

    private ViewPager viewPager;
    private Toolbar toolbar;

    /**
     * 装点点的ImageView数组
     */
    private ImageView[] tips;

    /**
     * 装ImageView数组
     */
    private ImageView[] mImageViews;

    private ArrayList<String> images;
    private String title;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent intent = getIntent();
        if (intent != null) {
            Bundle extras = intent.getExtras();
            if (extras != null) {
                images = extras.getStringArrayList(KEY_IMAGES);
                title = extras.getString(KEY_TITLE);
            }
        }

        setContentView(R.layout.activity_image_view_pager);

        viewPager = (ViewPager) findViewById(R.id.viewPager);
        toolbar = (Toolbar) findViewById(R.id.toolbar);

    //    initIndictor();

        initToolbar();

        loadImages();

        //设置Adapter
        viewPager.setAdapter(new MyAdapter());
        //设置监听，主要是设置点点的背景
        viewPager.setOnPageChangeListener(this);
        //设置ViewPager的默认项, 设置为长度的100倍，这样子开始就能往左滑动
        //viewPager.setCurrentItem((mImageViews.length) * 100);
        viewPager.setCurrentItem(0);

    }

    private void initIndictor() {
        ViewGroup group = (ViewGroup)findViewById(R.id.viewGroup);
        tips = new ImageView[images.size()];

        for(int i=0; i<tips.length; i++){
            ImageView imageView = new ImageView(this);
            imageView.setLayoutParams(new ViewPager.LayoutParams(this, null));
            ViewGroup.LayoutParams lp = imageView.getLayoutParams();
            lp.height = 10;
            lp.width = 10;
            imageView.setLayoutParams(lp);

            tips[i] = imageView;

            if(i == 0){
                //tips[i].setBackgroundResource(R.drawable.page_indicator_focused);
                tips[i].setBackgroundResource(R.color.red);
            }else{
                //tips[i].setBackgroundResource(R.drawable.page_indicator_unfocused);
                tips[i].setBackgroundResource(R.color.white);
            }

            LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                    new ViewGroup.LayoutParams(ViewPager.LayoutParams.WRAP_CONTENT,
                    ViewPager.LayoutParams.WRAP_CONTENT));
            layoutParams.leftMargin = 5;
            layoutParams.rightMargin = 5;
            group.addView(imageView, layoutParams);
        }
    }

    private void initToolbar() {
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setTitle(title);
        }
    }

    private void loadImages() {
        mImageViews = new ImageView[images.size()];
        for(int i=0; i<mImageViews.length; i++){
            ImageView imageView = new ImageView(this);
            mImageViews[i] = imageView;
            ImageUtil.loadImageThumbnail(imageView, images.get(i), 1);
            //imageView.setBackgroundResource(imgIdArray[i]);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
    }

    public class MyAdapter extends PagerAdapter {

        @Override
        public int getCount() {
            return mImageViews.length;
        }

        @Override
        public boolean isViewFromObject(View arg0, Object arg1) {
            return arg0 == arg1;
        }

        @Override
        public void destroyItem(View container, int position, Object object) {
           // ((ViewPager)container).removeView(mImageViews[position % mImageViews.length]);
            ((ViewPager)container).removeView(mImageViews[position]);
        }

        @Override
        public Object instantiateItem(View container, int position) {
            ((ViewPager) container).addView(mImageViews[position]);
            return mImageViews[position];
        }
    }

    @Override
    public void onPageScrollStateChanged(int arg0) {

    }

    @Override
    public void onPageScrolled(int arg0, float arg1, int arg2) {

    }

    @Override
    public void onPageSelected(int arg0) {
 //       setImageBackground(arg0 % mImageViews.length);
    }

    /**
     * 设置选中的tip的背景
     * @param selectItems
     */
    private void setImageBackground(int selectItems){
        for(int i=0; i<tips.length; i++){
            if(i == selectItems){
                //tips[i].setBackgroundResource(R.drawable.page_indicator_focused);
                tips[i].setBackgroundResource(R.color.red);
            }else{
                //tips[i].setBackgroundResource(R.drawable.page_indicator_unfocused);
                tips[i].setBackgroundResource(R.color.white);
            }
        }
    }

}
