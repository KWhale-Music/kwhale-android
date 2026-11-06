package org.dueattendant149.vkx.network

import org.dueattendant149.vkx.network.model.Problem
import java.io.IOException

class ApiException(
    val statusCode: Int,
    val problem: Problem?,
    message: String,
) : IOException(message) {
    val code: String? get() = problem?.code
    val isUnauthorized: Boolean get() = statusCode == 401
    val isForbidden: Boolean get() = statusCode == 403
    val isNotFound: Boolean get() = statusCode == 404
}
