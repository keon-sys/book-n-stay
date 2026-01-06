package org.keon.book.domain

data class User(
    val accountId: String,
    val nickname: String,
    val grantLevel: Int,
)