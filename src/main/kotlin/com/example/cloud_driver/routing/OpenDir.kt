package com.example.cloud_driver.routing

import com.example.cloud_driver.config.Cons
import com.example.cloud_driver.manager.logger
import com.example.cloud_driver.model.net.CodeMessage
import com.example.cloud_driver.model.net.DirCloudFile
import com.example.cloud_driver.model.net.Response
import com.example.cloud_driver.util.CloudFileUtil
import com.example.cloud_driver.util.FileUtil
import com.example.cloud_driver.util.TokenUtil
import com.google.common.net.MediaType
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.nio.file.Files
import java.util.*
import kotlin.io.path.Path

@Serializable
class OpenDir(
    val paths: List<String> = listOf()
)

fun Route.openDir() {
    post<OpenDir>("/opendir") { openDir ->
        val userName = TokenUtil.getUsername(call.queryParameters["token"])
        val paths = openDir.paths

        if (paths.isEmpty() || paths.firstOrNull() != Cons.Path.USER_DIR_STUB) {
            call.respond(Response<Unit>(CodeMessage.DIR_OR_FILE_NOT_EXIST.code, CodeMessage.DIR_OR_FILE_NOT_EXIST.message))
            return@post
        }

        val targetDirPath = userName
            .let { CloudFileUtil.getWholePath(paths, it) }
            .let { FileUtil.getWholePath(Cons.Path.DATA_DIR, it) }

        val targetDirFile = File(targetDirPath)


        if (!targetDirFile.exists() || !targetDirFile.isDirectory) {
            call.respond(Response<Unit>(CodeMessage.DIR_OR_FILE_NOT_EXIST.code, CodeMessage.DIR_OR_FILE_NOT_EXIST.message))
            return@post
        }

        val dataDir = File(Cons.Path.DATA_DIR)

        val children = targetDirFile.listFiles()
            ?.map {
                DirCloudFile.DirCloudFileChild(
                    it.name, it.isDirectory, assemblePreviewImg(it, dataDir)
                )
            }

        val userDir = File(FileUtil.getWholePath(Cons.Path.DATA_DIR, userName))

        val dirCloudFileName = if (targetDirFile == userDir) {
            Cons.Path.USER_DIR_STUB
        } else {
            targetDirFile.name
        }

        val dirCloudFile = DirCloudFile(dirCloudFileName, children)

        call.respond(Response(CodeMessage.OK.code, CodeMessage.OK.message, dirCloudFile))
    }
}

private fun assemblePreviewImg(file: File, rootFile: File): String? {
    var imgUrl: String? = null

    if (file.isFile) {

        //相对路径
        val path = FileUtil
            .getRelativePath(file, rootFile)
            //去掉最后一个元素，只要父路径
            .let { if (it.isNotEmpty()) it.subList(0, it.size - 1) else it }


        val previewParentPath = FileUtil.getWholePath(Cons.Path.TEMP_PREVIEW_DIR, FileUtil.getWholePath(path))

        logger.info {
            "previewParentPath:${previewParentPath}"
        }

        val preview = File(previewParentPath)
            .listFiles()
            ?.find { it.name.substringBeforeLast(".") == file.name }

        val fileType: Int
        val previewPath: String

        if (preview != null) { //能找到预览图就用预览图
            fileType = FILE_TYPE_TEMP_PREVIEW
            previewPath = path.toMutableList()
                .also { it.add(preview.name) }
                //username文件夹用占位符替代，DownloadFileServlet会用username取代
                .also { it[0] = Cons.Path.USER_DIR_STUB }
                .let { Json.encodeToString(it) }
        } else {
            val mimeType = Files.probeContentType(Path(file.absolutePath)) ?: return null
            val mediaType = MediaType.parse(mimeType)
            when {
                mediaType.`is`(MediaType.ANY_IMAGE_TYPE) -> { //如果是图片类型的,直接返回一个文件下载链接
                    fileType = FILE_TYPE_DATA
                    previewPath = path.toMutableList()
                        .also { it.add(file.name) }
                        //username文件夹用占位符替代，DownloadFileServlet会用username取代
                        .also { it[0] = Cons.Path.USER_DIR_STUB }
                        .let { Json.encodeToString(it) }
                }

                else -> {
                    return null
                }
            }
        }

        logger.info {
            "previewPath:${previewPath}"
        }

        imgUrl = "/downloadfile?fileType=${fileType}&filePaths=${
            Base64.getUrlEncoder().encodeToString(previewPath.toByteArray())
        }"
    }
    return imgUrl
}