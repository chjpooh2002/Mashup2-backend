# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 프로젝트 개요

CEOS 24기 MashUp Day 2팀 백엔드. 해외 송금 서비스(센트비) 비교 API를 제공한다.
Java 21 / Spring Boot 4.1.1 / Gradle / MySQL / JPA.

## 명령어

Windows에서는 `./gradlew` 대신 `gradlew`를 사용한다.

```bash
./gradlew bootRun                          # 애플리케이션 실행 (기본 포트 8080)
./gradlew build                            # 전체 빌드 (컴파일 + 테스트)
./gradlew test                             # 전체 테스트
./gradlew test --tests "*HealthCheck*"     # 단일 테스트 클래스/메서드 실행
./gradlew compileJava                      # 테스트 없이 컴파일만
```

- Swagger UI: `http://localhost:8080/swagger-ui/index.html`
- Health check: `curl http://localhost:8080/api/health`
- 테스트 실패 리포트: `build/reports/tests/test/index.html`

### DataSource 필수

`application.yaml`의 datasource는 `${DB_URL}` / `${DB_USERNAME}` / `${DB_PASSWORD}` 플레이스홀더를 쓴다.
이 값이 없으면 **`bootRun`도 `test`도 실패한다** (`Failed to configure a DataSource`).

값은 프로젝트 루트의 `.env` 파일로 주입한다 — `spring.config.import: optional:file:.env[.properties]`가 이를 읽는다.
`.env`는 properties 형식이며 git에 커밋되어 있지 않다 (단, `.gitignore`에도 등록되어 있지 않으므로 커밋하지 않도록 주의).

```properties
DB_URL=jdbc:mysql://localhost:3306/<db>
DB_USERNAME=<user>
DB_PASSWORD=<password>
```

`ddl-auto: update`, `open-in-view: false`, SQL 로깅(debug)은 로컬 개발 기준 설정이다.

## 아키텍처

패키지 루트: `com.ceos24.mashup2backend`

```
global/          공통 인프라 (응답 형식, 예외 처리, 설정)
  apipayload/    ApiResponse + 코드 체계 + 예외 + 핸들러
  config/        SwaggerConfig
  healthcheck/   HealthCheckController
domain/          도메인별 패키지 (현재 비어 있음)
```

### 공통 응답 규약 — 모든 API가 따라야 함

컨트롤러는 `ResponseEntity`가 아니라 **`ApiResponse<T>`를 직접 반환**한다.
응답 JSON은 항상 `{ isSuccess, code, message, result }` 형태이며 순서가 고정되어 있다.

```java
@GetMapping("/api/health")
public ApiResponse<Void> healthCheck() {
    return ApiResponse.onSuccess(GeneralSuccessCode.OK);
}
```

- 결과 데이터가 있으면 `ApiResponse.onSuccess(code, result)`, 없으면 `ApiResponse.onSuccess(code)`.
- `ApiResponse`의 생성자는 `@RequiredArgsConstructor`로 막혀 있으니 정적 팩토리만 사용한다.

### 에러/성공 코드 체계

`BaseErrorCode` / `BaseSuccessCode` 인터페이스는 `HttpStatus status`, `String code`, `String message` 세 가지를 요구한다.
공통 코드는 `GeneralErrorCode` / `GeneralSuccessCode` enum에 있고,
**도메인별 코드는 해당 도메인 패키지에서 같은 인터페이스를 구현하는 enum을 새로 만들어 추가한다** (공통 enum을 키우지 않는다).

코드 문자열 규칙:
- 에러: `COMMON400_1`처럼 `<도메인>` + `<HTTP 상태>` + `_<일련번호>`
- 성공: `200_1`, `201_1`

### 예외 처리

비즈니스 예외는 `throw new ProjectException(SomeErrorCode.XXX)` 한 가지 방식으로만 던진다.
`ProjectException`은 `BaseErrorCode`만 들고 있는 `RuntimeException`이고,
`GeneralExceptionHandler`(`@RestControllerAdvice`)가 이를 받아 코드에 담긴 `status`로 응답을 만든다.

핸들러가 이미 처리하는 예외들 — 중복 핸들러를 추가하지 말 것:

| 예외 | 상황 | `result` 내용 |
| --- | --- | --- |
| `ProjectException` | 비즈니스 예외 | 없음 |
| `MethodArgumentNotValidException` | `@RequestBody`/`@ModelAttribute` 검증 실패 | `{필드명: 메시지}` Map |
| `MethodArgumentTypeMismatchException` | `@PathVariable`/`@RequestParam` 타입 변환 실패 | 설명 문자열 |
| `HandlerMethodValidationException` | 단일 파라미터 검증 실패 | `{파라미터명: 메시지}` Map |
| `RuntimeException` | 그 외 전부 | 없음 (500) |

`GeneralExceptionHandler`는 `ResponseEntityExceptionHandler`를 상속하지 **않는다**. 새 핸들러도 같은 스타일로 `@ExceptionHandler` 메서드만 추가한다.

### Spring Boot 4 주의

의존성 이름이 Boot 3과 다르다 — `spring-boot-starter-webmvc`, `spring-boot-starter-webmvc-test`,
`spring-boot-starter-validation-test`를 사용한다 (`-web`, `-test`가 아님).
Boot 3 기준의 예제 코드를 그대로 가져오면 artifact를 찾지 못한다.

## 협업 규칙

- 커밋 메시지: `<type>: <한글 설명>` (`feat`, `fix`, `refactor`, `chore`, `docs`)
- 브랜치: `<type>/<kebab-case-주제>/<이슈번호>` (예: `feature/entity-build/4`)
- 이슈/PR은 `.github/ISSUE_TEMPLATE/`, `.github/PULL_REQUEST_TEMPLATE.md` 템플릿 구조를 그대로 채운다.
- 사용 가능한 라벨: `docs`, `feat`, `fix`, `refactor`, `api`, `chore`, `db`, `test`
  (feature_request 템플릿의 frontmatter에는 존재하지 않는 `feature` 라벨이 적혀 있다)
- 코드 주석과 로그 메시지는 한국어로 작성한다.
