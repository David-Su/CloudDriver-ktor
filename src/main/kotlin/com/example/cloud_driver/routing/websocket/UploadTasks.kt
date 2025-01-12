package com.example.cloud_driver.routing.websocket

import com.example.cloud_driver.manager.UploadTaskManager
import com.example.cloud_driver.manager.logger
import com.example.cloud_driver.model.net.UploadTask
import com.example.cloud_driver.util.TokenUtil
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private const val KEY_DATA_TYPE = "dataType"
private const val KEY_DATA = "data"

//更新任务上传进度
private const val DATA_TYPE_UPDATE = 0

//移除任务
private const val DATA_TYPE_REMOVE = 1

@Serializable
private data class Send<T>(val dataType: Int, val data: T)

fun Route.websocketUploadTasks() {

    webSocket("/websocket/uploadtasks") {

        logger.info { "enter ws" }

        val token = call.request.queryParameters["token"]

        val username = TokenUtil.getUsername(token)

        val channel = Channel<String>()

        UploadTaskManager.addListener(username, object : UploadTaskManager.Listener {
            override fun onTasksUpdate(tasks: List<UploadTask>) {
                this@webSocket.launch(CoroutineExceptionHandler { coroutineContext, throwable ->
                    throwable.printStackTrace()
                    logger.info { "onTasksUpdate error:${throwable}" }
                }) {
                    val text = Send(
                        DATA_TYPE_UPDATE,
                        tasks,
                    ).let {
                        Json.encodeToString(it)
                    }
                    logger.info { "send text:${text}" }
                    channel.send(text)
                }
            }

            override fun onTaskRemove(path: String) {
                this@webSocket.launch(CoroutineExceptionHandler { coroutineContext, throwable ->
                    throwable.printStackTrace()
                    logger.info { "onTaskRemove error:${throwable}" }
                }) {
                    val text = Send(
                        DATA_TYPE_REMOVE,
                        path,
                    ).let {
                        Json.encodeToString(it)
                    }
                    logger.info { "send text:${text}" }
                    channel.send(text)
                }
            }

        })

        logger.info { "context isActive1:${coroutineContext.isActive}" }

        while (coroutineContext.isActive) {
            val channelElement = channel.receive()
            this.send(Frame.Text(channelElement))
        }

        logger.info { "context isActive3:${coroutineContext.isActive}" }
    }
}