package com.yeyupiaoling.cameraxapp.utils;

import android.content.Context;
import android.graphics.Point;
import android.util.Log;
import android.util.Size;

import androidx.annotation.NonNull;
import androidx.camera.core.AspectRatio;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraControl;
import androidx.camera.core.CameraInfo;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.FocusMeteringAction;
import androidx.camera.core.FocusMeteringResult;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.MeteringPoint;
import androidx.camera.core.MeteringPointFactory;
import androidx.camera.core.Preview;
import androidx.camera.core.ZoomState;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.LiveData;

import com.blankj.utilcode.util.PathUtils;
import com.yeyupiaoling.cameraxapp.view.CameraXPreviewViewTouchListener;
import com.yeyupiaoling.cameraxapp.view.FocusImageView;
import com.google.common.util.concurrent.ListenableFuture;

import java.io.File;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * @author yeyupiaoling
 * @date: 2024-08-02
 * @description: CameraX工具类
 */
public class CameraXHelper {
    private static final String TAG = CameraXHelper.class.getSimpleName();
    // 默认使用后摄像头
    public CameraSelector cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;
    public int flashMode = ImageCapture.FLASH_MODE_OFF;
    private ImageCapture imageCapture;
    private CameraControl mCameraControl;
    private CameraInfo mCameraInfo;
    private final FocusImageView focusView;
    private final CameraXHelperListener listener;
    private final Context context;
    private final PreviewView previewView;
    private boolean useAnalysis;
    private final ExecutorService cameraExecutor;

    public CameraXHelper(Context context, PreviewView previewView, FocusImageView focusView, CameraXHelperListener listener) {
        this.context = context;
        this.previewView = previewView;
        this.focusView = focusView;
        this.listener = listener;
        cameraExecutor = Executors.newSingleThreadExecutor();
    }

    public void setCameraSelector(CameraSelector cameraSelector) {
        this.cameraSelector = cameraSelector;
    }

    public void enableAnalysis(boolean useAnalysis) {
        this.useAnalysis = useAnalysis;
    }

    public void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(context);
        cameraProviderFuture.addListener(() -> {
            try {
                // 绑定生命周期
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                int screenAspectRatio = previewView.getDisplay().getRotation();
                // 设置相机支持预览
                Preview preview = new Preview.Builder()
                        .setTargetRotation(screenAspectRatio)
                        .setTargetAspectRatio(AspectRatio.RATIO_16_9)
                        .build();
                preview.setSurfaceProvider(previewView.getSurfaceProvider());
                // 设置相机支持拍照
                imageCapture = new ImageCapture.Builder()
                        .setFlashMode(flashMode) // 设置闪光灯模式
                        .setTargetRotation(screenAspectRatio)
                        .setTargetAspectRatio(AspectRatio.RATIO_16_9)
                        .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY) // 设置照片质量
                        .build();
                // 设置相机支持图像分析
                ImageAnalysis imageAnalysis = null;
                if (useAnalysis) {
                    imageAnalysis = new ImageAnalysis.Builder()
                            .setTargetResolution(new Size(1080, 1920))
                            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                            .build();
                    // 实时获取图像进行分析
                    imageAnalysis.setAnalyzer(cameraExecutor, image -> {
                        if (listener != null) {
                            listener.onImageAnalysis(image);
                        }
                    });
                }
                // 在重新绑定之前取消绑定用例
                cameraProvider.unbindAll();
                Camera camera;
                if (useAnalysis) {
                    // 将用例绑定到摄像机
                    camera = cameraProvider.bindToLifecycle(
                            (LifecycleOwner) context,
                            cameraSelector,
                            preview,
                            imageCapture,
                            imageAnalysis
                    );
                } else {
                    // 将用例绑定到摄像机
                    camera = cameraProvider.bindToLifecycle(
                            (LifecycleOwner) context,
                            cameraSelector,
                            preview,
                            imageCapture
                    );
                }

                // 相机控制，如点击
                mCameraControl = camera.getCameraControl();
                mCameraInfo = camera.getCameraInfo();
                if (focusView != null) {
                    initCameraListener();
                }
                if (listener != null) {
                    listener.onCameraStart();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, ContextCompat.getMainExecutor(context));
    }

    private void initCameraListener() {
        LiveData<ZoomState> zoomState = mCameraInfo.getZoomState();
        CameraXPreviewViewTouchListener cameraXPreviewViewTouchListener = new CameraXPreviewViewTouchListener(context);

        cameraXPreviewViewTouchListener.setCustomTouchListener(new CameraXPreviewViewTouchListener.CustomTouchListener() {
            @Override
            public void zoom(float delta) {
                Log.d(TAG, "缩放");
                if (zoomState.getValue() != null) {
                    float currentZoomRatio = zoomState.getValue().getZoomRatio();
                    mCameraControl.setZoomRatio(currentZoomRatio * delta);
                }
            }

            @Override
            public void click(float x, float y) {
                Log.d(TAG, "单击");
                MeteringPointFactory factory = previewView.getMeteringPointFactory();
                MeteringPoint point = factory.createPoint(x, y);
                FocusMeteringAction action = new FocusMeteringAction.Builder(point, FocusMeteringAction.FLAG_AF)
                        .setAutoCancelDuration(3, TimeUnit.SECONDS)
                        .build();

                focusView.startFocus(new Point((int) x, (int) y));
                ListenableFuture<?> future = mCameraControl.startFocusAndMetering(action);
                future.addListener(() -> {
                    try {
                        FocusMeteringResult result = (FocusMeteringResult) future.get();
                        if (result.isFocusSuccessful()) {
                            focusView.onFocusSuccess();
                        } else {
                            focusView.onFocusFailed();
                        }
                    } catch (Exception e) {
                        Log.e(TAG, e.toString());
                        focusView.onFocusFailed();
                    }
                }, ContextCompat.getMainExecutor(context));
            }

            @Override
            public void doubleClick(float x, float y) {
                Log.d(TAG, "双击");
                if (zoomState.getValue() != null) {
                    float currentZoomRatio = zoomState.getValue().getZoomRatio();
                    if (currentZoomRatio > zoomState.getValue().getMinZoomRatio()) {
                        mCameraControl.setLinearZoom(0f);
                    } else {
                        mCameraControl.setLinearZoom(0.5f);
                    }
                }
            }

            @Override
            public void longPress(float x, float y) {
                Log.d(TAG, "长按");
            }
        });

        // 添加监听事件
        previewView.setOnTouchListener(cameraXPreviewViewTouchListener);
    }

    public void takePicture() {
        if (imageCapture == null) {
            return;
        }
        // 创建文件
        String savePath = PathUtils.getExternalAppPicturesPath() + File.separator + System.currentTimeMillis() + ".jpg";
        File photoFile = new File(savePath);
        // 创建包含文件和metadata的输出选项对象
        ImageCapture.OutputFileOptions outputOptions = new ImageCapture.OutputFileOptions.Builder(photoFile).build();
        // 设置图像捕获监听器，在拍照后触发
        imageCapture.takePicture(outputOptions, ContextCompat.getMainExecutor(context), new ImageCapture.OnImageSavedCallback() {
            @Override
            public void onImageSaved(@NonNull ImageCapture.OutputFileResults outputFileResults) {
                Log.d(TAG, "拍照成功，保存路径: " + savePath);
                if (listener != null) {
                    listener.takePicture(savePath);
                }
            }

            @Override
            public void onError(@NonNull ImageCaptureException exception) {
                Log.e(TAG, "Photo capture failed: " + exception.getMessage());
            }
        });
    }

    public void stopCamera() {
        // 关闭相机
        cameraExecutor.shutdown();
        if (listener != null) {
            listener.onCameraClose();
        }
    }

    public interface CameraXHelperListener {
        void onCameraStart();

        void onImageAnalysis(ImageProxy imageProxy);

        void takePicture(String path);

        void onCameraClose();
    }
}
