package com.template.member

data class MemberInfo(
    val id: Long,
    val name: String,
    val email: String,
    val status: MemberStatus,
)
