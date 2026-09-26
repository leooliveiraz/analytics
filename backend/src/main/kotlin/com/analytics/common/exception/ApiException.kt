package com.analytics.common.exception

import org.springframework.http.HttpStatus

open class ApiException(val status: HttpStatus, override val message: String) : RuntimeException(message)

class NotFoundException(message: String) : ApiException(HttpStatus.NOT_FOUND, message)

class ConflictException(message: String) : ApiException(HttpStatus.CONFLICT, message)

class BadRequestException(message: String) : ApiException(HttpStatus.BAD_REQUEST, message)

class UnauthorizedException(message: String = "Unauthorized") : ApiException(HttpStatus.UNAUTHORIZED, message)

class ForbiddenException(message: String = "Forbidden") : ApiException(HttpStatus.FORBIDDEN, message)

class TooManyRequestsException(message: String = "Too many requests") : ApiException(HttpStatus.TOO_MANY_REQUESTS, message)
