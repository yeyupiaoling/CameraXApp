package com.yeyupiaoling.cameraxapp.utils;

import android.graphics.Bitmap;
import android.graphics.Rect;
import android.media.Image;
import android.util.Log;

import androidx.camera.core.ExperimentalGetImage;
import androidx.camera.core.ImageProxy;

import com.yeyupiaoling.cameraxapp.bean.FaceResult;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceContour;
import com.google.mlkit.vision.face.FaceDetection;
import com.google.mlkit.vision.face.FaceDetector;
import com.google.mlkit.vision.face.FaceDetectorOptions;

import java.util.ArrayList;
import java.util.List;

/**
 * @author yeyupiaoling
 * @date: 2024-08-05
 * @description: 相机人脸检测器
 */
public class CameraFaceDetector {
    private static final String TAG = CameraFaceDetector.class.getSimpleName();
    private final ResultListener listener;
    private FaceDetector faceDetector;

    public CameraFaceDetector(ResultListener listener) {
        this.listener = listener;
        FaceDetectorOptions options = new FaceDetectorOptions.Builder()
                // 在检测人脸时更注重速度还是准确性
                .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
                // 是否检测面部特征的轮廓
                .setContourMode(FaceDetectorOptions.CONTOUR_MODE_NONE)
                // 是否尝试识别面部“特征点”：眼睛、耳朵、鼻子、 脸颊、嘴巴等
                .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_NONE)
                // 是否将人脸分为不同类别，例如“微笑”、 以及“睁大眼睛”的程度
                .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_NONE)
                .build();
        faceDetector = FaceDetection.getClient(options);
    }

    // 检测人脸
    @ExperimentalGetImage
    public void predict(ImageProxy imageProxy) {
        long time = System.currentTimeMillis();
        int rotation = imageProxy.getImageInfo().getRotationDegrees();
        Image mediaImage = imageProxy.getImage();
        if (mediaImage == null) {
            Log.e(TAG, "predict: 图片为空");
            imageProxy.close();
            return;
        }
        InputImage image = InputImage.fromMediaImage(mediaImage, rotation);
        Bitmap bitmap = Utils.rotateBitmap(imageProxy.toBitmap(), rotation);
        faceDetector.process(image)
                .addOnSuccessListener(faces -> {
                    Log.d(TAG, "onImageAnalysis: 消耗时间：" + (System.currentTimeMillis() - time) + "ms");
                    List<FaceResult> faceResults = new ArrayList<>();
                    for (Face face : faces) {
                        Rect box = face.getBoundingBox();
                        float rotX = face.getHeadEulerAngleX();
                        float rotY = face.getHeadEulerAngleY();
                        float rotZ = face.getHeadEulerAngleZ();
                        FaceResult faceResult = new FaceResult();
                        faceResult.left = box.left;
                        faceResult.top = box.top;
                        faceResult.right = box.right;
                        faceResult.bottom = box.bottom;
                        faceResult.rotX = rotX;
                        faceResult.rotY = rotY;
                        faceResult.rotZ = rotZ;

                        FaceContour faceContour = face.getContour(FaceContour.FACE);
                        if (faceContour != null) {
                            faceResult.faceContour = faceContour.getPoints();
                        }
                        FaceContour leftEyeBrowTopContour = face.getContour(FaceContour.LEFT_EYEBROW_TOP);
                        if (leftEyeBrowTopContour != null) {
                            faceResult.leftEyeBrowTopContour = leftEyeBrowTopContour.getPoints();
                        }
                        FaceContour leftEyeBrowBottomContour = face.getContour(FaceContour.LEFT_EYEBROW_BOTTOM);
                        if (leftEyeBrowBottomContour != null) {
                            faceResult.leftEyeBrowBottomContour = leftEyeBrowBottomContour.getPoints();
                        }
                        FaceContour rightEyeBrowTopContour = face.getContour(FaceContour.RIGHT_EYEBROW_TOP);
                        if (rightEyeBrowTopContour != null) {
                            faceResult.rightEyeBrowTopContour = rightEyeBrowTopContour.getPoints();
                        }
                        FaceContour rightEyeBrowBottomContour = face.getContour(FaceContour.RIGHT_EYEBROW_BOTTOM);
                        if (rightEyeBrowBottomContour != null) {
                            faceResult.rightEyeBrowBottomContour = rightEyeBrowBottomContour.getPoints();
                        }
                        FaceContour leftEye = face.getContour(FaceContour.LEFT_EYE);
                        if (leftEye != null) {
                            faceResult.leftEyeContour = leftEye.getPoints();
                        }
                        FaceContour rightEye = face.getContour(FaceContour.RIGHT_EYE);
                        if (rightEye != null) {
                            faceResult.rightEyeContour = rightEye.getPoints();
                        }
                        FaceContour upperLipTop = face.getContour(FaceContour.UPPER_LIP_TOP);
                        if (upperLipTop != null) {
                            faceResult.upperLipTopContour = upperLipTop.getPoints();
                        }
                        FaceContour upperLipBottom = face.getContour(FaceContour.UPPER_LIP_BOTTOM);
                        if (upperLipBottom != null) {
                            faceResult.upperLipBottomContour = upperLipBottom.getPoints();
                        }
                        FaceContour lowerLipTop = face.getContour(FaceContour.LOWER_LIP_TOP);
                        if (lowerLipTop != null) {
                            faceResult.lowerLipTopContour = lowerLipTop.getPoints();
                        }
                        FaceContour lowerLipBottom = face.getContour(FaceContour.LOWER_LIP_BOTTOM);
                        if (lowerLipBottom != null) {
                            faceResult.lowerLipBottomContour = lowerLipBottom.getPoints();
                        }
                        FaceContour noseBridge = face.getContour(FaceContour.NOSE_BRIDGE);
                        if (noseBridge != null) {
                            faceResult.noseBridgeContour = noseBridge.getPoints();
                        }
                        FaceContour noseBottom = face.getContour(FaceContour.NOSE_BOTTOM);
                        if (noseBottom != null) {
                            faceResult.noseBottomContour = noseBottom.getPoints();
                        }
                        FaceContour leftCheek = face.getContour(FaceContour.LEFT_CHEEK);
                        if (leftCheek != null) {
                            faceResult.leftCheekContour = leftCheek.getPoints();
                        }
                        FaceContour rightCheek = face.getContour(FaceContour.RIGHT_CHEEK);
                        if (rightCheek != null) {
                            faceResult.rightCheekContour = rightCheek.getPoints();
                        }
                        faceResults.add(faceResult);
                    }
                    listener.onFaceSuccess(faceResults, bitmap);
                    imageProxy.close();
                })
                .addOnFailureListener(e -> {
                    e.printStackTrace();
                    listener.onFaceFail(e.getMessage());
                    imageProxy.close();
                    Log.d(TAG, "predict: 识别失败，错误信息：" + e.getMessage());
                });
    }

    public void close() {
        if (faceDetector != null) {
            faceDetector.close();
            faceDetector = null;
        }
    }

    public interface ResultListener {
        void onFaceSuccess(List<FaceResult> faceResults,  Bitmap bitmap);

        void onFaceFail(String msg);
    }
}
