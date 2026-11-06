package org.dueattendant149.vkx.network.model

import kotlinx.serialization.Serializable

@Serializable
data class RegisterRequest(
    val email: String,
    val password: String,
    val displayName: String,
    val inviteToken: String,
    val deviceId: String? = null,
)

@Serializable
data class LoginRequest(
    val email: String,
    val password: String,
    val deviceId: String? = null,
)

@Serializable
data class RefreshRequest(
    val refreshToken: String,
)

@Serializable
data class LogoutRequest(
    val refreshToken: String,
)

@Serializable
data class AuthResponse(
    val accessToken: String,
    val refreshToken: String,
    val expiresIn: Long,
    val tokenType: String = "Bearer",
    val user: User,
) {
    fun toSession(nowEpochSec: Long = System.currentTimeMillis() / 1000): Session =
        Session(
            accessToken = accessToken,
            refreshToken = refreshToken,
            expiresAtEpochSec = nowEpochSec + expiresIn.coerceAtLeast(60),
            user = user,
        )
}
