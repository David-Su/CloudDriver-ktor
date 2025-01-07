package com.example.cloud_driver.util

import com.example.cloud_driver.manager.logger
import org.bytedeco.javacpp.Loader
import org.bytedeco.javacv.FFmpegFrameGrabber
import org.bytedeco.javacv.Java2DFrameConverter
import java.io.File
import java.io.IOException
import javax.imageio.ImageIO

object FFmpegUtil {

    fun extraMiddleFrameImg(videoPath: String, outPutPath: String): Boolean {

//        if (!isFileValid(videoPath)) {
//            return false
//        }

        val grabber: FFmpegFrameGrabber = try {
            FFmpegFrameGrabber.createDefault(videoPath)
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }

        try {
            grabber.start()
            //设置当前帧数为中间位置
            grabber.setVideoFrameNumber(grabber.lengthInFrames / 2)
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }

        val outPutFile = File(outPutPath)

        if (!outPutFile.parentFile.exists()) outPutFile.parentFile.mkdirs()

        outPutFile.delete()

        val converter = Java2DFrameConverter()
        val bi = converter.getBufferedImage(grabber.grabImage()) ?: return false

        try {
            ImageIO.write(bi, outPutFile.extension, outPutFile)
        } catch (e: IOException) {
            e.printStackTrace()
        }

        grabber.close()

        return outPutFile.exists()
    }

    private fun isFileValid(filePath: String): Boolean {
        if (File(filePath).extension != "mp4"){
            return true
        }
        val ffmpegPath = Loader.load(Class.forName("org.bytedeco.ffmpeg.ffmpeg"))
        val cmd = "${ffmpegPath} -i $filePath -v error -map 0:1 -f null -"

        val output = Runtime.getRuntime()
            .exec(cmd)
            .inputStream
            .bufferedReader()
            .use { it.readText() }

        if (output.isNotEmpty()) {
            logger.info {
                "错误"+output
            }
        }

        return output.isNullOrEmpty()
    }

}