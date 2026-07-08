package com.template.member.presentation

import com.template.member.MemberInfo
import com.template.member.MemberStatus
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank

data class RegisterMemberRequest(
    @field:NotBlank
    val name: String,
    @field:NotBlank
    @field:Email
    val email: String,
)

data class MemberResponse(
    val id: Long,
    val name: String,
    val email: String,
    val status: MemberStatus,
) {
    companion object {
        fun from(info: MemberInfo): MemberResponse =
            MemberResponse(id = info.id, name = info.name, email = info.email, status = info.status)
    }
}
