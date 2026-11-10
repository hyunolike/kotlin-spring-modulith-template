-- shared 모듈의 초기 스키마. 이력은 flyway_schema_history_shared 테이블에 따로 기록된다.
-- 모든 모듈이 함께 쓰는 인프라 테이블을 둔다. 시각 컬럼은 모두 timestamptz다.

-- Spring Modulith Event Publication Registry (spring-modulith-starter-jpa).
-- serialized_event는 Hibernate 자동 생성 기준으로는 varchar(255)지만 이벤트 페이로드가 커질 수 있어 text로 둔다.
create table event_publication
(
    id                     uuid primary key,
    event_type             varchar(255)             not null,
    listener_id            varchar(255)             not null,
    serialized_event       text                     not null,
    publication_date       timestamp with time zone not null,
    completion_date        timestamp with time zone,
    status                 varchar(255),
    completion_attempts    integer                  not null,
    last_resubmission_date timestamp with time zone,
    constraint ck_event_publication_status
        check (status in ('PUBLISHED', 'PROCESSING', 'COMPLETED', 'FAILED', 'RESUBMITTED'))
);

-- 미완료 이벤트 재발행(republish-outstanding-events-on-restart, resubmitIncompletePublications) 조회용
create index idx_event_publication_incomplete on event_publication (publication_date) where completion_date is null;

-- spring.modulith.events.completion-mode=archive: 완료된 이벤트는 이 테이블로 옮겨져 위 테이블이 작게 유지된다.
create table event_publication_archive
(
    id                     uuid primary key,
    event_type             varchar(255)             not null,
    listener_id            varchar(255)             not null,
    serialized_event       text                     not null,
    publication_date       timestamp with time zone not null,
    completion_date        timestamp with time zone,
    status                 varchar(255),
    completion_attempts    integer                  not null,
    last_resubmission_date timestamp with time zone,
    constraint ck_event_publication_archive_status
        check (status in ('PUBLISHED', 'PROCESSING', 'COMPLETED', 'FAILED', 'RESUBMITTED'))
);
