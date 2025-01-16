package com.example.cloud_driver

import com.example.cloud_driver.config.Cons
import com.example.cloud_driver.manager.CompressPreviewManager
import com.example.cloud_driver.manager.logger
import com.example.cloud_driver.model.net.CodeMessage
import com.example.cloud_driver.model.net.Response
import com.example.cloud_driver.routing.configureRouting
import com.example.cloud_driver.util.TokenUtil
import com.google.common.net.MediaType
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.plugins.autohead.*
import io.ktor.server.plugins.calllogging.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.request.*
import io.ktor.server.resources.*
import io.ktor.server.response.*
import io.ktor.server.sse.*
import io.ktor.server.websocket.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.runBlocking
import java.io.File
import java.nio.file.Files
import kotlin.io.path.Path

fun main(args: Array<String>) {
    io.ktor.server.tomcat.jakarta.EngineMain.main(args)
}

fun Application.module() {
    runBlocking {
        init()
    }
    install(Resources)
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
        allowSameOrigin = true
        allowOrigins { true }
        allowHeaders { true }
    }
    install(WebSockets)

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
                CompressPreviewManager.compressPreView(file)
            }
        }
        .collect()

    logger.info {
        "压缩图片总耗时:${System.currentTimeMillis() - startTime}"
    }
}