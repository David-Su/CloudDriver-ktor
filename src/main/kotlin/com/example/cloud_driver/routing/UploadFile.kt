package com.example.cloud_driver.routing

import com.example.cloud_driver.config.Cons
import com.example.cloud_driver.manager.UploadTaskManager
import com.example.cloud_driver.manager.logger
import com.example.cloud_driver.model.net.UploadTask
import com.example.cloud_driver.util.*
import io.ktor.http.content.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.utils.io.jvm.javaio.*
import java.io.File
import java.math.RoundingMode
import java.nio.file.Files

fun Route.uploadFile() {
    post("/uploadfile") {

        val username = TokenUtil.getUsername(call.queryParameters["token"])
        val path = CloudFileUtil.getWholePath(call.queryParameters["path"]!!, username)
        //客户端显示的路径
        val clientPath = CloudFileUtil.getWholePath(call.queryParameters["path"]!!, Cons.Path.USER_DIR_STUB)
        val realDir = FileUtil.getWholePath(Cons.Path.DATA_DIR, path)
        val tempDir = FileUtil.getWholePath(Cons.Path.TEMP_UPLOAD_DIR, path)

        val multipartData = call.receiveMultipart(formFieldLimit = Long.MAX_VALUE)

        multipartData.forEachPart { part ->
            if (part !is PartData.FileItem) return@forEachPart

            val provider = part.provider()
            val fileName = part.originalFileName as String
            val tempFile = File(FileUtil.getWholePath(tempDir, fileName))
//                .also { file ->
//                val parentFile = file.parentFile
//                if (!parentFile.exists()) parentFile.mkdirs()
//                if (file.isFile) file.delete()
//            }
            val input = provider.toInputStream()
            val output = tempFile.outputStream()
            val contentLength = call.request.contentLength()!!
            val bufferSize = DEFAULT_BUFFER_SIZE
            val isVideo = part.contentType?.contentType == "video"
            //更新进度的最小间隔
            val updateTaskSpan = 500L
            val realFile = File(FileUtil.getWholePath(realDir, fileName))
            val currentUploadTask = UploadTask(FileUtil.getWholePath(clientPath, fileName), 0.0, 0)
            var lastCalcSpeedTime: Long? = null //上次用来计算上传速度的时间

            UploadTaskManager.addTask(username, currentUploadTask)

            try {//将上传的数据写入文件缓存
                input.use {
                    output.use {
                        var lastRead = 0L //读取的文件总大小
                        var bytesCopied: Long = 0
                        val buffer = ByteArray(bufferSize)

                        while (true) {
                            val bytes = input.read(buffer)

                            if (bytes < 0) break

                            output.write(buffer, 0, bytes)

                            bytesCopied += bytes

                            val localLastCalcSpeedTime = lastCalcSpeedTime
                            val speed: Long
                            val progress: Double
                            if (bytesCopied >= contentLength) {
                                speed = 0
                                progress = 1.0
                            } else if (localLastCalcSpeedTime == null) {
                                speed = 0
                                progress = 0.0
                            } else {

                                speed = System.currentTimeMillis()
                                    .takeIf { it > localLastCalcSpeedTime && it - localLastCalcSpeedTime > updateTaskSpan }
                                    ?.let { (bytesCopied - lastRead) / (it - localLastCalcSpeedTime) * 1000 }
                                    ?: continue

                                logger.info {
                                    "bytesCopied->${bytesCopied} lastRead->${lastRead} speed->${speed}"
                                }

                                progress = bytesCopied
                                    .toBigDecimal()
                                    .divide(contentLength.toBigDecimal(), 2, RoundingMode.DOWN)
                                    .toDouble()
                            }

                            lastRead = bytesCopied
                            lastCalcSpeedTime = System.currentTimeMillis()

                            currentUploadTask.also {
                                it.speed = speed
                                it.progress = progress
                            }

                            UploadTaskManager.updateTask(username)

                        }
                    }
                }

                //将文件缓存移动到用户文件夹
                if (!realFile.exists()) {
                    if (!realFile.parentFile.exists()) {
                        realFile.parentFile.mkdirs()
                    }
                } else {
                    FileUtil.deleteFile(realFile)
                }

                Files.move(tempFile.toPath(), realFile.toPath())

                if (isVideo) { //生成预览图
                    val imagePath = FileUtil.getWholePath(
                        Cons.Path.TEMP_PREVIEW_DIR,
                        path,
                        "${fileName}.temp" + ".png"
                    )
                    FFmpegUtil.extraMiddleFrameImg(realFile.absolutePath, imagePath)
                    val compressImagePath = FileUtil.getWholePath(Cons.Path.TEMP_PREVIEW_DIR, path, "$fileName.jpg")
                    ImageCompressUtil.previewCompress(imagePath, compressImagePath)
                    logger.info {
                        "压缩图片: 原大小->${File(imagePath).length()}  压缩后大小->${
                            File(
                                compressImagePath
                            ).length()
                        }"
                    }
                    FileUtil.deleteFile(File(imagePath))
                }
            } catch (e: Exception) {
                e.printStackTrace()
                tempFile.delete()
            }

            UploadTaskManager.removeTask(username, currentUploadTask)

            part.dispose()
        }

        call.respondText("uploadFile")

    }
}