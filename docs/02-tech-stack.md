# 02. 기술 스택 선택 근거

> 아래 버전은 실제 스캐폴딩/빌드로 검증된 값입니다.

## 스택 요약

| 계층 | 선택 | 버전 |
|---|---|---|
| 언어 | Java | 21 (LTS) |
| 백엔드 | Spring Boot | 4.1.0 |
| 빌드 | Gradle | 9.5.1 |
| DB (운영) | PostgreSQL | 16 (Docker) |
| DB (테스트) | H2 | in-memory, PostgreSQL 호환 |
| 마이그레이션 | Flyway | Boot BOM 관리 |
| 인증 | Spring Security + JWT (jjwt 0.13.0) | — |
| API 문서 | springdoc-openapi | 3.0.3 (Boot 4 호환) |
| 프론트 | React + Vite + TypeScript | 19.2 / 8.1 / 6.0 |
| 서버 상태 | TanStack Query v5 | — |
| 차트 | Recharts v3 | — |
| 스타일 | CSS 변수(디자인 토큰) + CSS Modules | — |

## 선택 근거

**Java 21** — 요건은 17 이상이나 21이 더 최신 LTS. Virtual Threads 정식화, Record Patterns 등 최신 언어 기능 활용.

**Spring Boot 4.1.0** — Spring Initializr 기준 현재 최신 stable(기본값). Boot 4.x는 스타터가 모듈화(`starter-webmvc`, 기능별 `*-test` 스타터)되었으며, Jackson 3(`tools.jackson`)을 기본으로 사용합니다.

**Gradle** — Kotlin DSL 간결성, 빌드 속도, foojay toolchain 자동 프로비저닝(JDK 21 미설치 환경에서도 `./gradlew` 빌드 재현 가능).

**PostgreSQL + H2** — 운영은 PostgreSQL로 인덱스·제약·집계 쿼리를 실제 RDB에서 시연. 테스트는 H2(PostgreSQL 호환 모드) + 동일 Flyway 스크립트로 Docker 없이 `./gradlew test` 통과.

**JWT (무상태) + 리프레시 토큰** — 공개 API와 인증 API가 공존하는 구조에서 세션 대신 무상태 JWT 채택. 액세스(1시간) + 리프레시(14일 DB 상태) 조합으로 갱신·무효화 지원.

**React + Vite + TanStack Query** — 요건 명시 스택. TanStack Query로 로딩/에러/캐시 상태를 선언적으로 처리. Recharts를 대시보드 페이지에만 lazy 로딩(초기 번들 372KB 유지).

**CORS 없음, Dev Proxy** — Vite dev 프록시(`/api` → `:8080`)로 브라우저 입장에서 동일 출처. 백엔드 변경 없이 인증 헤더 정책 단순화.
