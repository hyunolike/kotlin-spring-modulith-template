package com.template

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.data.jpa.repository.config.EnableJpaAuditing
import org.springframework.modulith.Modulithic
import org.springframework.scheduling.annotation.EnableAsync
import org.springframework.scheduling.annotation.EnableScheduling

// 전역 인프라 설정은 루트에 둔다. @ApplicationModuleTest가 개별 모듈만 부트스트랩할 때도
// 루트 애플리케이션 클래스의 설정은 항상 적용되기 때문이다.
@EnableAsync // @ApplicationModuleListener의 비동기 실행에 필요
@EnableJpaAuditing // BaseTimeEntity의 createdAt/updatedAt 자동 기록
@EnableScheduling // EventPublicationMaintenance와 Modulith 이벤트 staleness 모니터에 필요
// shared는 모든 모듈이 쓰는 인프라(예외 처리, 이벤트 레지스트리 스키마)를 가진다. 공유 모듈로 선언해야
// @ApplicationModuleTest가 모듈 하나만 띄울 때도 shared의 빈과 마이그레이션이 함께 적용된다.
@Modulithic(sharedModules = ["shared"])
@SpringBootApplication
class TemplateApplication

fun main(args: Array<String>) {
    runApplication<TemplateApplication>(*args)
}
