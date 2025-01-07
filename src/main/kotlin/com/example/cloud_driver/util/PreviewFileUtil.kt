package com.example.cloud_driver.util

import com.example.cloud_driver.config.Cons
import com.example.cloud_driver.manager.logger
import java.io.File

object PreviewFileUtil {

    fun getPreviewFile(file: File, rootFile: File): File? {
        var preview: File? = null

        if (file.isFile) {
            preview = getPreviewParentFile(file, rootFile)
                    .also { logger.info { "previewParent -> $it" } }
                    .listFiles()
                    ?.find { it.name.substringBeforeLast(".") == file.name.substringBeforeLast(".") }
                    ?: return null
        }
        return preview
    }

    fun getPreviewParentFile(file: File, rootFile: File): File {
        //相对路径
        val path = FileUtil
                .getRelativePath(file, rootFile)
                //去掉最后一个元素，只要父路径
                .let { if (it.isNotEmpty() && file.isFile) it.subList(0, it.size - 1) else it }

        return File(FileUtil.getWholePath(Cons.Path.TEMP_PREVIEW_DIR, FileUtil.getWholePath(path)))
    }

}