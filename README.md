# Open Forms

Google Forms 와 유사한 설문/폼 서비스입니다. 제작자가 폼을 만들어 공개 링크로 배포하면 누구나 로그인 없이 응답할 수 있고, 제작자는 대시보드에서 응답을 집계·시각화합니다.

- **Backend**: Java 21 · Spring Boot 4.1.0 · Gradle 9.5.1 · PostgreSQL 16 · Spring Security(JWT) · Flyway
- **Frontend**: React · Vite · TypeScript · TanStack Query · Recharts
- 설계 문서: 아래 [설계 문서](#설계-문서) 섹션을 참고하십시오.

## 사전 요구 사항
- **Docker Desktop** (Compose v2 포함) — PostgreSQL 기동용
- **Node.js 20.19+ 또는 22.12+** — 프론트엔드(Vite 8)의 요구 버전입니다.
- **JDK 설치 불필요** — Gradle toolchain(foojay resolver)이 JDK 21 을 자동으로 내려받습니다.

## 빠른 시작

아래 순서대로 따라가면 clone 직후 바로 실행됩니다. 백엔드와 프론트엔드는 각각 별도 터미널에서 띄웁니다.

### 1) 클론
```bash
git clone https://github.com/SJ-Kye/open-froms.git
cd open-froms
```

### 2) 환경 변수 준비
```bash
cp .env.example .env
```
기본값 그대로도 동작합니다. `JWT_SECRET` 등을 바꾸면 DB 컨테이너와 백엔드에 함께 반영됩니다
(백엔드가 루트 `.env` 를 설정으로 읽습니다 — `application.yml` 의 `spring.config.import`).

### 3) 데이터베이스 기동 (Docker)
```bash
docker compose up -d
docker compose ps
```
PostgreSQL 16 컨테이너가 뜹니다. `docker compose ps` 의 STATUS 가 `healthy` 가 될 때까지 기다린 뒤 다음 단계로 넘어가십시오.

### 4) 백엔드 실행
```bash
cd open_forms_backend
./gradlew bootRun
```
- 서버 주소: http://localhost:8080
- 첫 실행은 Gradle 배포판과 JDK 21 을 내려받느라 수 분 걸릴 수 있습니다.
- Flyway 가 기동 시 스키마를 자동으로 마이그레이션합니다.
- API 문서(Swagger UI): http://localhost:8080/swagger-ui.html

### 5) 프론트엔드 실행 (새 터미널)
```bash
cd open_forms_frontend
npm install
npm run dev
```
개발 서버 주소는 http://localhost:5173 입니다.
개발 서버가 `/api` 요청을 `http://localhost:8080` 으로 프록시하므로 프론트엔드 `.env` 는 보통 필요 없습니다
(백엔드를 다른 호스트에 띄울 때만 `open_forms_frontend/.env.example` 참고).

### 6) 접속
| 대상 | 주소 |
|---|---|
| 앱 | http://localhost:5173 |
| API | http://localhost:8080/api |
| Swagger UI | http://localhost:8080/swagger-ui.html |

시드 계정은 없습니다. **회원가입 → 폼 생성 → 발행 → 공개 링크로 응답 → 대시보드 확인** 순서로 둘러보면 됩니다.

### 종료
```bash
docker compose down
```
컨테이너만 정리하고 데이터는 유지합니다. DB 를 완전히 초기화하려면 볼륨까지 지웁니다.
```bash
docker compose down -v
```

## 테스트

```bash
cd open_forms_backend
./gradlew test
```
H2 in-memory 를 사용하므로 Docker 없이도 실행됩니다.

## 문제 해결
- **포트 충돌** — 5432(DB)/8080(백엔드)/5173(프론트) 중 이미 쓰는 포트가 있으면, DB 는 `.env` 의 `POSTGRES_PORT` 를 바꾸고 `SPRING_DATASOURCE_URL` 의 포트도 같이 맞추십시오.
- **백엔드가 DB 연결 실패로 죽는 경우** — `docker compose ps` 로 컨테이너가 `healthy` 인지 먼저 확인하십시오.
- **Flyway 마이그레이션 오류** — 스키마가 꼬였다면 `docker compose down -v` 후 다시 `docker compose up -d` 로 초기화합니다.

## 프로젝트 구조
```
.
├── docs/                   # 설계 문서 (구현 완료분부터 순차 추가)
├── docker-compose.yml      # PostgreSQL
├── .env.example            # 환경 변수 예시
├── open_forms_backend/     # Spring Boot
└── open_forms_frontend/    # React + Vite
```

## 주요 기능
- 회원가입/로그인 (JWT 인증)
- 폼/질문 CRUD, 발행(공개 slug 링크)
- 공개 링크로 익명 응답 제출
- 대시보드: 총 응답 수 / 완료율 / 일별 추이 / 질문별 분포 (차트)

## 설계 문서

설계 문서는 [`docs/`](docs/) 에 정리되어 있습니다.

| 문서 | 소개 |
|---|---|
| [01. 서비스 개요](docs/01-service-overview.md) | 무엇을 왜 만들었는지, 핵심 사용자 흐름과 구현 범위를 설명합니다. |
| [02. 기술 스택 선택 근거](docs/02-tech-stack.md) | 프론트엔드·백엔드·DB 의 선택 이유와 대안 비교를 정리합니다. |
| [03. 시스템 구성도](docs/03-architecture.md) | 컴포넌트 구성도, 계층 책임, 요청 흐름과 예외 처리 전략을 정리합니다. |
| [04. DB 설계](docs/04-db-design.md) | ERD·테이블 정의서와 인덱스 설계 근거를 정리합니다. |
| [05. API 설계](docs/05-api-design.md) | 엔드포인트 목록, 공통 에러 포맷과 요청/응답 예시를 정리합니다. |
| [06. AI 도구 활용 내역](docs/06-ai-usage.md) | 어떤 도구로 무엇을 했고, AI 생성물 중 직접 수정한 부분과 이유를 기록합니다. |
| [07. 미완성 / 개선점](docs/07-limitations.md) | 현재 설계의 한계와 알려진 결함, 우선순위별 개선 계획을 정리합니다. |

> 각 문서는 구현·확정된 내용만 담습니다. 계획 단계의 상세본은 커밋하지 않으며, 기능이 확정되는 시점에 실측으로 검증한 내용만 옮깁니다.
