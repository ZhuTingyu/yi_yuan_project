package com.yuan.skeleton.base;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.ContextCompat;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.bugtags.library.Bugtags;

import com.yuan.skeleton.R;
import com.yuan.skeleton.application.Injector;

import javax.inject.Inject;

import butterknife.ButterKnife;
import timber.log.Timber;

/**
 * Created by alsor on 2/13/15.
 */
public class BaseFragmentActivity extends FragmentActivity {

    private static final java.lang.String TAG = BaseFragmentActivity.class.getSimpleName();

    @Inject
    public SharedPreferences prefs;

    @Inject
    public Context mContext;

    protected View mTopBar;
    protected View mTabBar;

    public View getTopBar() {
        return mTopBar;
    }

    public View getTabBar() {
        return mTabBar;
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Injector.inject(this);
    }

    @Override
    protected void onDestroy() {

        super.onDestroy();
    }

    @Override
    protected void onResume() {
        super.onResume();
        //注：回调 1
        Bugtags.onResume(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        //注：回调 2
        Bugtags.onPause(this);
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        //注：回调 3
        Bugtags.onDispatchTouchEvent(this, event);
        return super.dispatchTouchEvent(event);
    }

    @Override
    public void setContentView(int layoutResId) {
        setContentView(layoutResId, false, false);
    }

    public void setContentView(int layoutResId, boolean hasTopBar) {
        setContentView(layoutResId, hasTopBar, false);
    }

    /**
     * Override default setContentView to support inject topbar / tabbar
     *
     * @param layoutResId  contentView layout resId
     * @param hasBottomBar whether has tab bar to show
     */
    public void setContentView(int layoutResId, boolean hasTopBar, boolean hasBottomBar) {
        LayoutInflater inflater = LayoutInflater.from(this);
        ViewGroup viewGroup = (ViewGroup) inflater.inflate(layoutResId, null);

        if (hasTopBar) {
            mTopBar = inflater.inflate(R.layout.layout_topbar, null);

            int raw = (int) (getResources().getDimension(R.dimen.topbar_height) / getResources().getDisplayMetrics().density);
            int height = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, raw, getResources().getDisplayMetrics());
            viewGroup.addView(mTopBar, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, height));
        }

        if (hasBottomBar) {
            mTabBar = inflater.inflate(R.layout.layout_tabbar, null);

            int raw = (int) (getResources().getDimension(R.dimen.tabbar_height) / getResources().getDisplayMetrics().density);
            int height = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, raw, getResources().getDisplayMetrics());
            RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, height);
            layoutParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);

            viewGroup.addView(mTabBar, layoutParams);
        }

        super.setContentView(viewGroup);

        // Used to inject views with the Butterknife library
        ButterKnife.inject(this);
    }

    /**
     * Set top bar right button
     *
     * @param view
     */
    public void setRightItem(View view) {
        mTopBar.findViewById(R.id.topbar_right_btn).setVisibility(View.GONE);
        mTopBar.findViewById(R.id.topbar_right_textbtn).setVisibility(View.GONE);
    }

    /**
     * Set top bar right button
     *
     * @param resId
     */
    public void setRightItem(int resId, View.OnClickListener listener) {
        mTopBar.findViewById(R.id.topbar_right_textbtn).setVisibility(View.GONE);

        ImageButton item = (ImageButton) mTopBar.findViewById(R.id.topbar_right_btn);
        item.setVisibility(View.VISIBLE);
        item.setOnClickListener(listener);

        Drawable drawable = ContextCompat.getDrawable(getApplicationContext(), resId);
//        int h = drawable.getIntrinsicHeight();
//        int w = drawable.getIntrinsicWidth();
//        drawable.setBounds(0, 0, w, h);

        if (drawable != null) {
            item.setImageDrawable(drawable);
        }
    }

    /**
     * Set top bar right button
     *
     * @param title
     */
    public void setRightItem(String title, View.OnClickListener listener) {
        mTopBar.findViewById(R.id.topbar_right_btn).setVisibility(View.GONE);

        Button item = (Button) mTopBar.findViewById(R.id.topbar_right_textbtn);
        item.setOnClickListener(listener);

        if (title != null) {
            item.setVisibility(View.VISIBLE);

            item.setText(title);
        }
    }

    /**
     * Set top bar left button
     *
     * @param view
     */
    protected void setLeftItem(View view) {
        View item = (View) mTopBar.findViewById(R.id.topbar_left_btn);
        mTopBar.findViewById(R.id.topbar_left_textbtn).setVisibility(View.GONE);

        if (view == null) {
            item.setVisibility(View.GONE);
        } else {
            item.setVisibility(View.VISIBLE);
        }
    }

    /**
     * Set top bar left button
     *
     * @param resId
     */
    protected void setLeftItem(int resId, View.OnClickListener listener) {
        ImageButton item = (ImageButton) mTopBar.findViewById(R.id.topbar_left_btn);
        mTopBar.findViewById(R.id.topbar_left_textbtn).setVisibility(View.GONE);

        item.setVisibility(View.VISIBLE);

        item.setOnClickListener(listener);
        Drawable drawable = ContextCompat.getDrawable(getApplicationContext(), resId);
        int h = drawable.getIntrinsicHeight();
        int w = drawable.getIntrinsicWidth();

        drawable.setBounds(0, 0, w, h);
        if (drawable != null) {
            item.setImageDrawable(drawable);
        }
    }

    /**
     * Set top bar left button
     *
     * @param text
     */
    protected void setLeftItem(String text, View.OnClickListener listener) {
        Button item = (Button) mTopBar.findViewById(R.id.topbar_left_textbtn);
        mTopBar.findViewById(R.id.topbar_left_btn).setVisibility(View.GONE);

        item.setVisibility(View.VISIBLE);

        item.setOnClickListener(listener);
        item.setCompoundDrawables(null, null, null, null);
        item.setText(text);
    }

    /**
     * Set top bar left button
     *
     * @param resId
     */
    protected void setLeftItem(int resId, String text, View.OnClickListener listener) {
        Button item = (Button) mTopBar.findViewById(R.id.topbar_left_textbtn);
        mTopBar.findViewById(R.id.topbar_left_btn).setVisibility(View.GONE);

        item.setVisibility(View.VISIBLE);
        item.setText(text);

        item.setOnClickListener(listener);
        Drawable drawable = ContextCompat.getDrawable(getApplicationContext(), resId);
        int h = drawable.getIntrinsicHeight();
        int w = drawable.getIntrinsicWidth();

        drawable.setBounds(0, 0, w, h);
        if (drawable != null) {
            item.setCompoundDrawables(null, drawable, null, null);
        }
    }

    /**
     * Set top bar center item
     *
     * @param resId
     */
    public void setTitleItem(int resId) {
        TextView textView = (TextView) mTopBar.findViewById(R.id.topbar_title_item);
        textView.setVisibility(View.VISIBLE);
        textView.setText(getString(resId));

        mTopBar.findViewById(R.id.topbar_title_button).setVisibility(View.GONE);
    }

    /**
     * Set top bar center item
     *
     * @param title
     */
    public void setTitleItem(CharSequence title) {
        TextView textView = (TextView) mTopBar.findViewById(R.id.topbar_title_item);
        textView.setVisibility(View.VISIBLE);
        textView.setText(title);

        mTopBar.findViewById(R.id.topbar_title_button).setVisibility(View.GONE);
    }

    public void setTitleItem(String title) {
        TextView textView = (TextView) mTopBar.findViewById(R.id.topbar_title_button);
        textView.setText(title);
    }

    public void setTitleItem(int resId, String title, View.OnClickListener listener) {
        Button item = (Button) mTopBar.findViewById(R.id.topbar_title_button);
        mTopBar.findViewById(R.id.topbar_title_item).setVisibility(View.GONE);
        item.setVisibility(View.VISIBLE);
        item.setText(title);

        item.setOnClickListener(listener);
        Drawable drawable = ContextCompat.getDrawable(getApplicationContext(), resId);
        int h = drawable.getIntrinsicHeight();
        int w = drawable.getIntrinsicWidth();

        drawable.setBounds(0, 0, w, h);
        if (drawable != null) {
            item.setCompoundDrawables(null, null, drawable, null);
        }
    }

    protected void logI(String tag, String msg) {
        Timber.i(tag, msg);
    }

    protected void logI(String msg) {
        logI(TAG, msg);
    }
}
