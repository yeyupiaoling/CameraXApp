package com.yeyupiaoling.cameraxapp.view;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;

import androidx.annotation.NonNull;

/**
 * @author yeyupiaoling
 * @date: 2024-08-02
 * @description: 自定义CameraX点击事件
 */
public class CameraXPreviewViewTouchListener implements View.OnTouchListener {
    private final GestureDetector mGestureDetector;
    private CustomTouchListener mCustomTouchListener;
    private final ScaleGestureDetector mScaleGestureDetector;

    public CameraXPreviewViewTouchListener(Context context) {
        mGestureDetector = new GestureDetector(context, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onSingleTapConfirmed(@NonNull MotionEvent e) {
                if (mCustomTouchListener != null) {
                    mCustomTouchListener.click(e.getX(), e.getY());
                }
                return true;
            }

            @Override
            public void onLongPress(@NonNull MotionEvent e) {
                if (mCustomTouchListener != null) {
                    mCustomTouchListener.longPress(e.getX(), e.getY());
                }
            }

            @Override
            public boolean onDoubleTap(@NonNull MotionEvent e) {
                if (mCustomTouchListener != null) {
                    mCustomTouchListener.doubleClick(e.getX(), e.getY());
                }
                return true;
            }

            // 可以根据需要添加其他手势处理
        });

        mScaleGestureDetector = new ScaleGestureDetector(context, new ScaleGestureDetector.SimpleOnScaleGestureListener() {
            @Override
            public boolean onScale(@NonNull ScaleGestureDetector detector) {
                float delta = detector.getScaleFactor();
                if (mCustomTouchListener != null) {
                    mCustomTouchListener.zoom(delta);
                }
                return true;
            }
        });
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouch(View v, MotionEvent event) {
        mScaleGestureDetector.onTouchEvent(event);
        if (!mScaleGestureDetector.isInProgress()) {
            mGestureDetector.onTouchEvent(event);
        }
        return true;
    }

    // 设置监听
    public void setCustomTouchListener(CustomTouchListener customTouchListener) {
        mCustomTouchListener = customTouchListener;
    }

    // 操作接口
    public interface CustomTouchListener {
        // 放大缩小
        void zoom(float delta);

        // 点击
        void click(float x, float y);

        // 双击
        void doubleClick(float x, float y);

        // 长按
        void longPress(float x, float y);
    }
}