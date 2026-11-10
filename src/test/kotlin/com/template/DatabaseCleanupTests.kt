package com.template

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.MethodOrderer
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestMethodOrder
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.jdbc.core.JdbcTemplate

/**
 * DatabaseCleanupListener가 테스트 사이에 데이터를 지우는지 검증한다.
 * 격리 자체를 확인하는 테스트라 예외적으로 실행 순서를 고정한다.
 */
@SpringBootTest
@Import(TestcontainersConfiguration::class)
@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
class DatabaseCleanupTests(
    @Autowired private val jdbcTemplate: JdbcTemplate,
) {
    @Test
    @Order(1)
    fun `앞선 테스트가 데이터를 남긴다`() {
        jdbcTemplate.update(
            """
            insert into members (name, email, status, version, created_at, updated_at)
            values ('남은회원', 'leftover@example.com', 'ACTIVE', 0, now(), now())
            """.trimIndent(),
        )

        assertThat(countMembers()).isEqualTo(1)
    }

    @Test
    @Order(2)
    fun `다음 테스트는 빈 테이블과 초기화된 ID에서 시작한다`() {
        assertThat(countMembers()).isZero()

        val id =
            jdbcTemplate.queryForObject(
                """
                insert into members (name, email, status, version, created_at, updated_at)
                values ('새회원', 'fresh@example.com', 'ACTIVE', 0, now(), now()) returning id
                """.trimIndent(),
                Long::class.java,
            )
        assertThat(id).isEqualTo(1L)
    }

    private fun countMembers(): Int = jdbcTemplate.queryForObject("select count(*) from members", Int::class.java)!!
}
