package com.example.cloud_driver

import com.example.cloud_driver.model.net.CodeMessage
import com.example.cloud_driver.model.net.Response
import com.example.cloud_driver.routing.configureRouting
import com.example.cloud_driver.util.TokenUtil
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.plugins.autohead.*
import io.ktor.server.plugins.calllogging.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.plugins.partialcontent.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.sse.*
import io.ktor.server.websocket.*
import org.slf4j.event.Level

fun main(args: Array<String>) {
    io.ktor.server.tomcat.jakarta.EngineMain.main(args)
}

fun Application.module() {
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
