package com.placidmasvidal.reactiveKotlin.controllers

import com.placidmasvidal.reactiveKotlin.models.User
import com.placidmasvidal.reactiveKotlin.repositories.UserRepository
import com.placidmasvidal.reactiveKotlin.requests.UserCreateRequest
import com.placidmasvidal.reactiveKotlin.responses.PagingResponse
import com.placidmasvidal.reactiveKotlin.responses.UserCreateResponse
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.reactive.awaitFirstOrNull
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.*
import org.springframework.web.server.ResponseStatusException
import java.lang.Exception
import javax.validation.Valid

@RestController
@RequestMapping("/users", produces = [MediaType.APPLICATION_JSON_VALUE])
class UserController {

    @Autowired
    lateinit var userRepository: UserRepository

    @PostMapping
    suspend fun createUser(
        @RequestBody @Valid request: UserCreateRequest
    ): UserCreateResponse {
        val existingUser = userRepository.findByEmail(request.email).awaitFirstOrNull()
        if(existingUser != null) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Duplicate user")
        }

        val user = User(
            id = null,
            email = request.email,
            firstName = request.firstName,
            lastName = request.lastName,
        )

        val createdUser = try {
            userRepository.save(user).awaitFirstOrNull()
        } catch (e : Exception){
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Unable to create user", e)
        }

        val id = createdUser?.id ?:
        throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Missing id for created user")

        return UserCreateResponse(
            id = id,
            email = createdUser.email,
            firstName = createdUser.firstName,
            lastName = createdUser.lastName
        )
    }

    @GetMapping("")
    suspend fun listUsers(
        @RequestParam pageNo: Int = 1,
        @RequestParam pageSize: Int = 10
    ): PagingResponse<User> {
        val limit = pageSize
        val offset = (limit * pageNo) - limit
        val list = userRepository.findAllUsers(limit, offset).collectList().awaitFirst()
        val total = userRepository.count().awaitFirst()
        return PagingResponse(total, list)
    }
}