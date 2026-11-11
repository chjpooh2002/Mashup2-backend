# mashup2-backend

CEOS Mashup Day 2팀 백엔드 프로젝트

## 실행 환경

- Java 21
- Gradle 

## 실행 방법

```bash
./gradlew bootRun
```

Windows(PowerShell / CMD)에서는 다음 명령어를 사용합니다.

```bash
gradlew bootRun
```

기본 포트는 `8080`입니다. 실행 후 `http://localhost:8080`으로 접속할 수 있습니다.

## API

### Health Check

**Response** `200 OK`

```json
{
  "status": "ok"
}
```

**확인 방법**

```bash
curl http://localhost:8080/api/health
```

## 프로젝트 회고

### 프로젝트 소개

CEOS 24기 MashUp Day 2팀이 해외송금 서비스 센트비를 역기획한 3주 단기 프로젝트입니다.
기존 앱은 처음 금액을 입력하는 화면에서 "받는 방법" 목록을 바로 볼 수 없다는 사용자 경험 문제가 있어, 금액을 입력하면 받는 방법 목록을 함께 비교할 수 있도록 바꾸는 데 집중했습니다.
환율 표시 방식 등 나머지 흐름은 기존 센트비 앱을 그대로 따라갔습니다.

### 백엔드 역할 분담

| 이름 | 담당 | 주요 작업 |
| --- | --- | --- |
| 홍준 | 배포, 환율 | EC2 + RDS + Docker 배포, PR 빌드·테스트 CI와 main 머지 시 자동 배포 CD ([#9](https://github.com/chjpooh2002/Mashup2-backend/pull/9))<br>Open Exchange Rates 연동 환율 갱신 스케줄러, 인도네시아·베트남·싱가포르·캄보디아 4개 통화 ([#14](https://github.com/chjpooh2002/Mashup2-backend/pull/14))<br>환율 조회 API ([#20](https://github.com/chjpooh2002/Mashup2-backend/pull/20)) |
| 김강민 | 공통 구조, 받는 방법, 송금 이력 | 공통 응답 형식·예외 처리·Swagger 설정, 이슈·PR 템플릿 ([#1](https://github.com/chjpooh2002/Mashup2-backend/pull/1), [#3](https://github.com/chjpooh2002/Mashup2-backend/pull/3))<br>ERD 기반 엔티티 설계 ([#5](https://github.com/chjpooh2002/Mashup2-backend/pull/5))<br>받는 방법 목록 조회 API ([#7](https://github.com/chjpooh2002/Mashup2-backend/pull/7)), 상세 조회 API ([#13](https://github.com/chjpooh2002/Mashup2-backend/pull/13))<br>최근 송금 이력 조회 API와 환율 표시 방향 결정 로직 ([#16](https://github.com/chjpooh2002/Mashup2-backend/pull/16))<br>송금 이력 저장 API ([#18](https://github.com/chjpooh2002/Mashup2-backend/pull/18))<br>받는 방법 초기 데이터 ([#22](https://github.com/chjpooh2002/Mashup2-backend/pull/22)) |

### 기술적으로 고민한 점 (홍준)

1. **외부 호출과 트랜잭션 분리**
   `ExchangeRateSyncService`는 트랜잭션 없이 외부 API를 호출하고, 검증을 통과한 값만 `ExchangeRateService.saveRates`에서 하나의 트랜잭션으로 저장합니다.
   외부 응답을 기다리는 동안 DB 커넥션을 잡고 있지 않기 위해서입니다. 커넥션 풀은 별도 설정 없이 기본값을 쓰고 있으며, 풀 크기 조정 필요 여부는 확인 필요입니다.
2. **실패하면 재시도하지 않고 기존 값 유지**
   무료 플랜 호출 한도(월 1,000회) 때문에 재시도하지 않습니다. KRW 환율 누락, 0 이하 값, 대상 통화 누락 중 하나라도 있으면 전체를 저장하지 않아 일부 통화만 갱신되는 상태를 막았습니다.
   외부 호출에는 연결 5초, 읽기 10초 시간 제한을 두었습니다.
3. **스케줄러 주기와 실행 환경**
   `prod` 프로필에서만 빈을 등록해 로컬·테스트에서는 호출 한도를 쓰지 않습니다. 매시 5분(`0 5 * * * *`)에 실행되어 한 달 최대 약 744회로 한도 안에 들어갑니다.
   정각이 아닌 5분으로 정한 이유는 코드에 남아 있지 않아 확인 필요입니다.
4. **비밀 값 노출 줄이기**
   환율 API 예외 메시지에는 앱 키가 담긴 URL이 들어가므로 로그에는 상태 코드와 예외 종류만 남깁니다.
   CD는 AWS 액세스 키 대신 OIDC로 임시 역할을 맡고, 배포하는 동안에만 러너 IP에 22번 포트를 열었다가 성공·실패와 관계없이 규칙을 지웁니다.

### 이 프로젝트의 의미

기획자와 함께 실제 서비스의 사용자 경험 문제를 정의하고, 그것을 "금액 입력과 함께 받는 방법 비교"라는 API 설계로 옮겨 본 경험이었습니다.
3주 안에 배포까지 끝내기 위해 아래 항목은 알고 내린 선택입니다.

- **탄력적 IP 미사용**: 인스턴스를 다시 시작하면 공인 IP가 바뀌어 `EC2_HOST` 시크릿과 프론트 설정을 다시 맞춰야 합니다.
  → 실제 운영이었다면 탄력적 IP나 도메인을 연결해 주소를 고정했을 것입니다.
- **`ddl-auto: update` 유지**: 엔티티 변경이 바로 스키마에 반영되지만, 컬럼 삭제·타입 변경은 RDS에 직접 `ALTER`해야 합니다.
  → 실제 운영이었다면 Flyway 같은 마이그레이션 도구로 스키마 변경을 코드로 관리하고 `validate`로 바꿨을 것입니다.
- **인증/인가 제외**: 송금 이력의 `userId`를 1로 고정했습니다.
  → 실제 운영이었다면 로그인과 사용자별 이력 조회, 권한 검사를 먼저 붙였을 것입니다.