package com.template.shared.response

import org.springframework.data.domain.Page

/**
 * 목록 조회 API의 공통 응답. ApiResponse의 data에 담는다.
 * Spring Data Page를 그대로 직렬화하면 내부 구조(pageable, sort 등)가 API 계약으로 굳어지므로 필요한 값만 옮긴다.
 */
data class PageResponse<T>(
    val content: List<T>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int,
    val hasNext: Boolean,
) {
    companion object {
        fun <T : Any, R> from(
            page: Page<T>,
            transform: (T) -> R,
        ): PageResponse<R> =
            PageResponse(
                content = page.content.map(transform),
                page = page.number,
                size = page.size,
                totalElements = page.totalElements,
                totalPages = page.totalPages,
                hasNext = page.hasNext(),
            )
    }
}
