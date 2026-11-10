package com.template.shared.error

import org.hibernate.exception.ConstraintViolationException
import org.springframework.dao.DataIntegrityViolationException

/**
 * 이 예외가 주어진 이름의 DB 제약을 위반해서 생긴 것인지 확인한다.
 * DataIntegrityViolationException은 unique 위반뿐 아니라 길이 초과·not null 위반도 포함하므로,
 * BusinessException으로 바꾸기 전에 어느 제약인지 반드시 구분한다.
 */
fun DataIntegrityViolationException.violatesConstraint(constraintName: String): Boolean =
    generateSequence<Throwable>(this) { it.cause }
        .filterIsInstance<ConstraintViolationException>()
        .any { it.constraintName.equals(constraintName, ignoreCase = true) }
