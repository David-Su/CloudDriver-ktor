package com.example.cloud_driver.manager

import com.example.cloud_driver.model.net.UploadTask


object UploadTaskManager {

    interface Listener {
        fun onTasksUpdate(tasks: List<UploadTask>)
        fun onTaskRemove(path: String)
    }

    private val tasksMap = hashMapOf<String, ArrayList<UploadTask>>()
    private val listenerMap = hashMapOf<String, ArrayList<Listener>>()
    private val channelMap = hashMapOf<String, ArrayList<Listener>>()

    fun getCurrentTasks(username: String): List<UploadTask>? = tasksMap[username]

    fun addListener(username: String, listener: Listener): Unit = synchronized(username.intern()) {
        val listeners = listenerMap[username]
                ?: ArrayList<Listener>().also { listenerMap[username] = it }
        if (!listeners.contains(listener)) listeners.add(listener)
    }


    fun removeListener(username: String, listener: Listener): Unit = synchronized(username.intern()) {
        listenerMap[username]?.removeIf { it == listener }
    }

    fun addTask(username: String, uploadTask: UploadTask) = synchronized(username.intern()) {
        logger.info { "新任务：${uploadTask}" }
        val tasks = tasksMap[username] ?: (ArrayList<UploadTask>().also {
            logger.info { "新建任务队列：${it}" }
            tasksMap[username] = it
        })
        tasks.add(uploadTask)
        logger.info { "插入任务队列：${tasks}" }
        updateTask(username)
    }

    fun updateTask(username: String, uploadTask: UploadTask? = null): Unit = synchronized(username.intern()) {

        val tasks = tasksMap[username] ?: return@synchronized

        listenerMap[username]?.forEach { it.onTasksUpdate(tasks) }
    }

    fun removeTask(username: String, uploadTask: UploadTask? = null): Unit = synchronized(username.intern()) {

        if (uploadTask != null) {
            val tasks = tasksMap[username]
            if (tasks == null) {
                logger.info {
                    "任务队列为空"
                }
            } else {
                tasks.remove(uploadTask).also {
                    logger.info {
                        "移除${uploadTask}${if (it) "成功" else "失败"}"
                    }
                }
            }

        } else {
            tasksMap[username]?.clear()
        }

        updateTask(username)
    }
}