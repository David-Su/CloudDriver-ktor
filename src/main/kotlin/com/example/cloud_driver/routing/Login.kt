package com.example.cloud_driver.routing

import com.example.cloud_driver.config.Cons
import com.example.cloud_driver.model.net.CodeMessage
import com.example.cloud_driver.model.net.Response
import com.example.cloud_driver.model.net.Token
import com.example.cloud_driver.util.FileUtil
import com.example.cloud_driver.util.TokenUtil
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import java.io.File

@Serializable
data class Login(
    val username: String?,
    val password: String?,
)

fun Route.login() {
    post("/login") {
        val login = call.receive<Login>()
        val username = login.username
        val password = login.password

        if (username.isNullOrEmpty() || password.isNullOrEmpty()) {
            call.respond(
                Response<Unit>(
                    CodeMessage.UN_OR_PW_ERROR.code,
                    CodeMessage.UN_OR_PW_ERROR.message,
                )
            )
            return@post
        }

        if (username != "root" || password != "root") {
            call.respond(
                Response<Unit>(
                    CodeMessage.UN_OR_PW_ERROR.code,
                    CodeMessage.UN_OR_PW_ERROR.message,
                )
            )
            return@post
        }

        val token = TokenUtil.generateToken(username)

        call.application.log.info(
            buildString {
                append("\n")
                append("username->$username")
                append("\n")
                append("password->$password")
                append("\n")
                append("token->$token")
            }
        )

        //创建用户的数据文件夹
        File(FileUtil.getWholePath(Cons.Path.DATA_DIR, username))
            .mkdirs()

        call.respond(
            Response(
                CodeMessage.OK.code,
                CodeMessage.OK.message,
                Token(token)
            ),
        )
    }
}