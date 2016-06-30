package com.avoscloud.leanchatlib.view;

import android.content.Context;
import android.graphics.PorterDuff;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;
import android.widget.EditText;

import com.avoscloud.leanchatlib.controller.EmotionHelper;
import com.yuan.house.R;

public class EmotionEditText extends EditText {

    public EmotionEditText(Context context) {
        super(context);
        initUnderlineColor();
    }

    public EmotionEditText(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        initUnderlineColor();
    }

    public EmotionEditText(Context context, AttributeSet attrs) {
        super(context, attrs);
        initUnderlineColor();
    }

    @Override
    public void setText(CharSequence text, BufferType type) {
        if (!TextUtils.isEmpty(text)) {
            super.setText(EmotionHelper.replace(getContext(), text.toString()), type);
        } else {
            super.setText(text, type);
        }
    }

    private void initUnderlineColor() {
        setOnFocusChangeListener(new OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
                    getBackground().setColorFilter(getResources().getColor(R.color.chat_edit_text_underline_color), PorterDuff.Mode.SRC_IN);
                } else {
                    getBackground().clearColorFilter();
                }
            }
        });
    }
}
