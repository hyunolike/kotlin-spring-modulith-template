package com.template.shared.config

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.modulith.events.CompletedEventPublications
import org.springframework.modulith.events.FailedEventPublications
import org.springframework.modulith.events.ResubmissionOptions
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.Duration

/**
 * Event Publication Registry 운영 작업.
 *
 * - 재제출: 리스너가 실패한 이벤트는 레지스트리에 FAILED로 남을 뿐 스스로 다시 실행되지 않는다.
 *   주기적으로 다시 제출해야 일시적 장애(락 충돌, 외부 시스템 오류)가 복구된다. 재제출은 DB에서
 *   원자적으로 선점하므로 인스턴스가 여러 대여도 같은 이벤트를 한 번만 다시 실행한다.
 *   처리 중 멈춘 이벤트(인스턴스 종료 등)는 spring.modulith.events.staleness 설정이 FAILED로 바꿔 준다.
 * - 정리: completion-mode=archive는 완료된 이벤트를 archive 테이블로 옮기기만 하므로 보존 기간이 지나면 지운다.
 *
 * 리스너는 같은 이벤트를 두 번 받아도 결과가 같도록(멱등) 작성해야 한다.
 */
@Component
class EventPublicationMaintenance(
    private val failedPublications: FailedEventPublications,
    private val completedPublications: CompletedEventPublications,
    @Value("\${template.events.resubmission.max-attempts}") private val maxAttempts: Int,
    @Value("\${template.events.archive.retention}") private val archiveRetention: Duration,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Scheduled(fixedDelayString = "\${template.events.resubmission.interval}")
    fun resubmitFailedPublications() {
        // 계속 실패하는 이벤트를 영원히 재시도하지 않는다. 한도를 넘긴 이벤트는 FAILED로 남아 사람이 확인한다.
        failedPublications.resubmit(
            ResubmissionOptions.defaults().withFilter { it.completionAttempts < maxAttempts },
        )
    }

    @Scheduled(cron = "\${template.events.archive.purge-cron}")
    fun purgeArchivedPublications() {
        completedPublications.deletePublicationsOlderThan(archiveRetention)
        log.info("보존 기간({})이 지난 완료 이벤트를 정리했습니다.", archiveRetention)
    }
}
