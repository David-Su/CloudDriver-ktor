package com.example.cloud_driver.model.net

import kotlinx.serialization.Serializable

@Serializable
data class UploadTask(
    val path: String,
    var progress: Double,
    var speed: Long //字节
)
