package com.example.cloud_driver.routing

import com.example.cloud_driver.config.Cons
import com.example.cloud_driver.manager.logger
import com.example.cloud_driver.model.net.CodeMessage
import com.example.cloud_driver.model.net.Response
import com.example.cloud_driver.util.CloudFileUtil
import com.example.cloud_driver.util.FileUtil
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import java.io.File

@Serializable
data class CreateDir(val paths: List<String> = listOf())

fun Route.createDir() {

    post("/createdir") {

        val createDir = call.receive<CreateDir>()

        val paths = CloudFileUtil.getWholePath(
            createDir.paths,
            call.queryParameters["token"]!!
        )

        val path = FileUtil.getWholePath(Cons.Path.DATA_DIR, paths)

        val file = File(path)

        val codeMessage = if (file.exists()) {
            CodeMessage.DIR_ALREADY_EXIST
        } else if (file.mkdirs()) {
            CodeMessage.OK
        } else {
            CodeMessage.CREATE_DIR_FAIL
        }

        logger.info {
            "file:${file}"
        }

        call.respond(Response<Unit>(codeMessage.code, codeMessage.message))
    }


}