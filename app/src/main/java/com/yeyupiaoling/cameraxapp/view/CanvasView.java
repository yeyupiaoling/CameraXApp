package com.yeyupiaoling.cameraxapp.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.NonNull;

import com.yeyupiaoling.cameraxapp.bean.FaceResult;

import java.util.List;

public class CanvasView extends View {
    private static final String TAG = CanvasView.class.getSimpleName();
    private final Paint rectPaint = new Paint();
    private int width;
    private int height;
    private float widthScale;
    private float heightScale;
    private List<FaceResult> faceResults;
    private boolean isBackCamera = true;

    public CanvasView(final Context context, final AttributeSet attrs) {
        super(context, attrs);
        // 框的画笔
        rectPaint.setStyle(Paint.Style.STROKE);
        rectPaint.setColor(Color.GREEN);
        rectPaint.setStrokeWidth(5.0f);
    }

    // 获取View的大小
    public void setTextureViewDimen(int width, int height, int imageWidth, int imageHeight) {
        this.width = width;
        this.height = height;
        this.widthScale = (float) width / imageWidth;
        this.heightScale = (float) height / imageHeight;
    }

    // 设置摄像头
    public void setCamera(boolean isBack) {
        this.isBackCamera = isBack;
    }

    // 获取预测数据并进行绘画
    public void populateResult(List<FaceResult> faceResults) {
        this.faceResults = faceResults;
        postInvalidate();
    }

    @Override
    public void draw(@NonNull Canvas canvas) {
        super.draw(canvas);
        if (faceResults == null || faceResults.isEmpty()) {
            canvas.drawColor(Color.TRANSPARENT);
            return;
        }
        for (int i = 0; i < faceResults.size(); i++) {
            FaceResult faceResult = faceResults.get(i);
            // 获取框的位置
            float left = faceResult.left * widthScale;
            float right = faceResult.right * widthScale;
            float top = faceResult.top * heightScale;
            float bottom = faceResult.bottom * heightScale;
            // 绘制框
            if (isBackCamera) {
                canvas.drawRoundRect(left, top, right, bottom, 10, 10, rectPaint);
            } else {
                canvas.drawRoundRect(this.width - right, top, this.width - left, bottom, 10, 10, rectPaint);
            }
        }
    }
}
