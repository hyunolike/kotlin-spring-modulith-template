/**
 * 주문 모듈. 주문 생성·조회·취소를 담당한다. member 모듈에 단방향으로 의존하며,
 * 주문 생성 시 {@code MemberApi}로 회원을 검증하고 {@code MemberDeactivatedEvent}를
 * 수신하면 해당 회원의 주문을 취소한다.
 */
package com.template.order;
