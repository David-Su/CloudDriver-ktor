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
    val logFile = File(logDir, "logger.log")

    if (!logDir.exists()) logDir.mkdirs()
    if (!logFile.exists()) logFile.createNewFile()

    val fileHandler = FileHandler(logFile.absolutePath, true).apply {
        formatter = SimpleFormatter()
    }

    logger.addHandler(fileHandler)
}