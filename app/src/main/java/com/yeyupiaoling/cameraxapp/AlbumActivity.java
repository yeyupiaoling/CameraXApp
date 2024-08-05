package com.yeyupiaoling.cameraxapp;

import android.os.Bundle;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.blankj.utilcode.util.PathUtils;
import com.bumptech.glide.Glide;
import com.yeyupiaoling.cameraxapp.utils.Utils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class AlbumActivity extends AppCompatActivity {
    // 图片数组
    private List<String> images = new ArrayList<>();
    //定义手势监听对象
    private GestureDetector gestureDetector;
    //图片下标序号
    private int count = 0;
    private ImageView imageView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_album);

        gestureDetector = new GestureDetector(onGestureListener);
        imageView = findViewById(R.id.imageView);
        ImageButton deleteBtn = findViewById(R.id.delete_btn);
        deleteBtn.setOnClickListener(v -> {
            File file = new File(images.get(count));
            file.delete();
            images.remove(images.get(count));
            if (images.isEmpty()) {
                Toast.makeText(AlbumActivity.this, "没有照片了！", Toast.LENGTH_SHORT).show();
                finish();
            } else {
                count--;
                if (count < 0) count = 0;
                Glide.with(AlbumActivity.this).load(images.get(count)).into(imageView);
            }
        });

        // 显示最新的图像
        images = Utils.getFilesAllName(PathUtils.getExternalAppPicturesPath());
        if (!images.isEmpty()) {
            Glide.with(this).load(images.get(count)).into(imageView);
        }else{
            Toast.makeText(this, "没有照片了！", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        // 显示最新的图像
        images = Utils.getFilesAllName(PathUtils.getExternalAppPicturesPath());
        if (!images.isEmpty()) {
            Glide.with(this).load(images.get(count)).into(imageView);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return gestureDetector.onTouchEvent(event);
    }
    private final GestureDetector.OnGestureListener onGestureListener = new GestureDetector.SimpleOnGestureListener() {
        @Override
        public boolean onFling(MotionEvent e1, @NonNull MotionEvent e2, float velocityX, float velocityY) {
            // 通过计算判断是向左还是向右滑动
            float x = 0;
            if (e1 != null) {
                x = e1.getX() - e2.getX();
            }
            if (x > 0) {
                // 向右滑动
                if (count == images.size() - 1) {
                    // 已经是最后一张图片，不执行任何操作
                    return true;
                }
                count++;
            } else if (x < 0) {
                // 向左滑动
                if (count == 0) {
                    // 已经是第一张图片，不执行任何操作
                    return true;
                }
                count--;
            }
            // 切换imageView的图片
            Glide.with(AlbumActivity.this).load(images.get(count)).into(imageView);
            return true;
        }
    };
}