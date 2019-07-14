package com.fire.blogapp;

import android.content.Context;
import android.util.AttributeSet;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

public class FloatingImageButton extends FloatingActionButton {


    public FloatingImageButton(Context context) {
        super(context);
    }

    public FloatingImageButton(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        setPadding(0, 0, 0, 0);
        setScaleType(ScaleType.CENTER_CROP);


    }
}