package com.example.cloud_driver.model.net

import kotlinx.serialization.Serializable

@Serializable
data class Response<T>(val code: String, val message: String, val result: T? = null)