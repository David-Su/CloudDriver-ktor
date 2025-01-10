package com.example.cloud_driver.manager

import com.example.cloud_driver.config.Cons
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.logging.FileHandler
import java.util.logging.Level
import java.util.logging.Logger
import java.util.logging.SimpleFormatter

val logger: Logger = Logger.getLogger("CloudDriver").also { logger ->
    logger.level = Level.ALL

    val logDir = File(Cons.Path.TEMP_LOG_DIR)

    logger.info {
        logDir.absolutePath
    }

    if (logDir.exists()) {
        val today = SimpleDateFormat("yyyy-MM-dd").format(Date())
        val logFile = File(logDir, "logger_${today}.log").also {
            if (it.exists().not()) it.createNewFile()
        }
        val fileHandler = FileHandler(logFile.absolutePath, true).also {
            it.formatter = SimpleFormatter()
        }
        logger.addHandler(fileHandler)
    }

}