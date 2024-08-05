package com.yeyupiaoling.cameraxapp.utils;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.Rect;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * @author yeyupiaoling
 * @date: 2024-08-05
 * @description: TODO
 */
public class Utils {

    // 旋转图像
    public static Bitmap rotateBitmap(Bitmap bitmap, int angle) {
        return rotateBitmap(bitmap, angle, false);
    }

    // 旋转图像
    public static Bitmap rotateBitmap(Bitmap bitmap, int angle, boolean isHorizontal) {
        Matrix matrix = new Matrix();
        matrix.postRotate(angle);
        if (isHorizontal) {
            matrix.postScale(-1, 1);
        }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
    }

    // 图片路径转换为Bitmap
    public static Bitmap getBitmapFromPath(String path) {
        return BitmapFactory.decodeFile(path);
    }

    // 在图片上画出框
    public static Bitmap drawRect(Bitmap bitmap, Rect box) {
        Paint paint = new Paint();
        paint.setColor(Color.RED);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(5);
        Bitmap mutableBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true);
        Canvas canvas = new Canvas(mutableBitmap);
        canvas.drawRect(box, paint);
        return mutableBitmap;
    }

    // 在图片上画点
    public static Bitmap drawPoint(Bitmap bitmap, List<PointF> points) {
        Paint paint = new Paint();
        paint.setColor(Color.GREEN);
        paint.setStyle(Paint.Style.FILL);
        paint.setStrokeWidth(1);
        Bitmap mutableBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true);
        Canvas canvas = new Canvas(mutableBitmap);
        for (PointF point : points) {
            canvas.drawCircle(point.x, point.y, 10, paint);
        }
        return mutableBitmap;
    }

    // 获取所有文件名（仅图像文件）
    public static List<String> getFilesAllName(String path) {
        List<String> imagePaths = new ArrayList<>();
        try {
            // 传入指定文件夹的路径
            File file = new File(path);
            File[] files = file.listFiles();
            if (files != null) {
                for (File f : files) {
                    if (checkIsImageFile(f.getPath())) {
                        imagePaths.add(f.getPath());
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return imagePaths;
    }

    // 判断是否是照片
    private static boolean checkIsImageFile(String fName) {
        // 获取拓展名
        String fileEnd = fName.substring(fName.lastIndexOf(".") + 1).toLowerCase();
        return "jpg".equals(fileEnd) || "png".equals(fileEnd) || "jpeg".equals(fileEnd);
    }

    // 查看文件是否存在，不存在就创建
    public static boolean isFolderExists(String strFolder) {
        File file = new File(strFolder);
        if (!file.exists()) {
            return file.mkdirs();
        }
        return false;
    }
}
