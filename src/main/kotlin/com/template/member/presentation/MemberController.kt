package com.template.member.presentation

import com.template.member.application.MemberService
import com.template.shared.response.ApiResponse
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/members")
class MemberController(
    private val memberService: MemberService,
) {
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun register(
        @Valid @RequestBody request: RegisterMemberRequest,
    ): ApiResponse<MemberResponse> {
        val member = memberService.register(request.name, request.email)
        return ApiResponse.success(MemberResponse.from(member))
    }

    @GetMapping("/{memberId}")
    fun getMember(
        @PathVariable memberId: Long,
    ): ApiResponse<MemberResponse> = ApiResponse.success(MemberResponse.from(memberService.getMember(memberId)))

    @PostMapping("/{memberId}/deactivate")
    fun deactivate(
        @PathVariable memberId: Long,
    ): ApiResponse<Unit> {
        memberService.deactivate(memberId)
        return ApiResponse.success(Unit)
    }
}
