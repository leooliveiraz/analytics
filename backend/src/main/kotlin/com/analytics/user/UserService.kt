package com.analytics.user

import com.analytics.common.exception.NotFoundException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
@Transactional(readOnly = true)
class UserService(private val userRepository: UserRepository) {

    fun findById(id: UUID): User =
        userRepository.findById(id).orElseThrow { NotFoundException("User not found") }

    fun findByEmail(email: String): User =
        userRepository.findByEmail(email.trim().lowercase()) ?: throw NotFoundException("User not found")
}
