package com.yeyupiaoling.cameraxapp

import android.os.Bundle
import android.view.GestureDetector
import android.view.MotionEvent
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.blankj.utilcode.util.PathUtils
import com.bumptech.glide.Glide
import kotlinx.android.synthetic.main.activity_album.*
import kotlinx.android.synthetic.main.activity_main.*
import java.io.File


class AlbumActivity : AppCompatActivity() {

    // 图片数组
    private var images: List<String> = ArrayList()

    //图片下标序号
    private var count = 0

    //定义手势监听对象
    private var gestureDetector: GestureDetector? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_album)

        gestureDetector = GestureDetector(onGestureListener)

        // 显示最新的图像
        images = Utils.getFilesAllName(PathUtils.getExternalAppPicturesPath())
        if (images.isNotEmpty()) {
            Glide.with(this@AlbumActivity).load(images[count]).into(imageView)
        }

        // 删除图片
        delete_btn.setOnClickListener {
            val file = File(images[count])
            file.delete()
            images = images - images[count]
            if (images.isEmpty()) {
                Toast.makeText(this@AlbumActivity, "没有照片了！", Toast.LENGTH_SHORT).show()
                finish()
            } else {
                count--
                Glide.with(this@AlbumActivity).load(images[count]).into(imageView)
            }
        }
    }

    //当Activity被触摸时回调
    override fun onTouchEvent(event: MotionEvent?): Boolean {
        return gestureDetector!!.onTouchEvent(event)
    }

    //自定义GestureDetector的手势识别监听器
    private val onGestureListener: GestureDetector.OnGestureListener =
        object : GestureDetector.SimpleOnGestureListener() {
            //当识别的手势是滑动手势时回调onFinger方法
            override fun onFling(
                e1: MotionEvent,
                e2: MotionEvent,
                velocityX: Float,
                velocityY: Float
            ): Boolean {
                //通过计算判断是向左还是向右滑动
                val x = e1.x - e2.x
                if (x > 0) {
                    // 最大值
                    if (count == images.size - 1) {
                        return true
                    }
                    count++
                } else if (x < 0) {
                    // 最小值
                    if (count == 0) {
                        return true
                    }
                    count--
                }
                //切换imageView的图片
                Glide.with(this@AlbumActivity).load(images[count]).into(imageView)
                return true
            }
        }
}