package com.yeyupiaoling.cameraxapp.utils

import java.io.File
import java.lang.Exception
import java.util.*
import kotlin.collections.ArrayList

class Utils {
    companion object {
        fun getFilesAllName(path: String): List<String> {
            val imagePaths: MutableList<String> = ArrayList()
            return try {
                //传入指定文件夹的路径
                val file = File(path)
                val files: Array<File> = file.listFiles()
                for (i in files.indices) {
                    if (checkIsImageFile(files[i].path)) {
                        imagePaths.add(files[i].path)
                    }
                }
                imagePaths
            } catch (e: Exception) {
                imagePaths
            }
        }

        // 判断是否是照片
        private fun checkIsImageFile(fName: String): Boolean {
            //获取拓展名
            val fileEnd = fName.substring(
                fName.lastIndexOf(".") + 1, fName.length
            ).toLowerCase(Locale.ROOT)
            return fileEnd == "jpg" || fileEnd == "png" || fileEnd == "jpeg"
        }

        // 查看文件是否存在，不存在就创建
        fun isFolderExists(strFolder: String): Boolean {
            val file = File(strFolder)
            if (!file.exists()) {
                return file.mkdirs()
            }
            return false
        }
    }

}