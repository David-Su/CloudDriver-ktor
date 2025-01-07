package com.example.cloud_driver.config


import com.auth0.jwt.algorithms.Algorithm
import com.example.cloud_driver.util.FileUtil
import java.io.File

class Cons {
    object Env {
        const val IS_RELEASE = false
    }

    object Token {
        const val KEY_USERNAME = "key_username"
        private const val SECRET = "jfaksdjfiaosbjxcvbnfng"
        val ALGORITHM: Algorithm = Algorithm.HMAC256(SECRET)
    }

    object Path {
        //顶层文件夹
        private val TOP_DIR = System.getProperty("os.name")
            ?.let { os ->
                when {
                    os.contains("Linux") -> File("${File.separator}mnt${File.separator}sdb")
                        .takeIf { it.exists() }
                        ?.absolutePath

                    else -> null
                }
            } ?: System.getProperty("user.home")
        private val ROOT_DIR = FileUtil.getWholePath(TOP_DIR, "CloudDriver")
        private val TEMP_DIR = FileUtil.getWholePath(ROOT_DIR, "temp")
        val DATA_DIR = FileUtil.getWholePath(ROOT_DIR, "data")
        val TEMP_UPLOAD_DIR = FileUtil.getWholePath(TEMP_DIR, "upload")
        val TEMP_PREVIEW_DIR = FileUtil.getWholePath(TEMP_DIR, "preview")
        const val USER_DIR_STUB = "." //用户目录占位符

        init {
//            logger.info { "当前系统：${System.getProperty("os.name")}" }
//            logger.info { "ROOT_DIR: $ROOT_DIR" }
//            logger.info { "TEMP_DIR: $TEMP_DIR" }
        }
    }
}