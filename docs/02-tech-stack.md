# 02. 기술 스택 선택 근거

> 아래 버전은 실제 스캐폴딩/빌드로 검증된 값입니다 (문서-코드 정합 유지).

## 요약

| 계층 | 선택 | 버전 |
|---|---|---|
| 언어 | Java | **21 (LTS)** |
| 백엔드 프레임워크 | Spring Boot | **4.1.0** |
| 빌드 도구 | Gradle | **9.5.1** (wrapper) |
| DB (운영/개발) | PostgreSQL | 16 (Docker) |
| DB (테스트) | H2 | in-memory, PostgreSQL 호환 모드 |
| 마이그레이션 | Flyway | Boot BOM 관리 |
| 인증 | Spring Security + JWT (jjwt) | Boot BOM / jjwt 0.13.0 |
| API 문서 | springdoc-openapi | 3.0.3 (Boot 4 호환) |
| 프론트 프레임워크 | React + Vite + TypeScript | React 19.2 / Vite 8.1 / TS 6.0 |
| 라우팅 | react-router-dom | v7 |
| 서버 상태 | TanStack Query (React Query) | v5 |
| 인증 상태 | React Context API | 내장 |
| 차트 | Recharts | v3 |
| HTTP 클라이언트 | axios | v1 |

> Spring Boot BOM 이 관리하는 전이 의존성(Hibernate, Jackson, HikariCP 등)의 버전은 `spring-boot-dependencies:4.1.0` 에 위임합니다.

## 백엔드: Java 21 + Spring Boot 4.1.0

**Java 21 을 고른 이유**
- 요건은 "Java 17 이상" 입니다. 17 도 LTS 이지만 **21 이 더 최신 LTS** 라서 장기 지원 근거가 더 명확합니다.
- **Virtual Threads 정식화, Record Patterns, switch 패턴 매칭**을 DTO/집계 로직에 활용하여 코드를 간결하게 유지합니다.
- 대안 비교: Java 17(구 LTS, 최신 언어 기능 부재), Java 25/26(비-LTS, 운영 리스크) 과 비교했을 때 **21 이 최신 LTS 균형점**입니다.

**Spring Boot 4.1.0 을 고른 이유**
- Spring Initializr 기준 현재 **최신 stable**(기본값)이며 Java 21 을 공식 지원합니다.
- 대안 비교: Spring Boot 3.x(직전 세대), 4.0.x(직전 마이너) 와 비교했을 때 **4.1.0 이 최신 stable** 입니다.
- 참고: Boot 4.x 는 스타터가 모듈화되었습니다 — `spring-boot-starter-web` 은 `spring-boot-starter-webmvc` 로, `spring-boot-starter-test` 는 기능별 `*-test` 스타터로 분리되었습니다. `build.gradle` 에 이 구조가 그대로 반영되어 있습니다.

**Gradle 을 고른 이유 (vs Maven)**
- 간결한 Kotlin/Groovy DSL, 빌드 속도, `toolchain` 자동 프로비저닝을 이유로 선택했습니다.
- **foojay-resolver-convention 1.0.0** 을 `settings.gradle` 에 추가하여, 로컬에 JDK 21 이 없어도 Gradle 이 자동으로 다운로드해 빌드/테스트가 재현되도록 했습니다(실행 가능성 확보).
  - (0.9.0 은 Gradle 9.5 에서 `IBM_SEMERU` NoSuchFieldError 가 발생하여 1.0.0 으로 고정했습니다.)

## DB: PostgreSQL(운영) + H2(테스트)

**PostgreSQL 을 운영/개발 DB 로 고른 이유**
- 인덱스(복합/부분), 제약조건, 시계열 집계 쿼리를 **실제 운영급 RDB 에서 시연**할 수 있어 DB 설계 역량이 제대로 드러납니다.
- `docker-compose up -d` 한 줄로 기동되어 "최소 설정 통합 실행" 요건을 충족합니다.
- 대안 비교: H2 만 사용하면 일부 인덱스/제약 동작이 운영 DB 와 달라 "왜 이 인덱스인가"의 설득력이 약합니다. MySQL 도 선택 가능하지만 기능/친숙도 면에서 PostgreSQL 을 선호했습니다.

**테스트는 H2 in-memory(PostgreSQL 호환 모드)**
- `./gradlew test` 가 **Docker 없이도** 통과하도록 보장하여 테스트 실행 가능성을 확보합니다.
- H2 를 `MODE=PostgreSQL` 로 띄우고 **동일한 Flyway 스크립트**를 실행하여, 테스트 스키마와 운영 스키마를 일치시킵니다(정합성). PostgreSQL 전용 문법을 피하고 이식 가능한 DDL 만 사용합니다.

## 인증: Spring Security + JWT

- 제작자 API 는 인증이 필요하고 공개 응답 API 는 익명이므로, **stateless JWT** 가 세션보다 적합합니다(서버 상태 없음, 확장 용이).
- 비밀번호는 **BCrypt 로 해싱**하여 저장합니다(평문 저장 금지).
- 인증 실패는 401, 권한 부족은 403 으로 필터/핸들러에서 구분하여 반환합니다.

## 프론트: React + Vite + TypeScript

- 요건이 명시한 스택(React)을 사용합니다. **Vite** 로 빠른 개발 서버/빌드를, **TypeScript** 로 API 응답 타입 안정성을 확보합니다.
- **TanStack Query**: 서버 상태(로딩/에러/캐시/리페치)를 선언적으로 처리하여 로딩/에러/빈 상태 처리를 일관되게 유지합니다.
- **Context API**: 인증 토큰/사용자 정보를 전역으로 관리합니다(외부 상태 라이브러리는 과합니다).
- **Recharts**: 대시보드 집계를 시각화합니다(일별 추이 라인, 질문별 분포 바/파이).

## 관련 문서
- [01. 서비스 개요](01-service-overview.md)
- [06. AI 도구 활용 내역](06-ai-usage.md)
