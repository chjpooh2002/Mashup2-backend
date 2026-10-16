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
- 수취 옵션 조회: `curl "http://localhost:8080/api/v1/receive-options?sendCurrency=KRW&receiveCurrency=VND&amount=1000000"`
- 환율 조회: `curl "http://localhost:8080/api/v1/exchange-rates?sendCurrency=KRW&receiveCurrency=VND"`
- 테스트 실패 리포트: `build/reports/tests/test/index.html`

### DataSource 필수

`application.yaml`의 datasource는 `${DB_URL}` / `${DB_USERNAME}` / `${DB_PASSWORD}` 플레이스홀더를 쓴다.
이 값이 없으면 **`bootRun`도 `test`도 실패한다** (`Failed to configure a DataSource`).

값은 프로젝트 루트의 `.env` 파일로 주입한다 — `spring.config.import: optional:file:.env[.properties]`가 이를 읽는다.
`.env`는 properties 형식이고 `.gitignore`에 등록되어 있다. 키 목록은 `.env.example`에 있다.

```properties
DB_URL=jdbc:mysql://localhost:3306/<db>
DB_USERNAME=<user>
DB_PASSWORD=<password>
OPEN_EXCHANGE_RATES_APP_ID=<앱 키>
```

`OPEN_EXCHANGE_RATES_APP_ID`는 빈 기본값이 있어 없어도 기동·테스트는 된다. 비어 있으면 환율 갱신이 경고 로그만 남기고 건너뛴다.

로컬 개발 기준 설정 — `ddl-auto: update`(마이그레이션 도구 없음, 엔티티 변경이 곧 스키마 변경),
`open-in-view: false`, `default_batch_fetch_size: 100`, SQL 로깅(debug).
통화 초기 데이터는 `src/main/resources/data.sql`(`INSERT IGNORE`)이 기동마다 넣는다
(`sql.init.mode: always` + `defer-datasource-initialization: true`로 테이블 생성 이후 실행). 이미 있는 행은 덮어쓰지 않는다.

## 아키텍처

패키지 루트: `com.ceos24.mashup2backend`

```
global/          공통 인프라 (응답 형식, 예외 처리, 설정)
  apipayload/    ApiResponse + 코드 체계 + 예외 + 핸들러
  config/        SwaggerConfig, SchedulingConfig (prod 전용 @EnableScheduling)
  healthcheck/   HealthCheckController
domain/
  currency/      Currency + CurrencyRepository
  exchangerate/  ExchangeRate + Open Exchange Rates 연동 환율 갱신 (클라이언트·서비스·prod 스케줄러) + 환율 조회 API
  receivemethod/ 수취 옵션 조회 API — 전체 레이어가 구현된 기준 도메인
  remittance/    최근 송금 이력 조회·저장 API (프로젝트의 유일한 쓰기 API)
```

도메인 패키지 레이어는 `receivemethod`를 기준으로 삼는다.

```
domain/<도메인>/
  controller/      @RestController
  service/         @Service
  repository/      Spring Data JPA 인터페이스
  converter/       엔티티 → 응답 DTO 매핑 (정적 메서드)
  dto/request/     요청 DTO (record)
  dto/response/    응답 DTO (record)
  dto/enums/       요청·응답에서만 쓰는 enum (영속화하지 않음)
  entity/          엔티티
  entity/enums/    엔티티 컬럼에 매핑되는 enum
  exception/       도메인 예외
  exception/code/  도메인 성공·에러 코드 enum
  client/          외부 API 호출 클라이언트 (client/dto/에 외부 응답 DTO record)
  scheduler/       @Scheduled 진입점. @Profile("prod")로 운영에서만 빈 등록, 로직은 서비스에 위임
```

`exchangerate`의 환율 갱신은 외부 호출을 트랜잭션 밖(`ExchangeRateSyncService`)에서 하고,
검증을 마친 값만 `ExchangeRateService.saveRates`의 단일 트랜잭션으로 저장한다.
호출 실패·응답 이상이면 기존 값을 유지하고 로그만 남기며, 무료 플랜 호출 한도(월 1,000회) 때문에 재시도하지 않는다.
클라이언트 예외 메시지에는 앱 키가 담긴 URL이 들어가므로 로그에 메시지를 그대로 남기지 않는다.

환율 조회 API(`GET /api/v1/exchange-rates`)는 저장용 `ExchangeRateService`에 넣지 않고
`ExchangeRateQueryService`(클래스 레벨 `readOnly`)로 분리했다 — 역할은 Sync=외부 호출, Service=저장, QueryService=조회.
`exchange_rate`만 읽고 외부 API는 호출하지 않는다. 표시 방향·표시용 환율은 `ExchangeRate.isKrwBasedDisplay()` /
`displayRate()`가 정하고, 응답은 `/api/v1/remittances/latest`의 `exchangeRate` 블록과 같은 모양이다.

enum은 영속화 여부로 가른다 — 컬럼에 매핑되면 `entity/enums/`, 요청·응답에서만 쓰면 `dto/enums/`.
도메인 API 경로는 `/api/v1/<리소스>`를 쓴다 (`global`의 헬스체크만 `/api/health`).

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

**예외 — 200이 아닌 상태를 내리는 API는 `ResponseEntity<ApiResponse<T>>`로 감싼다.**
`ApiResponse`를 그대로 반환하면 **성공 코드의 `status`가 응답에 반영되지 않는다**
(에러는 `GeneralExceptionHandler`가 `status`를 꺼내 쓰지만 성공 경로에는 그런 지점이 없다).
그대로 두면 body는 `REMITTANCE201_1`인데 HTTP 상태는 200으로 나간다.

```java
// 상태는 HttpStatus 상수를 박지 않고 성공 코드의 status에서 읽는다 (body의 code와 어긋날 수 없다).
// @ResponseStatus는 OpenAPI 문서용이다 — 없으면 springdoc이 응답을 200으로 적어 실제 상태와 문서가 어긋난다
@ResponseStatus(HttpStatus.CREATED)
@PostMapping
public ResponseEntity<ApiResponse<RemittanceSaveResponse>> saveRemittance(
        @Valid @RequestBody RemittanceSaveRequest request) {

    RemittanceSuccessCode successCode = RemittanceSuccessCode.REMITTANCE_SAVED;

    return ResponseEntity.status(successCode.getStatus())
            .body(ApiResponse.onSuccess(successCode, remittanceService.saveRemittance(request)));
}
```

`ApiResponse`에 `ResponseEntity`를 만들어주는 팩토리를 **두지 않는다.** 쓰기 API가 하나뿐이라
공통 파일을 건드리지 않고 컨트롤러에서 조립한다. 조회 API는 전부 200이라 계속 `ApiResponse<T>`를
직접 반환하며, 현재 `ResponseEntity`를 쓰는 곳은 `POST /api/v1/remittances` 하나뿐이다.

### 에러/성공 코드 체계

`BaseErrorCode` / `BaseSuccessCode` 인터페이스는 `HttpStatus status`, `String code`, `String message` 세 가지를 요구한다.
공통 코드는 `GeneralErrorCode` / `GeneralSuccessCode` enum에 있고,
**도메인별 코드는 `domain/<도메인>/exception/code/`에 같은 인터페이스를 구현하는 enum을 새로 만들어 추가한다**
(공통 enum을 키우지 않는다). 성공 코드와 에러 코드를 파일 둘로 나눈다 —
`ReceiveOptionSuccessCode` / `ReceiveOptionErrorCode`.

코드 문자열 규칙 — `<도메인>` + `<HTTP 상태>` + `_<일련번호>`:
- 에러: `COMMON400_1`
- 성공: 공통 코드는 도메인 접두사가 없지만(`200_1`, `201_1`),
  **도메인 성공 코드는 접두사를 붙인다** (예: `RECEIVE-OPTION200_1`).

### 예외 처리

비즈니스 예외는 `ProjectException`을 상속한 **도메인 예외**를 만들어 던진다.
`ProjectException`은 `BaseErrorCode`만 들고 있는 `RuntimeException`이고,
`GeneralExceptionHandler`(`@RestControllerAdvice`)가 `ProjectException` 하나로 하위 타입까지 받아
코드에 담긴 `status`로 응답을 만든다. **도메인 예외를 추가해도 핸들러는 건드리지 않는다.**

```java
// domain/receivemethod/exception/ReceiveOptionException.java
public class ReceiveOptionException extends ProjectException {
    public ReceiveOptionException(BaseErrorCode errorCode) {
        super(errorCode);
    }
}

throw new ReceiveOptionException(ReceiveOptionErrorCode.RECEIVE_CURRENCY_NOT_FOUND);
```

핸들러가 이미 처리하는 예외들 — 중복 핸들러를 추가하지 말 것:

| 예외 | 상황 | `result` 내용 |
| --- | --- | --- |
| `ProjectException` | 비즈니스 예외 | 없음 |
| `MethodArgumentNotValidException` | `@RequestBody`/`@ModelAttribute` 검증 실패 | `{필드명: 메시지}` Map |
| `MethodArgumentTypeMismatchException` | `@PathVariable`/`@RequestParam` 타입 변환 실패 | 설명 문자열 |
| `HandlerMethodValidationException` | 단일 파라미터 검증 실패 | `{파라미터명: 메시지}` Map |
| `RuntimeException` | 그 외 전부 | 없음 (500) |

`GeneralExceptionHandler`는 `ResponseEntityExceptionHandler`를 상속하지 **않는다**. 새 핸들러도 같은 스타일로 `@ExceptionHandler` 메서드만 추가한다.

### 도메인 모델

`Currency`가 모든 도메인의 중심이다. PK는 `@GeneratedValue` 없는 통화 코드 문자열(`VND`, `SGD`).

- `ExchangeRate` — `Currency`와 **PK를 공유**한다 (`@MapsId` + `@OneToOne`). 통화당 환율 1건.
  `currencyCode` 필드는 `@MapsId`가 `currency`로부터 채우므로 빌더에서 받지 않는다.
- `ReceiveMethod` — `Currency` N:1. `(receive_currency, method_type, method_provider, speed_type)` 유니크.
  `MethodType`(받는 방법) × `MethodProvider`(제공사) 조합으로 수단을 표현하고, 제공사가 없는 수단은 `MethodProvider.NONE`.
  적용 환율은 `base_rate * (1 - rate_margin)`이며 `rate_margin` 단위는 `0.001 = 0.1%`다.
  도착일은 `speedType`의 가산 일수로 산출한다 (영업일·공휴일 미고려).
  **`is_active`와 `unselectable_reason`은 다른 개념이다** — 전자는 목록에서 아예 빼고(리포지토리에서 제외),
  후자는 노출하되 선택만 막는다(응답에 포함, 맨 뒤).
- `RecentRemittance` — `Currency` N:1. 인증이 없어 `userId`는 현재 1로 고정.

### 엔티티 작성 규약

기존 엔티티 4개가 모두 같은 형태다. 새 엔티티도 여기에 맞춘다.

- `@Entity @Getter @NoArgsConstructor(access = AccessLevel.PROTECTED)` + `@Table(name = "<snake_case>")`.
- **setter를 만들지 않는다.** 생성은 `private` 생성자에 `@Builder`를 붙여서만 노출한다.
- 연관관계는 전부 `fetch = FetchType.LAZY`, 필수면 `optional = false`. 양방향 매핑과 `cascade`는 쓰지 않는다.
- enum 필드는 `@Enumerated(EnumType.STRING)` + `@Column(nullable = false, length = 30)`.
- 금액/비율은 `BigDecimal` — 금액 `precision = 19, scale = 4`, KRW 전용 고정 수수료 `19, 0`,
  매매기준율 `20, 10`, 비율 마진 `6, 5`.
- `BaseTimeEntity`나 `@EntityListeners` 같은 공통 Auditing이 **없다.** `updatedAt`, `sentAt` 같은 시각 필드는 직접 채운다.
- 필드 의미는 필드 위 한 줄 `//` 주석으로 적는다 (ERD 컬럼 설명을 그대로 옮기는 식).

**엔티티에 비즈니스 로직을 둔다.** 자기 필드만으로 판단·계산되는 규칙은 엔티티 메서드로 노출한다
(예: `ReceiveMethod.calculateTotalAmount(sendAmount, baseRate, decimalPlaces)` — 자기 `rateMargin`을 적용한다).

- 서비스에서 getter로 값을 꺼내 계산하는 코드가 보이면 그 계산을 엔티티로 옮길 후보다.
- 리포지토리 조회나 다른 도메인 의존이 필요한 로직은 엔티티가 아니라 서비스에 둔다.
- 로직을 둔다고 setter가 허용되는 것은 아니다. 상태를 바꿔야 하면 의도가 드러나는 메서드명을 쓴다
  (`deactivate()` 같은 식, `setIsActive(false)`가 아니라).

### enum 작성 규약

enum 값 하나로 완전히 결정되는 부수 데이터는 `@Getter @RequiredArgsConstructor` + `private final` 필드로
enum에 담는다. 컬럼을 따로 두면 enum 값과 어긋나는 행을 막을 방법이 없다.

```java
public enum SpeedType {

    EXPRESS("15분 ~ 1일", 0),
    STANDARD("영업일 2~3일", 2);

    private final String transferTime;  // 화면 표시용 송금 시간 문구
    private final int arrivalDays;      // 조회일 기준 도착일 가산 일수
}
```

반대로 **받는 방법·제공사별로 달라질 수 있는 값은 컬럼으로 둔다** (`fixed_fee`, `rate_margin`).
수수료나 마진을 `speedType`에 묶으면 값이 두 종류뿐이 되어 `sort=TIME`과 `sort=FEE`/`sort=RATE`의
결과가 서로 역순이거나 아예 동일해진다. 비교 API의 정렬 축이 붕괴한다.

### 조회 API 구현 규약

`receivemethod`의 수취 옵션 조회(`GET /api/v1/receive-options`)가 기준 구현이다.

- **DTO는 `record`.** 응답 DTO에 `@JsonInclude(NON_NULL)`을 붙여 `null` 필드를 응답에서 뺀다.
- **요청 DTO는 `@Valid @ModelAttribute`로 바인딩**한다. 쿼리 파라미터 기본값은 record의 compact 생성자에서
  채운다 (`sort == null`이면 `RATE`). 검증 실패는 기존 핸들러가 처리하므로 핸들러를 추가하지 않는다.
- 컨트롤러에서 쿼리 파라미터 record를 쓸 때는 **`@ParameterObject`**(springdoc)를 붙여야
  Swagger UI에 파라미터가 개별로 노출된다. 없으면 단일 객체로 문서화된다.
- **엔티티 → DTO 매핑은 `converter/`의 정적 메서드**로 뺀다 (`private` 생성자로 인스턴스화 차단).
  값을 정하는 계산은 엔티티·서비스에 두고 **컨버터는 모양만 바꾼다.**
- **다건 조회는 `@Query` + `join fetch`**로 연관을 함께 읽어 N+1을 막고 `order by <엔티티>.id asc`로 순서를 고정한다.
  계산값(수령액, 도착일)은 SQL로 정렬할 수 없어 서비스에서 정렬하는데, `List.sort`가 안정 정렬이라
  **1차 키 동률 시 조회 순서가 그대로 2차 키가 된다.** 리포지토리의 `order by`를 지우면 2차 키가 깨진다.
- `BigDecimal`을 그대로 직렬화하면 DB scale이 드러난다 (`0.00050`). 정리하려면 `stripTrailingZeros()`를 쓰되
  **scale이 0인 값에는 쓰지 말 것** — `5000`이 `5E+3`으로 나간다.

### 타임존 — 코드에 박지 않는다

날짜·시각은 `LocalDate.now()`처럼 **시스템 기본 타임존을 그대로 쓴다.**
`ZoneId.of("Asia/Seoul")`을 코드에 박지 않고, 배포 시 컨테이너 `TZ` 환경변수로 맞추는 방식을 택했다
(로그·JDBC·다른 날짜 필드까지 한 knob으로 일관되게 유지하려는 의도다).

`ReceiveMethod.calculateArrivalDate()`의 `LocalDate.now()`는 **의도된 것이다. 고치지 말 것.**

- 운영: Dockerfile에 `ENV TZ=Asia/Seoul`
- 테스트를 CI에서 돌릴 때도 `TZ`를 맞춰야 날짜 단정이 깨지지 않는다 (러너 기본값은 보통 UTC).
  CI 타임존을 못 건드리는 상황이면 그때 `Clock`을 주입해 테스트에서 고정한다.
- MySQL 서버 타임존도 같이 맞춰야 `LocalDateTime` 왕복에서 값이 밀리지 않는다.

### Spring Boot 4 주의

의존성 이름이 Boot 3과 다르다 — `spring-boot-starter-webmvc`, `spring-boot-starter-webmvc-test`,
`spring-boot-starter-validation-test`를 사용한다 (`-web`, `-test`가 아님).
Boot 3 기준의 예제 코드를 그대로 가져오면 artifact를 찾지 못한다.

## 협업 규칙

- 커밋 메시지: `<type>: <한글 설명>` (`feat`, `fix`, `refactor`, `chore`, `docs`)
- 브랜치: `<type>/<kebab-case-주제>/<이슈번호>` (예: `feature/entity-build/4`)
- 이슈/PR은 `.github/ISSUE_TEMPLATE/`, `.github/PULL_REQUEST_TEMPLATE.md` 템플릿 구조를 그대로 채운다.
- 사용 가능한 라벨: `docs`, `feat`, `fix`, `refactor`, `api`, `chore`, `db`, `test`
  (이슈 템플릿 frontmatter에는 레포에 없는 라벨이 적혀 있다 — feature_request는 `feature`, bug_request는 `bug`.
  이슈를 만들 때는 각각 `feat`, `fix`로 바꿔 붙인다.)
- 코드 주석과 로그 메시지는 한국어로 작성한다.
