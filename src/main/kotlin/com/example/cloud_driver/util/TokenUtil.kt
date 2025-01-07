package com.example.cloud_driver.util

import com.auth0.jwt.JWT
import com.auth0.jwt.exceptions.JWTVerificationException
import com.example.cloud_driver.config.Cons
import java.util.*

object TokenUtil {
    // 超时间隔
    private const val TIMEOUT = 24 * 60 * (60 * 1000)
    fun generateToken(username: String?): String {
        val now = Date().time
        return JWT
            .create()
            .withClaim(Cons.Token.KEY_USERNAME, username)
            .withExpiresAt(Date(now + TIMEOUT))
            .sign(Cons.Token.ALGORITHM)
    }

    fun valid(token: String?): Boolean {
        try {
            JWT.require(Cons.Token.ALGORITHM).build().verify(token)
        } catch (exception: JWTVerificationException) {
            return false
        }
        return true
    }

    fun timeout(token: String?): Boolean {
        return JWT.decode(token).expiresAt.before(Date())
    }

    fun getUsername(token: String?): String {
        return JWT.decode(token).getClaim(Cons.Token.KEY_USERNAME).asString()
    }
}