# Open Forms

Google Forms 와 유사한 설문/폼 서비스입니다. 제작자가 폼을 만들어 공개 링크로 배포하면 누구나 로그인 없이 응답할 수 있고, 제작자는 대시보드에서 응답을 집계·시각화합니다.

- **Backend**: Java 21 · Spring Boot 4.1.0 · Gradle 9.5.1 · PostgreSQL 16 · Spring Security(JWT) · Flyway
- **Frontend**: React · Vite · TypeScript · TanStack Query · Recharts
- 설계 문서: 아래 [설계 문서](#설계-문서) 섹션을 참고하십시오.

## 사전 요구 사항
- **Docker** (PostgreSQL 기동용)
- **JDK 21** — 없어도 됩니다. Gradle toolchain(foojay)이 자동으로 다운로드합니다.
- **Node.js 18+** (프론트엔드)

## 빠른 시작

### 1) 환경 변수 준비
```bash
cp .env.example .env
# 필요 시 값 수정 (특히 JWT_SECRET)
```

### 2) 데이터베이스 기동 (Docker)
```bash
docker-compose up -d        # PostgreSQL 16 기동
```

### 3) 백엔드 실행
```bash
cd open_forms_backend
./gradlew bootRun           # http://localhost:8080
```
- API 문서(Swagger UI): http://localhost:8080/swagger-ui.html
- Flyway 가 기동 시 스키마를 자동으로 마이그레이션합니다.

### 4) 프론트엔드 실행
```bash
cd open_forms_frontend
npm install
npm run dev                 # http://localhost:5173
```

## 테스트

```bash
cd open_forms_backend
./gradlew test              # H2 in-memory 사용 → Docker 불필요
```

## 프로젝트 구조
```
.
├── docs/                   # 설계 문서 (구현 완료분부터 순차 추가)
├── docker-compose.yml      # PostgreSQL
├── .env.example            # 환경 변수 예시
├── .dockerignore           # 배포 이미지 빌드 컨텍스트 제외 목록
├── open_forms_backend/     # Spring Boot  (+ Dockerfile, fly.toml)
└── open_forms_frontend/    # React + Vite (+ Dockerfile, fly.toml, nginx.conf)
```

## 배포

[Fly.io](https://fly.io) 에 **앱 두 개**로 배포하고, 데이터베이스는 [Neon](https://neon.tech)
(관리형 PostgreSQL)을 사용합니다.

```
[브라우저] ──https──▶ open-forms.fly.dev        (nginx, 256MB)
                        ├─ /                      → 정적 번들 (SPA fallback)
                        └─ /api, /swagger-ui, /v3/api-docs
                                │ Fly 사설망(6PN) — 공개 IP 없음
                                ▼
                       open-forms-api.internal   (JVM, 512MB)
                                │ TLS
                                ▼
                       Neon PostgreSQL
```

**앱을 왜 둘로 나눴는가** — 백엔드에는 CORS 설정이 없습니다. 이는 누락이 아니라 설계입니다.
개발 환경에서 `vite.config.ts` 의 프록시가 하던 역할을 배포 환경에서는 nginx 가 그대로 이어받아,
브라우저 입장에서는 언제나 **단일 출처**가 됩니다. 프론트엔드는 `VITE_API_BASE_URL` 없이 기본값
`/api` 로 동작하므로 번들에 백엔드 절대 URL 이 박히지 않고, 백엔드는 공개 IP 없이 사설망에서만
열립니다.

### 최초 설정

```bash
brew install flyctl && fly auth login

# fly launch 는 쓰지 않습니다 — 공개 IP 를 자동 할당해 백엔드가 인터넷에 노출됩니다.
fly apps create open-forms-api
fly apps create open-forms

fly secrets set -a open-forms-api \
  SPRING_DATASOURCE_URL='jdbc:postgresql://<host>.neon.tech/open_forms?sslmode=require' \
  SPRING_DATASOURCE_USERNAME='<user>' \
  SPRING_DATASOURCE_PASSWORD='<password>' \
  JWT_SECRET="$(openssl rand -base64 48)"
```

> Neon 은 Fly 리전(`nrt`)과 맞춰 **AWS ap-northeast-1** 에 만들고, JDBC URL 에
> `?sslmode=require` 를 반드시 붙입니다. 스키마는 첫 기동 때 Flyway 가 자동 적용합니다.

### 배포

두 Dockerfile 모두 **빌드 컨텍스트가 리포지터리 루트**이므로 루트에서 실행합니다
(`build.gradle` 의 `test` 태스크가 `../docs/05-api-design.md` 를 입력으로 선언하기 때문입니다).

```bash
fly deploy --config open_forms_backend/fly.toml    # 백엔드 먼저
fly deploy --config open_forms_frontend/fly.toml

fly ips list -a open-forms-api                     # 비어 있어야 정상(사설 전용)
fly logs   -a open-forms-api
```

## 주요 기능
- 회원가입/로그인 (JWT 인증)
- 폼/질문 CRUD, 발행(공개 slug 링크)
- 공개 링크로 익명 응답 제출
- 대시보드: 총 응답 수 / 완료율 / 일별 추이 / 질문별 분포 (차트)

## 설계 문서

설계 문서는 [`docs/`](docs/) 에 정리되어 있으며, 구현·확정된 문서부터 순차적으로 추가합니다.

| 문서 | 소개 |
|---|---|
| [01. 서비스 개요](docs/01-service-overview.md) | 무엇을 왜 만들었는지, 핵심 사용자 흐름과 구현 범위를 설명합니다. |
| [02. 기술 스택 선택 근거](docs/02-tech-stack.md) | 프론트엔드·백엔드·DB 의 선택 이유와 대안 비교를 정리합니다. |
| [06. AI 도구 활용 내역](docs/06-ai-usage.md) | 어떤 도구로 무엇을 했고, AI 생성물 중 직접 수정한 부분과 이유를 기록합니다. |
| [03. 시스템 구성도](docs/03-architecture.md) | 컴포넌트 구성도, 계층 책임, 요청 흐름과 예외 처리 전략을 정리합니다. |
| [04. DB 설계](docs/04-db-design.md) | ERD·테이블 정의서·인덱스 설계 근거 *(작성 중)* |
| [05. API 설계](docs/05-api-design.md) | 엔드포인트 목록과 요청/응답 예시 *(작성 중)* |
| [07. 미완성 / 개선점](docs/07-limitations.md) | 현재 한계와 개선 계획 *(작성 중)* |

> 03·04·05·07 은 섹션 구조만 잡힌 상태이며, 해당 기능이 구현·확정되는 시점에 내용을 채웁니다.
