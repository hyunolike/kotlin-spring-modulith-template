package com.template

import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.context.TestContext
import org.springframework.test.context.TestExecutionListener
import javax.sql.DataSource

/**
 * 각 테스트 메서드 전에 모든 테이블을 비운다 (Flyway 이력 테이블 제외).
 *
 * 테스트끼리 데이터를 공유하면 "다른 테스트가 쓰지 않는 값"을 골라야만 통과하는 테스트가 되고,
 * 실행 순서에 따라 깨진다. src/test/resources/META-INF/spring.factories에 등록되어
 * 모든 Spring 테스트에 자동으로 적용되므로 테스트 클래스마다 선언할 필요가 없다.
 */
class DatabaseCleanupListener : TestExecutionListener {
    override fun beforeTestMethod(testContext: TestContext) {
        val dataSource = testContext.applicationContext.getBeanProvider(DataSource::class.java).ifAvailable ?: return
        val jdbcTemplate = JdbcTemplate(dataSource)
        val tables =
            jdbcTemplate.queryForList(
                """
                select quote_ident(tablename) from pg_tables
                where schemaname = current_schema() and tablename not like 'flyway_schema_history%'
                """.trimIndent(),
                String::class.java,
            )
        if (tables.isNotEmpty()) {
            jdbcTemplate.execute("truncate table ${tables.joinToString()} restart identity cascade")
        }
    }
}
