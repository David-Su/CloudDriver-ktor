package com.example.cloud_driver.model.net

import kotlinx.serialization.Serializable

@Serializable
data class DirCloudFile(
    val name: String? = null,
    val children: List<DirCloudFileChild>? = null,
    val size: Long? = null,
) {
    @Serializable
    data class DirCloudFileChild(
        val name: String? = null,
        val isDir: Boolean = false,
        val previewImg: String? = null,
        val size: Long? = null,
    )
}
