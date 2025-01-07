package com.example.cloud_driver.routing

import com.example.cloud_driver.manager.UploadTaskManager
import com.example.cloud_driver.manager.logger
import com.example.cloud_driver.model.net.UploadTask
import com.example.cloud_driver.util.TokenUtil
import io.ktor.server.routing.*
import io.ktor.server.sse.*
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private const val EVENT_ID_UPDATE = "event_id_update"
private const val EVENT_ID_REMOVE = "event_id_remove"

private data class ChannelElement(val eventId: String, val data: Any)

fun Route.uploadTasks() {

    sse("/uploadtasks") {

        val token = call.request.queryParameters["token"]

        if (token.isNullOrEmpty() || !TokenUtil.valid(token)) {
            logger.info { "Token is invalid token: $token" }
            cancel()
            return@sse
        }

        val username = TokenUtil.getUsername(token)

        val channel = Channel<ChannelElement>()


        UploadTaskManager.addListener(username, object : UploadTaskManager.Listener {
            override fun onTasksUpdate(tasks: List<UploadTask>) {
                this@sse.launch {
                    channel.send(ChannelElement(EVENT_ID_UPDATE, tasks))
                }
            }

            override fun onTaskRemove(path: String) {
                this@sse.launch {
//                    channel.send(ChannelElement(EVENT_ID_REMOVE, path))
                }
            }

        })

        while (this.coroutineContext.isActive) {
            val channelElement = channel.receive()
            val data = when (channelElement.eventId) {
                EVENT_ID_UPDATE -> {
                    try {
                        Json.encodeToString(channelElement.data)
                    } catch (e: Exception) {
                        e.printStackTrace()
                        "Hello"
                    }
                }
                else -> Json.encodeToString(channelElement.data)
            }
            this.send(data = data, id = channelElement.eventId, event = channelElement.eventId)
        }
    }
}