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