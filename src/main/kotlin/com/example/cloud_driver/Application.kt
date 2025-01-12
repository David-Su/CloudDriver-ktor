package com.example.cloud_driver

import com.example.cloud_driver.config.Cons
import com.example.cloud_driver.manager.logger
import com.example.cloud_driver.model.net.CodeMessage
import com.example.cloud_driver.model.net.Response
import com.example.cloud_driver.routing.configureRouting
import com.example.cloud_driver.util.FFmpegUtil
import com.example.cloud_driver.util.FileUtil
import com.example.cloud_driver.util.ImageCompressUtil
import com.example.cloud_driver.util.TokenUtil
import com.google.common.net.MediaType
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.plugins.autohead.*
import io.ktor.server.plugins.calllogging.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.sse.*
import io.ktor.server.websocket.*
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.file.Files
import kotlin.io.path.Path
import kotlin.time.Duration.Companion.seconds

fun main(args: Array<String>) {
    io.ktor.server.tomcat.jakarta.EngineMain.main(args)
}

fun Application.module() {
    runBlocking {
//        init()
    }
    install(ContentNegotiation) {
        json()
    }
    install(SSE)
    install(AutoHeadResponse)
    install(CallLogging)
    install(CORS) {
        anyHost()
        anyMethod()
        allowCredentials = true
        allowNonSimpleContentTypes = true
        allowOrigins { true }
        this.allowHeaders { true }
        this.allowSameOrigin = true
    }
    install(WebSockets) {
        pingPeriod = 15.seconds
        timeout = 15.seconds
        maxFrameSize = Long.MAX_VALUE
        masking = false
    }

    intercept(ApplicationCallPipeline.Call) {
        if (call.request.path().endsWith("/uploadtasks")) {
            return@intercept proceed()
        }
        if (call.request.path().endsWith("/login")) {
            return@intercept proceed()
        }
        val token = call.request.queryParameters["token"]

        if (token.isNullOrEmpty()) {
            call.respond(Response<Unit>(CodeMessage.TOKEN_ILLEGAL.code, CodeMessage.TOKEN_ILLEGAL.message))
            return@intercept finish()
        }
        if (TokenUtil.timeout(token)) {
            call.respond(Response<Unit>(CodeMessage.TOKEN_TIMEOUT.code, CodeMessage.TOKEN_TIMEOUT.message))
            return@intercept finish()
        }
        if (!TokenUtil.valid(token)) {
            call.respond(Response<Unit>(CodeMessage.TOKEN_ILLEGAL.code, CodeMessage.TOKEN_ILLEGAL.message))
            return@intercept finish()
        }
        proceed()
    }

    configureRouting()
}

private suspend fun init() {
    val startTime = System.currentTimeMillis()

    val userDir = File(Cons.Path.DATA_DIR)
    if (!userDir.exists()) {
        userDir.mkdirs()
        return
    }

    userDir.walkTopDown()
        .asFlow()
        .filter { it.isFile }
        .filter {
            Files.probeContentType(Path(it.absolutePath))
                ?.takeIf { it.isNotEmpty() }
                ?.let { MediaType.parse(it) }
                ?.let { it.`is`(MediaType.ANY_VIDEO_TYPE) or it.`is`(MediaType.ANY_IMAGE_TYPE) }
                ?: false
        }
        .flatMapMerge { file ->
            flow<Unit> {
                val taskStartTime = System.currentTimeMillis()

                val mimeType = Files.probeContentType(Path(file.absolutePath))
                    .let { MediaType.parse(it) }

                //相对路径
                val path = FileUtil
                    .getRelativePath(file, userDir)
                    //去掉最后一个元素，只要父路径
                    .let { if (it.isNotEmpty()) it.subList(0, it.size - 1) else it }
                val previewParentPath =
                    FileUtil.getWholePath(Cons.Path.TEMP_PREVIEW_DIR, FileUtil.getWholePath(path))
                val preview = File(previewParentPath)
                    .listFiles()
                    ?.find { it.name.substringBeforeLast(".") == file.name }

                logger.info { "压缩图片-找到对应的预览图:${file.name}  ${preview}" }

                if (preview != null) return@flow

                //生成压缩前预览图
                val tempImagePath = FileUtil.getWholePath(
                    previewParentPath,
                    "${file.name}_temp" + ".png"
                )

                val tempImageFile = File(tempImagePath)

                if (tempImageFile.exists() && tempImageFile.isFile) {
                    tempImageFile.delete()
                } else if (!tempImageFile.parentFile.exists()) {
                    tempImageFile.parentFile.mkdirs()
                }

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
                        return@flow
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
                    logger.info { "压缩图片-压缩图片-进入协程:${file.name}  " }
                    ImageCompressUtil.previewCompress(tempImagePath, compressImagePath)
                }

                logger.info {
                    val imageFile = File(tempImagePath)
                    val compressedFile = File(compressImagePath)
                    buildString {
                        append("压缩图片-结束:${file.name}  ")
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
        .collect()

    logger.info {
        "压缩图片总耗时:${System.currentTimeMillis() - startTime}"
    }
}