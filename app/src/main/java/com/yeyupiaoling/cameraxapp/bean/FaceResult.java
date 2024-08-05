package com.yeyupiaoling.cameraxapp.bean;

import android.graphics.PointF;

import java.util.List;

/**
 * @author yeyupiaoling
 * @date: 2024-08-05
 * @description: TODO
 */
public class FaceResult {
    public int left;
    public int top;
    public int right;
    public int bottom;

    public float rotX;
    public float rotY;
    public float rotZ;

    public List<PointF> faceContour;
    public List<PointF> leftEyeBrowTopContour;
    public List<PointF> leftEyeBrowBottomContour;
    public List<PointF> rightEyeBrowTopContour;
    public List<PointF> rightEyeBrowBottomContour;
    public List<PointF> leftEyeContour;
    public List<PointF> rightEyeContour;
    public List<PointF> upperLipTopContour;
    public List<PointF> upperLipBottomContour;
    public List<PointF> lowerLipTopContour;
    public List<PointF> lowerLipBottomContour;
    public List<PointF> noseBridgeContour;
    public List<PointF> noseBottomContour;
    public List<PointF> leftCheekContour;
    public List<PointF> rightCheekContour;
}
