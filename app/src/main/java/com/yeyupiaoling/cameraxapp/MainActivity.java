package com.yeyupiaoling.cameraxapp;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ExperimentalGetImage;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageProxy;
import androidx.camera.view.PreviewView;

import com.blankj.utilcode.util.PathUtils;
import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.yeyupiaoling.cameraxapp.bean.FaceResult;
import com.yeyupiaoling.cameraxapp.utils.CameraFaceDetector;
import com.yeyupiaoling.cameraxapp.utils.CameraXHelper;
import com.yeyupiaoling.cameraxapp.utils.Utils;
import com.yeyupiaoling.cameraxapp.view.CanvasView;
import com.yeyupiaoling.cameraxapp.view.FocusImageView;

import java.util.List;

public class MainActivity extends AppCompatActivity implements CameraXHelper.CameraXHelperListener {
    private static final String TAG = MainActivity.class.getSimpleName();
    private boolean isSet = false;
    private CameraXHelper mCameraXHelper;
    private PreviewView previewView;
    private FocusImageView focusView;
    private CameraFaceDetector detector;
    private ImageView photoViewButton;
    private CanvasView mCanvasView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initView();
        mCameraXHelper = new CameraXHelper(this, previewView, focusView, this);
        mCameraXHelper.enableAnalysis(true);
        // 申请权限
        if (!hasPermission()) {
            requestPermission();
        }else {
            mCameraXHelper.startCamera();
        }

        detector = new CameraFaceDetector(new CameraFaceDetector.ResultListener() {
            @Override
            public void onFaceSuccess(List<FaceResult> faceResults, Bitmap bitmap) {
                if (!isSet) {
                    // 设置画布的宽高
                    mCanvasView.setTextureViewDimen(mCanvasView.getWidth(), mCanvasView.getHeight(),
                            bitmap.getWidth(), bitmap.getHeight());
                    isSet = true;
                }
                runOnUiThread(() -> mCanvasView.populateResult(faceResults));
            }

            @Override
            public void onFaceFail(String msg) {

            }
        });
    }

    private void initView() {
        previewView = findViewById(R.id.previewView);
        focusView = findViewById(R.id.focus_view);
        mCanvasView = findViewById(R.id.canvas_view);
        photoViewButton = findViewById(R.id.photo_view_button);
        // 打开相册
        photoViewButton.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, AlbumActivity.class);
            startActivity(intent);
        });
        // 拍照
        ImageView cameraCaptureButton = findViewById(R.id.camera_capture_button);
        cameraCaptureButton.setOnClickListener(v -> mCameraXHelper.takePicture());
        // 切换摄像头
        ImageView cameraSwitchButton = findViewById(R.id.camera_switch_button);
        cameraSwitchButton.setOnClickListener(v -> {
            CameraSelector cameraSelector;
            if (CameraSelector.DEFAULT_FRONT_CAMERA == mCameraXHelper.cameraSelector) {
                cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;
                mCanvasView.setCamera(true);
            } else {
                cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA;
                mCanvasView.setCamera(false);
            }
            mCameraXHelper.setCameraSelector(cameraSelector);
            // 重启相机
            mCameraXHelper.startCamera();
        });
        // 闪光灯
        ImageView flashSwitchButton = findViewById(R.id.flash_switch_button);
        flashSwitchButton.setOnClickListener(v -> {
            switch (mCameraXHelper.flashMode) {
                case ImageCapture.FLASH_MODE_OFF:
                    mCameraXHelper.flashMode = ImageCapture.FLASH_MODE_ON;
                    flashSwitchButton.setImageResource(R.drawable.open_flash);
                    break;

                case ImageCapture.FLASH_MODE_ON:
                    mCameraXHelper.flashMode = ImageCapture.FLASH_MODE_AUTO;
                    flashSwitchButton.setImageResource(R.drawable.auto_flash);
                    break;

                case ImageCapture.FLASH_MODE_AUTO:
                    mCameraXHelper.flashMode = ImageCapture.FLASH_MODE_OFF;
                    flashSwitchButton.setImageResource(R.drawable.stop_flash);
                    break;
            }
        });
    }

    @Override
    public void onCameraStart() {
        Log.d(TAG, "onCameraStart: ");
    }

    @ExperimentalGetImage
    @Override
    public void onImageAnalysis(ImageProxy imageProxy) {
        detector.predict(imageProxy);
    }

    @Override
    public void takePicture(String path) {
        Glide.with(this)
                .load(path)
                .apply(RequestOptions.circleCropTransform())
                .into(photoViewButton);
    }

    @Override
    public void onCameraClose() {
        Log.d(TAG, "onCameraClose: ");
    }

    @Override
    protected void onResume() {
        super.onResume();

        // 显示最新的图像
        List<String> images = Utils.getFilesAllName(PathUtils.getExternalAppPicturesPath());
        if (!images.isEmpty()) {
            Glide.with(this)
                    .load(images.get(images.size() - 1))
                    .apply(RequestOptions.circleCropTransform())
                    .into(photoViewButton);
        } else {
            Glide.with(this)
                    .load(R.drawable.ic_photo)
                    .apply(RequestOptions.circleCropTransform())
                    .into(photoViewButton);
        }
    }

    @Override
    protected void onDestroy() {
        mCameraXHelper.stopCamera();
        super.onDestroy();
    }

    // 检查权限
    private boolean hasPermission() {
        return checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED &&
                checkSelfPermission(android.Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;
    }

    // 请求权限
    private void requestPermission() {
        requestPermissions(new String[]{android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.CAMERA}, 1);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (hasPermission()) {
            mCameraXHelper.startCamera();
        } else {
            Toast.makeText(this, "没有授权，无法使用！", Toast.LENGTH_SHORT).show();
            finish();
        }
    }
}