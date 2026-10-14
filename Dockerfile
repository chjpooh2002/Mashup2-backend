# syntax=docker/dockerfile:1

# ===== 빌드 단계 =====
FROM eclipse-temurin:21-jdk AS builder
WORKDIR /workspace

# 의존성 레이어 캐시 — 빌드 스크립트가 바뀌지 않으면 소스 변경 시 의존성을 다시 받지 않는다
COPY gradlew settings.gradle build.gradle ./
COPY gradle gradle
RUN chmod +x gradlew
RUN --mount=type=cache,target=/root/.gradle \
    ./gradlew dependencies --no-daemon > /dev/null

# 테스트는 DataSource가 필요하므로 이미지 빌드에서는 돌리지 않는다 (CI에서 수행)
COPY src src
RUN --mount=type=cache,target=/root/.gradle \
    ./gradlew bootJar --no-daemon

# ===== 실행 단계 =====
FROM eclipse-temurin:21-jre
WORKDIR /app

# 타임존은 코드에 박지 않고 컨테이너 환경변수로 맞춘다 (CLAUDE.md 타임존 정책)
ENV TZ=Asia/Seoul

RUN groupadd --system spring && useradd --system --gid spring spring
COPY --from=builder --chown=spring:spring /workspace/build/libs/*.jar app.jar
USER spring

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
