package com.example.cloud_driver.routing

import com.example.cloud_driver.routing.websocket.websocketUploadTasks
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Application.configureRouting() {
    routing {
        get("/test") {
            call.respondText("Hello World!")
        }
        login()
        createDir()
        DeleteFile()
        listFile()
        openDir()
        renameFile()
        downloadFile()
        uploadFile()
        websocketUploadTasks()
    }
}
