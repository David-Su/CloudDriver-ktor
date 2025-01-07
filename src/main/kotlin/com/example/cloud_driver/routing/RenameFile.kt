package com.example.cloud_driver.routing

import com.example.cloud_driver.config.Cons
import com.example.cloud_driver.manager.logger
import com.example.cloud_driver.model.net.CodeMessage
import com.example.cloud_driver.model.net.Response
import com.example.cloud_driver.util.CloudFileUtil
import com.example.cloud_driver.util.FileUtil
import com.example.cloud_driver.util.PreviewFileUtil
import com.example.cloud_driver.util.TokenUtil
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import java.io.File

@Serializable
data class RenameFile(
    val paths: List<String> = listOf(),
    val newPaths: List<String> = listOf(),
)

fun Route.renameFile() {
    post<RenameFile>("/renamefile") { renameFile ->

        val username = TokenUtil.getUsername(call.queryParameters["token"])
        val paths = CloudFileUtil.getWholePath(renameFile.paths, username)
        val newPaths = CloudFileUtil.getWholePath(renameFile.newPaths, username)
        val sourceFile = File(FileUtil.getWholePath(Cons.Path.DATA_DIR, paths))
        val newFile = File(FileUtil.getWholePath(Cons.Path.DATA_DIR, newPaths))

        logger.info { "sourceFile:$sourceFile" }
        logger.info { "newFile:$newFile" }

        if (!sourceFile.exists()) {
            call.respond(
                Response<Unit>(
                    CodeMessage.DIR_OR_FILE_NOT_EXIST.code,
                    CodeMessage.DIR_OR_FILE_NOT_EXIST.message
                )
            )
            return@post
        }

        if (newFile.exists()) {
            call.respond(
                Response<Unit>(
                    CodeMessage.DIR_OR_FILE_ALREADY_EXIST.code,
                    CodeMessage.DIR_OR_FILE_ALREADY_EXIST.message
                )
            )
            return@post
        }

        val sourcePreviewFile: File? = if (sourceFile.isFile) PreviewFileUtil.getPreviewFile(
            sourceFile,
            File(Cons.Path.DATA_DIR)
        ) else PreviewFileUtil.getPreviewParentFile(sourceFile, File(Cons.Path.DATA_DIR))
        logger.info { "sourcePreviewFile:$sourcePreviewFile" }

        if (!sourceFile.renameTo(newFile)) {
            call.respond(Response<Unit>(CodeMessage.RENAME_FILE_FAIL.code, CodeMessage.RENAME_FILE_FAIL.message))
            return@post
        }

        /**
         * 来到这里可以保证newFile已存在而且sourceFile和newFile同为文件或文件夹
         */

        if (sourcePreviewFile != null && sourcePreviewFile.exists()) {

            val newPreviewFileParent = PreviewFileUtil.getPreviewParentFile(newFile, File(Cons.Path.DATA_DIR))

            if (sourcePreviewFile.isFile) { //sourceFile是文件的情况

                val newPreviewFile = File(newPreviewFileParent, newFile.name.let {
                    it.replaceAfterLast(".", sourcePreviewFile.extension)
                })

                if (newPreviewFile.exists()) {
                    FileUtil.deleteFile(newPreviewFile)
                } else {
                    newPreviewFile.parentFile.mkdirs()
                }

                val result = sourcePreviewFile.renameTo(newPreviewFile)

                logger.info {
                    buildString {
                        append("\n")
                        append("sourceFile是文件")
                        append("\n")
                        append("sourcePreviewFile:${sourcePreviewFile}")
                        append("\n")
                        append("newPreviewFile:${newPreviewFile}")
                        append("\n")
                        append("result:${result}")
                    }
                }
            } else if (sourcePreviewFile.isDirectory) { //sourceFile是文件夹的情况
                if (newPreviewFileParent.exists()) {
                    FileUtil.deleteFile(newPreviewFileParent)
                } else {
                    newPreviewFileParent.parentFile.mkdirs()
                }

                val result = sourcePreviewFile.renameTo(newPreviewFileParent)

                logger.info {
                    buildString {
                        append("\n")
                        append("sourceFile是文件夹")
                        append("\n")
                        append("sourcePreviewFile:${sourcePreviewFile}")
                        append("\n")
                        append("newPreviewFileParent:${newPreviewFileParent}")
                        append("\n")
                        append("result:${result}")
                    }
                }
            }

        }

        call.respond(Response<Unit>(CodeMessage.OK.code, CodeMessage.OK.message))
    }

}
