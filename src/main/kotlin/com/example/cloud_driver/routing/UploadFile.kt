package com.example.cloud_driver.routing

import com.example.cloud_driver.config.Cons
import com.example.cloud_driver.manager.CompressPreviewManager
import com.example.cloud_driver.manager.UploadTaskManager
import com.example.cloud_driver.manager.logger
import com.example.cloud_driver.model.net.CodeMessage
import com.example.cloud_driver.model.net.Response
import com.example.cloud_driver.model.net.UploadTask
import com.example.cloud_driver.util.*
import com.google.common.net.MediaType
import io.ktor.http.content.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.utils.io.*
import io.ktor.utils.io.core.*
import io.ktor.utils.io.jvm.javaio.*
import kotlinx.io.readByteArray
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
        val part = multipartData.readPart() as PartData.FileItem
        val provider = part.provider()
        val fileName = part.originalFileName as String
        val tempFile = File(FileUtil.getWholePath(tempDir, fileName)).also {
            it.parentFile?.also { if (!it.exists()) it.mkdirs() }
        }
        val output = tempFile.outputStream()
        val contentLength = call.request.contentLength()!!
        val bufferSize = DEFAULT_BUFFER_SIZE
        //更新进度的最小间隔
        val updateTaskSpan = 500L
        val realFile = File(FileUtil.getWholePath(realDir, fileName))
        val currentUploadTask = UploadTask(FileUtil.getWholePath(clientPath, fileName), 0.0, 0)
        //上次用来计算上传速度的时间
        var lastCalcSpeedTime: Long? = null
        //读取的文件总大小
        var lastRead = 0L
        var bytesCopied: Long = 0

        UploadTaskManager.addTask(username, currentUploadTask)

        //将上传的数据写入文件缓存
        runCatching {

            while (!provider.isClosedForRead) {

                val source = provider.readRemaining(bufferSize.toLong())
                if (source.exhausted()) break
                val bytes = source.readByteArray()
                output.write(bytes)
                bytesCopied += bytes.size

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

            //将文件缓存移动到用户文件夹
            if (!realFile.exists()) {
                if (!realFile.parentFile.exists()) {
                    realFile.parentFile.mkdirs()
                }
            } else {
                FileUtil.deleteFile(realFile)
            }

            Files.move(tempFile.toPath(), realFile.toPath())

            CompressPreviewManager.compressPreView(realFile,overwrite = true)
        }.onFailure {
            it.printStackTrace()
        }
        runCatching { provider.cancel() }
        runCatching { output.close() }
        runCatching { tempFile.delete() }

        UploadTaskManager.removeTask(username, currentUploadTask)

        part.dispose()

        call.respond(Response<Unit>(CodeMessage.OK.code, CodeMessage.OK.message))
    }
}