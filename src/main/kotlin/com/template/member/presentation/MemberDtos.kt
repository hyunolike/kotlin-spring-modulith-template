package com.template.member.presentation

import com.template.member.MemberInfo
import com.template.member.MemberStatus
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

// 길이 제한은 컬럼 길이(varchar(255))와 맞춘다. 검증이 없으면 DB 오류가 500으로 올라간다.
data class RegisterMemberRequest(
    @field:NotBlank
    @field:Size(max = 255)
    val name: String,
    @field:NotBlank
    @field:Email
    @field:Size(max = 255)
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
