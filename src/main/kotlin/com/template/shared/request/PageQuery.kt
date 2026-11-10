package com.template.shared.request

import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort

/**
 * 목록 조회 API의 공통 쿼리 파라미터 (?page=0&size=20).
 *
 * Spring Data의 Pageable을 컨트롤러에서 직접 받지 않는 이유: 클라이언트가 sort에 임의의 프로퍼티를 넣으면
 * 500이 나거나 인덱스를 타지 못하는 정렬이 실행된다. 정렬은 각 API가 [toPageable]에 고정해서 넘긴다.
 */
data class PageQuery(
    @field:Min(0)
    val page: Int = 0,
    @field:Min(1)
    @field:Max(MAX_SIZE)
    val size: Int = DEFAULT_SIZE,
) {
    fun toPageable(sort: Sort): Pageable = PageRequest.of(page, size, sort)

    companion object {
        const val DEFAULT_SIZE = 20
        const val MAX_SIZE = 100L
    }
}
