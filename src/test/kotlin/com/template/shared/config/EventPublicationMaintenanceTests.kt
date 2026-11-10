package com.template.shared.config

import com.template.TestcontainersConfiguration
import org.assertj.core.api.Assertions.assertThat
import org.awaitility.Awaitility.await
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.ApplicationEventPublisher
import org.springframework.context.annotation.Import
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.modulith.events.ApplicationModuleListener
import org.springframework.stereotype.Component
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.support.TransactionTemplate
import java.time.Duration
import java.util.concurrent.atomic.AtomicInteger

data class FlakyProbeEvent(
    val id: String,
)

data class PoisonProbeEvent(
    val id: String,
)

/** 첫 시도는 실패하고 두 번째부터 성공하는 리스너. 일시적 장애를 흉내 낸다. */
@Component
class FlakyProbeListener {
    val attempts = AtomicInteger()

    @ApplicationModuleListener
    fun on(event: FlakyProbeEvent) {
        check(attempts.incrementAndGet() > 1) { "일시적 장애 (event: ${event.id})" }
    }
}

/** 몇 번을 시도해도 실패하는 리스너. 처리할 수 없는 이벤트를 흉내 낸다. */
@Component
class PoisonProbeListener {
    val attempts = AtomicInteger()

    @ApplicationModuleListener
    fun on(event: PoisonProbeEvent) {
        attempts.incrementAndGet()
        error("처리할 수 없는 이벤트 (event: ${event.id})")
    }
}

@SpringBootTest(
    properties = [
        // 스케줄러가 테스트 도중 끼어들지 않게 주기를 길게 잡고, 재제출과 정리는 직접 호출한다
        "template.events.resubmission.interval=PT1H",
        "template.events.resubmission.max-attempts=3",
        "template.events.archive.retention=PT0S",
    ],
)
@Import(TestcontainersConfiguration::class, FlakyProbeListener::class, PoisonProbeListener::class)
class EventPublicationMaintenanceTests(
    @Autowired private val maintenance: EventPublicationMaintenance,
    @Autowired private val flakyListener: FlakyProbeListener,
    @Autowired private val poisonListener: PoisonProbeListener,
    @Autowired private val eventPublisher: ApplicationEventPublisher,
    @Autowired private val transactionManager: PlatformTransactionManager,
    @Autowired private val jdbcTemplate: JdbcTemplate,
) {
    @Test
    fun `리스너가 실패한 이벤트는 다시 제출되어 완료되고, 보존 기간이 지나면 archive에서 지워진다`() {
        publish(FlakyProbeEvent("retry-me"))
        // 1) 첫 시도가 실패하면 이벤트는 FAILED로 레지스트리에 남는다
        await().atMost(TIMEOUT).untilAsserted {
            assertThat(count("event_publication", "FlakyProbeEvent", "and status = 'FAILED'")).isEqualTo(1)
        }
        assertThat(flakyListener.attempts.get()).isEqualTo(1)

        // 2) 재제출하면 리스너가 다시 실행되어 완료되고 archive로 옮겨진다
        maintenance.resubmitFailedPublications()
        await().atMost(TIMEOUT).untilAsserted {
            assertThat(flakyListener.attempts.get()).isEqualTo(2)
            assertThat(count("event_publication", "FlakyProbeEvent")).isZero()
            assertThat(count("event_publication_archive", "FlakyProbeEvent")).isEqualTo(1)
        }

        // 3) 보존 기간(테스트에서는 0초)이 지난 완료 이벤트는 archive에서 지워진다
        maintenance.purgeArchivedPublications()
        assertThat(count("event_publication_archive", "FlakyProbeEvent")).isZero()
    }

    @Test
    fun `계속 실패하는 이벤트는 최대 시도 횟수를 채우면 더 이상 다시 제출하지 않는다`() {
        publish(PoisonProbeEvent("poison"))

        // 첫 시도를 포함해 MAX_ATTEMPTS번까지만 실행된다
        for (expectedAttempts in 1..MAX_ATTEMPTS) {
            await().atMost(TIMEOUT).untilAsserted {
                assertThat(poisonListener.attempts.get()).isEqualTo(expectedAttempts)
                assertThat(count("event_publication", "PoisonProbeEvent", "and status = 'FAILED'")).isEqualTo(1)
            }
            maintenance.resubmitFailedPublications()
        }

        // 한도를 채운 뒤에는 재제출해도 리스너가 다시 실행되지 않고, 이벤트는 FAILED로 남아 사람이 확인한다
        maintenance.resubmitFailedPublications()
        await().during(Duration.ofSeconds(1)).atMost(TIMEOUT).untilAsserted {
            assertThat(poisonListener.attempts.get()).isEqualTo(MAX_ATTEMPTS)
        }
        assertThat(count("event_publication", "PoisonProbeEvent", "and status = 'FAILED'")).isEqualTo(1)
    }

    private fun publish(event: Any) {
        TransactionTemplate(transactionManager).executeWithoutResult { eventPublisher.publishEvent(event) }
    }

    private fun count(
        table: String,
        eventType: String,
        condition: String = "",
    ): Int =
        jdbcTemplate.queryForObject(
            "select count(*) from $table where event_type like '%$eventType' $condition",
            Int::class.java,
        )!!

    companion object {
        private const val MAX_ATTEMPTS = 3
        private val TIMEOUT = Duration.ofSeconds(10)
    }
}
