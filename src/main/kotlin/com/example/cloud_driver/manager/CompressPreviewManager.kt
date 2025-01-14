package com.example.cloud_driver.manager

import com.example.cloud_driver.config.Cons
import com.example.cloud_driver.util.FFmpegUtil
import com.example.cloud_driver.util.FileUtil
import com.example.cloud_driver.util.ImageCompressUtil
import com.google.common.net.MediaType
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.file.Files
import kotlin.io.path.Path

object CompressPreviewManager {
    suspend fun compressPreView(file: File, overwrite: Boolean = false) {
        val taskStartTime = System.currentTimeMillis()

        val mimeType = Files.probeContentType(Path(file.absolutePath))
            .let { MediaType.parse(it) }

        //小于100KB的图片不用压缩了
        if (mimeType.`is`(MediaType.ANY_IMAGE_TYPE) && file.length() <= 100 * 1000) {
            return
        }

        //相对路径
        val path = FileUtil
            .getRelativePath(file, File(Cons.Path.DATA_DIR))
            //去掉最后一个元素，只要父路径
            .let { if (it.isNotEmpty()) it.subList(0, it.size - 1) else it }
        val previewParentPath =
            FileUtil.getWholePath(Cons.Path.TEMP_PREVIEW_DIR, FileUtil.getWholePath(path))
        val preview = File(previewParentPath)
            .listFiles()
            ?.find { it.name.substringBeforeLast(".") == file.name }

        logger.info { "压缩图片-找到对应的预览图:${file.name}  ${preview}" }

        if (preview != null && !overwrite) return

        //生成压缩前预览图
        val tempImagePath = FileUtil.getWholePath(
            previewParentPath,
            "${file.name}_temp" + ".png"
        )
        val tempImageFile = File(tempImagePath)

        when {
            mimeType.`is`(MediaType.ANY_VIDEO_TYPE) -> {
                //使用IO调度器获取视频某一帧的图片
                withContext(Dispatchers.IO + CoroutineExceptionHandler { _, throwable ->
                    throwable.printStackTrace()
                    logger.info { "压缩图片-extraMiddleFrameImg异常:${file.name}  ${throwable}" }
                }) {
                    logger.info { "压缩图片-获取视频某一帧的图片-进入协程:${file.name}" }
                    FFmpegUtil.extraMiddleFrameImg(file.absolutePath, tempImagePath)
                }
            }

            mimeType.`is`(MediaType.ANY_IMAGE_TYPE) -> {
                file.copyTo(tempImageFile)
            }

            else -> {
                return
            }
        }


        val compressImagePath = FileUtil.getWholePath(
            previewParentPath,
            "${file.name}.jpg"
        )
        //使用Default调度器压缩图片
        withContext(Dispatchers.Default + CoroutineExceptionHandler { _, throwable ->
            throwable.printStackTrace()
            logger.info { "压缩图片-压缩图片异常:${file.name}  ${throwable}" }
        }) {
            logger.info { "压缩图片-压缩图片-进入协程:${file.name}" }
            ImageCompressUtil.previewCompress(tempImagePath, compressImagePath)
        }

        logger.info {
            val imageFile = File(tempImagePath)
            val compressedFile = File(compressImagePath)
            buildString {
                append("压缩图片-结束:${file.name}")
                appendLine()
                append("图片路径: ${imageFile.absolutePath}->${compressedFile.absolutePath}")
                appendLine()
                append("图片大小: ${imageFile.length()}->${compressedFile.length()}")
                appendLine()
                append("耗时: ${System.currentTimeMillis() - taskStartTime}")
            }
        }
        FileUtil.deleteFile(File(tempImagePath))
    }
}