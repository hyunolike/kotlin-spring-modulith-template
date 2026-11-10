# 실행 이미지 빌드. 사용법은 README의 "Run as a container" 참고.
#
#   docker build -t kotlin-spring-modulith-template .

# ---- build: 실행 가능한 jar를 만들고 계층별로 푼다 ----
FROM eclipse-temurin:21-jdk AS build
WORKDIR /workspace

# 의존성을 소스보다 먼저 받는다. 빌드 스크립트가 그대로면 이 계층은 캐시에서 재사용된다.
COPY gradlew settings.gradle.kts build.gradle.kts ./
COPY gradle gradle
RUN ./gradlew --no-daemon dependencies > /dev/null

# 테스트와 정적 분석은 CI가 맡는다. 이미지 빌드는 jar만 만든다 (Testcontainers는 Docker 안에서 돌릴 수 없다).
COPY src src
RUN ./gradlew --no-daemon bootJar \
    && cp build/libs/*.jar application.jar \
    && java -Djarmode=tools -jar application.jar extract --layers --destination extracted

# ---- run: JRE만 담은 실행 이미지 ----
FROM eclipse-temurin:21-jre
WORKDIR /app

# root로 실행하지 않는다
RUN useradd --system --uid 10001 app
USER app

# 자주 바뀌는 계층(애플리케이션 코드)을 마지막에 둔다. 코드만 바뀌면 의존성 계층은 다시 올리지 않는다.
COPY --from=build /workspace/extracted/dependencies/ ./
COPY --from=build /workspace/extracted/spring-boot-loader/ ./
COPY --from=build /workspace/extracted/snapshot-dependencies/ ./
COPY --from=build /workspace/extracted/application/ ./

# prod 프로필은 DB_URL, DB_USERNAME, DB_PASSWORD 환경 변수가 필요하다
ENV SPRING_PROFILES_ACTIVE=prod
EXPOSE 8080

# 컨테이너 메모리 한도의 75%까지 힙으로 쓴다 (기본값 25%는 컨테이너에서 너무 작다)
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-jar", "application.jar"]
