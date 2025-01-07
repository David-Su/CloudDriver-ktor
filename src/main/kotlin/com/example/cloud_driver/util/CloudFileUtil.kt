package com.example.cloud_driver.util

import com.example.cloud_driver.config.Cons
import com.example.cloud_driver.manager.logger


object CloudFileUtil {

    fun getWholePath(pathsStr: String, root: String): String {
        return getWholePath(pathsStr.split(",".toRegex()), root)
    }

    fun getWholePath(paths: List<String>, root: String): String {
        return paths
                .let {
                    if (it.getOrNull(0) == Cons.Path.USER_DIR_STUB) {
                        it.toMutableList().also {
                            it[0] = root
                        }
                    } else it
                }.let {
                    logger.info { "getWholePath:${it}" }
                FileUtil.getWholePath(it)
                }
    }

}