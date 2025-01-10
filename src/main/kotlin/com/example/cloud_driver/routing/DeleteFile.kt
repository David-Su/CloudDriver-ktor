package com.example.cloud_driver.routing

import com.example.cloud_driver.config.Cons
import com.example.cloud_driver.manager.logger
import com.example.cloud_driver.model.net.CodeMessage
import com.example.cloud_driver.model.net.Response
import com.example.cloud_driver.util.CloudFileUtil
import com.example.cloud_driver.util.FileUtil
import com.example.cloud_driver.util.PreviewFileUtil
import com.example.cloud_driver.util.TokenUtil
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import java.io.File

@Serializable
data class DeleteFile(val paths: List<String> = listOf())

fun Route.deleteFile() {
    post("/deletefile") {
        val location = CloudFileUtil.getWholePath(
            call.receive<DeleteFile>().paths,
            TokenUtil.getUsername(call.queryParameters["token"]!!)
        )
        val path = FileUtil.getWholePath(Cons.Path.DATA_DIR, location)
        logger.info {
            "path:$path"
        }
        val dataFile = File(path)
        //先删除预览图
        if (dataFile.isDirectory) {
            FileUtil.deleteFile(PreviewFileUtil.getPreviewParentFile(dataFile, File(Cons.Path.DATA_DIR)))
        } else {
            PreviewFileUtil
                .getPreviewFile(dataFile, File(Cons.Path.DATA_DIR))
                ?.also {
                    logger.info {
                        "PreviewFile:${it.absolutePath}"
                    }
                    FileUtil.deleteFile(it)
                }
        }
        //整个文件夹或文件删除
        FileUtil.deleteFile(dataFile)

        call.respond(Response<Unit>(CodeMessage.OK.code, CodeMessage.OK.message))
    }
}