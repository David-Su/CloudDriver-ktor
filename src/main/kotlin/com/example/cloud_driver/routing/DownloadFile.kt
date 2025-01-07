package com.example.cloud_driver.routing

import com.example.cloud_driver.config.Cons
import com.example.cloud_driver.manager.logger
import com.example.cloud_driver.util.CloudFileUtil
import com.example.cloud_driver.util.FileUtil
import com.example.cloud_driver.util.TokenUtil
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.partialcontent.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.json.Json
import java.io.File
import java.util.*

const val FILE_TYPE_DATA = 1
const val FILE_TYPE_TEMP_PREVIEW = 2

//下载模式
const val DOWNLOAD_MODE_DOWNLOAD = 1
const val DOWNLOAD_MODE_PLAY_ONLINE = 2


fun Route.downloadFile() {
    route("/downloadfile") {
        install(PartialContent)
    }
    get("/downloadfile") {
        val filePaths = Base64
            .getUrlDecoder()
            .decode(call.queryParameters["filePaths"])
            .toString(Charsets.UTF_8)
            .let {
                logger.info {
                    "filePaths json:$it"
                }
                Json.decodeFromString<List<String>>(it)
            }
        val fileType = call.queryParameters["fileType"]?.toInt() ?: FILE_TYPE_DATA
        val downloadMode = call.queryParameters["downloadMode"]?.toInt() ?: DOWNLOAD_MODE_DOWNLOAD
        logger.info {
            "filePaths:$filePaths"
        }

        val dir = when (fileType) {
            FILE_TYPE_TEMP_PREVIEW -> Cons.Path.TEMP_PREVIEW_DIR
            else -> Cons.Path.DATA_DIR
        }

        val path = FileUtil.getWholePath(
            dir,
            CloudFileUtil.getWholePath(
                filePaths,
                TokenUtil.getUsername(call.queryParameters["token"])
            )
        )

        val file = File(path)

        logger.info {
            "path:$path"
        }

        when (downloadMode) {
            DOWNLOAD_MODE_DOWNLOAD -> {
                // Content-Disposition 表示响应内容以何种形式展示，是以内联的形式（即网页或者页面的一部分），还是以附件的形式下载并保存到本地。
                // 这里文件名换成下载后你想要的文件名，inline表示内联的形式，即：浏览器直接下载
                call.response.header(
                    HttpHeaders.ContentDisposition,
                    ContentDisposition.Attachment.withParameter(
                        ContentDisposition.Parameters.FileName,
                        file.name
                    ).toString()
                )
                //表示服务器返回了请求的完整资源
                call.response.status(HttpStatusCode.OK)
            }

            DOWNLOAD_MODE_PLAY_ONLINE -> {
                //表示服务器成功处理了部分请求，并返回了部分资源
                call.response.status(HttpStatusCode.PartialContent)
            }
        }

        call.respondFile(file)

    }
}