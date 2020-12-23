package com.yeyupiaoling.cameraxapp

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Point
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LiveData
import com.blankj.utilcode.util.PathUtils
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.google.common.util.concurrent.ListenableFuture
import com.yeyupiaoling.cameraxapp.utils.Utils
import com.yeyupiaoling.cameraxapp.utils.YuvToRgbConverter
import com.yeyupiaoling.cameraxapp.view.CameraXPreviewViewTouchListener
import com.yeyupiaoling.cameraxapp.view.FocusImageView
import kotlinx.android.synthetic.main.activity_main.*
import java.io.File
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit


class MainActivity : AppCompatActivity() {
    private var imageCapture: ImageCapture? = null
    private var mCameraControl: CameraControl? = null
    private var mCameraInfo: CameraInfo? = null
    private var focusView: FocusImageView? = null
    private var isInfer = false
    private var imageRotationDegrees: Int = 0

    // 使用后摄像头
    private var cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

    private lateinit var cameraExecutor: ExecutorService
    private lateinit var bitmapBuffer: Bitmap
    private lateinit var converter: YuvToRgbConverter

    companion object {
        private const val TAG = "MainActivity"
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS =
            arrayOf(Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 隐藏状态栏
        val flag = WindowManager.LayoutParams.FLAG_FULLSCREEN
        // 获得当前窗体对象
        val window: Window = this@MainActivity.window
        // 设置当前窗体为全屏显示
        window.setFlags(flag, flag)
        setContentView(R.layout.activity_main)

        // 对焦框控件
        focusView = findViewById(R.id.focus_view)

        // 请求权限
        if (allPermissionsGranted()) {
            startCamera()
        } else {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS)
        }

        // 点击拍照
        camera_capture_button.setOnClickListener { takePhoto() }

        cameraExecutor = Executors.newSingleThreadExecutor()

        // 把图像数据转换为RGB格式图像
        converter = YuvToRgbConverter(this)

        // 切换摄像头
        camera_switch_button.setOnClickListener {
            cameraSelector = if (CameraSelector.DEFAULT_FRONT_CAMERA == cameraSelector) {
                CameraSelector.DEFAULT_BACK_CAMERA
            } else {
                CameraSelector.DEFAULT_FRONT_CAMERA
            }
            // 重启相机
            startCamera()
        }

        photo_view_button.setOnClickListener {
            val intent = Intent(this@MainActivity, AlbumActivity::class.java)
            startActivity(intent)
        }
    }

    override fun onResume() {
        super.onResume()

        // 显示最新的图像
        val images = Utils.getFilesAllName(PathUtils.getExternalAppPicturesPath())
        if (images.isNotEmpty()) {
            Glide.with(this@MainActivity)
                .load(images[images.size - 1])
                .apply(RequestOptions.circleCropTransform())
                .into(photo_view_button)
        } else {
            Glide.with(this@MainActivity)
                .load(R.drawable.ic_photo)
                .apply(RequestOptions.circleCropTransform())
                .into(photo_view_button)
        }
    }

    // 启动相机
    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this@MainActivity)

        cameraProviderFuture.addListener({
            // 绑定生命周期
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            // 设置相机支持预览
            val preview = Preview.Builder().build()
            preview.setSurfaceProvider(viewFinder.surfaceProvider);

            // 设置相机支持拍照
            imageCapture = ImageCapture.Builder()
                // 设置闪光灯
                .setFlashMode(ImageCapture.FLASH_MODE_OFF)
                // 设置照片质量
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
                .build()

            // 设置相机支持图像分析
            val imageAnalysis = ImageAnalysis.Builder()
                .setTargetAspectRatio(AspectRatio.RATIO_4_3)
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()

            // 实时获取图像进行分析
            imageAnalysis.setAnalyzer(ContextCompat.getMainExecutor(this), { image ->
                if (isInfer){
                    // 执行人脸检测
                    infer(image)
                }
            })

            try {
                // 在重新绑定之前取消绑定用例
                cameraProvider.unbindAll()

                // 将用例绑定到摄像机
                val camera: Camera =
                    cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture, imageAnalysis)
                // 相机控制，如点击
                mCameraControl = camera.cameraControl
                mCameraInfo = camera.cameraInfo
                initCameraListener()
            } catch (exc: Exception) {
                Log.e(TAG, "Use case binding failed", exc)
            }

        }, ContextCompat.getMainExecutor(this))
    }

    // 人脸检测
    @SuppressLint("UnsafeExperimentalUsageError")
    private fun infer(image: ImageProxy) {
        if (!::bitmapBuffer.isInitialized) {
            imageRotationDegrees = image.imageInfo.rotationDegrees
            Log.d("测试", "方向：$imageRotationDegrees")
            bitmapBuffer = Bitmap.createBitmap(image.width, image.height, Bitmap.Config.ARGB_8888)
        }

        // 将图像转换为RGB，并将其放在bitmapBuffer
        image.use { converter.yuvToRgb(image.image!!, bitmapBuffer) }

        // 画框
        (box_prediction.layoutParams as ViewGroup.MarginLayoutParams).apply {
            topMargin = 20
            leftMargin = 30
            width = 400
            height = 500
        }
    }

    // 拍照
    private fun takePhoto() {
        // 保证相机可用
        val imageCapture = imageCapture ?: return

        // 保存路径
        val photoFile =
            File(PathUtils.getExternalAppPicturesPath(), "" + System.currentTimeMillis() + ".jpg")

        // 创建包含文件和metadata的输出选项对象
        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        // 设置图像捕获监听器，在拍照后触发
        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageSavedCallback {
                override fun onError(exc: ImageCaptureException) {
                    Log.e(TAG, "Photo capture failed: ${exc.message}", exc)
                }

                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    val savedUri = Uri.fromFile(photoFile)
                    Log.d(TAG, "拍照成功，保存路径: $savedUri")
                    Glide.with(this@MainActivity)
                        .load(savedUri)
                        .apply(RequestOptions.circleCropTransform())
                        .into(photo_view_button)
                }
            })
    }

    // 相机点击等相关操作监听
    private fun initCameraListener() {
        val zoomState: LiveData<ZoomState> = mCameraInfo!!.zoomState
        val cameraXPreviewViewTouchListener = CameraXPreviewViewTouchListener(this)

        cameraXPreviewViewTouchListener.setCustomTouchListener(object :
            CameraXPreviewViewTouchListener.CustomTouchListener {
            // 放大缩小操作
            override fun zoom(delta: Float) {
                Log.d(TAG, "缩放")
                zoomState.value?.let {
                    val currentZoomRatio = it.zoomRatio
                    mCameraControl!!.setZoomRatio(currentZoomRatio * delta)
                }
            }

            // 点击操作
            override fun click(x: Float, y: Float) {
                Log.d(TAG, "单击")
                val factory = viewFinder.meteringPointFactory
                // 设置对焦位置
                val point = factory.createPoint(x, y)
                val action = FocusMeteringAction.Builder(point, FocusMeteringAction.FLAG_AF)
                    // 3秒内自动调用取消对焦
                    .setAutoCancelDuration(3, TimeUnit.SECONDS)
                    .build()
                // 执行对焦
                focusView!!.startFocus(Point(x.toInt(), y.toInt()))
                val future: ListenableFuture<*> = mCameraControl!!.startFocusAndMetering(action)
                future.addListener({
                    try {
                        // 获取对焦结果
                        val result = future.get() as FocusMeteringResult
                        if (result.isFocusSuccessful) {
                            focusView!!.onFocusSuccess()
                        } else {
                            focusView!!.onFocusFailed()
                        }
                    } catch (e: java.lang.Exception) {
                        Log.e(TAG, e.toString())
                    }
                }, ContextCompat.getMainExecutor(this@MainActivity))
            }

            // 双击操作
            override fun doubleClick(x: Float, y: Float) {
                Log.d(TAG, "双击")
                // 双击放大缩小
                val currentZoomRatio = zoomState.value!!.zoomRatio
                if (currentZoomRatio > zoomState.value!!.minZoomRatio) {
                    mCameraControl!!.setLinearZoom(0f)
                } else {
                    mCameraControl!!.setLinearZoom(0.5f)
                }
            }

            override fun longPress(x: Float, y: Float) {
                Log.d(TAG, "长按")
            }
        })
        // 添加监听事件
        viewFinder.setOnTouchListener(cameraXPreviewViewTouchListener)
    }

    override fun onDestroy() {
        super.onDestroy()
        // 关闭相机
        cameraExecutor.shutdown()
    }

    // 权限申请
    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_GRANTED
    }

    // 权限申请结果
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                startCamera()
            } else {
                Toast.makeText(this, "没有授权，无法使用！", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }
}