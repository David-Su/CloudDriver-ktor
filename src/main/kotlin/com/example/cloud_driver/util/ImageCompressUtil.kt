package com.example.cloud_driver.util

import com.example.cloud_driver.manager.logger
import net.coobird.thumbnailator.Thumbnails
import java.io.File

object ImageCompressUtil {

    /**
     * 压缩为预览图片类型
     * @param targetFilePath 目标文件路径,jpg后缀(否则报错)
     */
    fun previewCompress(sourceFilePath: String, targetFilePath: String): Boolean {
        val sourceFile = File(sourceFilePath)
        if (!sourceFile.exists()) return false

        val targetFile = File(targetFilePath)
        if (targetFile.isFile && targetFile.exists()) {
            targetFile.delete()
        }

        assert(targetFile.extension != "jpg") {
            "TargetFilePath should use jpg extension"
        }

        return runCatching {
            Thumbnails.of(sourceFile)
                .outputQuality(0.8)
//                .scale(0.25)
                .scale(0.8)
                .outputFormat("jpg")
                .toFile(targetFile)
            true
        }.getOrElse { exception: Throwable ->
            logger.info { "压缩图片异常:${exception}" }
            exception.printStackTrace()
            false
        }

    }
}