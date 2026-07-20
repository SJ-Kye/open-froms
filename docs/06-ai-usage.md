# 06. AI 도구 활용 내역

## 사용 도구

| 도구 | 용도 |
|---|---|
| **Claude Code (Opus)** | 설계 문서 작성, 스캐폴딩, 코드 생성/리팩터링, 디버깅, 테스트 작성 |

## 개발 원칙

1. **설계 문서 우선**: 코드보다 설계를 먼저 확정하고 AI에게 그 설계에 맞게 구현 지시
2. **하이브리드 TDD**: 도메인/서비스 규칙은 실패 테스트(red) 먼저 작성 후 구현(green)
3. **AI 생성물 무비판 수용 금지**: 생성 코드를 검토해 버그·설계 위반·불필요 코드를 직접 수정하고 이유를 아래 로그에 기록

## AI 생성물 수정 이력

| 분류 | 내용 | 수정 이유 |
|---|---|---|
| **버전 교정** | AI 초안 Spring Boot 3.x → 4.1.0, Java 17 → 21 | Spring Initializr 실측치가 4.1.0이었고, 최신 LTS 선택 |
| **재현성** | foojay toolchain 0.9.0 → 1.0.0 추가 | 0.9.0이 Gradle 9.5에서 IBM_SEMERU NoSuchFieldError 발생. JDK 21 자동 프로비저닝으로 클린 환경 재현성 확보 |
| **호환성** | springdoc 버전 미지정 → 3.0.3 고정 | springdoc 2.x는 Spring Boot 3 대상. Maven Central에서 Boot 4(Framework 7) 호환 라인 확인 후 고정 |
| **버그 수정** | `answers` FK에 ON DELETE CASCADE 누락 | AI 생성 스키마에서 `question_id`·`option_id`에 CASCADE 누락. 실제 PostgreSQL에서 폼 삭제 시 FK 위반 실측 재현 후 수정 |
| **버그 수정** | Boot 4 = Jackson 3 (`tools.jackson`) | `ObjectMapper` 주입 시 `NoSuchBeanDefinitionException`. `com.fasterxml.jackson` → `tools.jackson.databind` import 교체 |
| **보안** | `/api/auth/**` 전체 permitAll → 경로 한정 | `register`·`login`만 허용해야 하는데 `/me`까지 미인증으로 통과, `Authentication` null → 500. 통합 테스트로 발견 |
| **설계 판단** | 에러 래퍼+traceId 구조 결정 | 성공은 bare 본문 유지, 에러만 래퍼(`code`/`message`/`traceId`/`fieldErrors`). `X-Trace-Id` 헤더로 추적성 확보 |
| **설계 판단** | 로그인 실패 401 통일 | 이메일 없음·비밀번호 불일치를 `INVALID_CREDENTIALS` 하나로. 두 경우를 구분하면 계정 열거(enumeration) 가능 |
| **설계 판단** | `users`만 UUID(v7) 혼합 전략 | 전체 UUID는 인덱스 지역성 비용. 가입자 수 추정이 가장 민감한 `users`에만 적용 |
| **설계 판단** | `api_call_logs` principal을 FK 대신 VARCHAR | 매 요청 DB 조회 없애고 `common` 모듈이 `user` 모듈에 의존하지 않도록 경계 유지 |
| **버그 수정** | 재사용 탐지 폐기가 롤백됨 | `REQUIRES_NEW` 없이 예외 throw 시 UPDATE가 함께 롤백. 단위(mock)·기존 통합(트랜잭션 공유) 테스트 모두 놓침 → `REQUIRES_NEW` 분리 + 비트랜잭션 테스트 신설 |
| **버그 수정** | 집계 별칭 H2 예약어 충돌 | `day`·`value` 별칭이 H2 예약어 → 네이티브 쿼리를 JPQL로 전환해 방언 차이를 Hibernate가 흡수 |
| **버그 수정** | 정합 테스트 UP-TO-DATE 문제 | `docs/05`가 Gradle 입력이 아니어서 문서만 바꿔도 테스트 건너뜀. `build.gradle`에 문서를 입력으로 선언 후 3가지 훼손 모두 실패 확인 |
| **설계 판단** | 저장을 카드 단위 → 일괄 전환 | API는 질문 단위이나 UX상 매번 저장이 불편. 사전 전량 검증(`draftProblem`) + 부분 실패 시 항목별 결과 수집으로 대체 |
| **버그 수정** | 빠른 문항 추가가 유형만 교체 | 미저장 카드 1개 제한 로직이 신규 카드 대신 기존 카드 유형을 교체. position을 화면 자리로 계산해 카드 배열화로 해결 |
| **문서 정합** | 서버 실기동 후 응답 캡처로 예시 작성 | `createdAt`에 `Z` 없음, `traceId` 누락, 목록 기본값 미기재 등 코드 읽기로는 못 잡는 불일치 3건 발견 |

## 핵심 교훈

- **문서의 주장을 실측으로 교차검증**: AI 생성 스키마의 "폼 삭제 시 연쇄 삭제" 보장이 실제로는 실패했음. PostgreSQL에 실데이터 삽입 후 DELETE로 재현해 발견.
- **단위 테스트가 구조적으로 놓치는 지점**: 트랜잭션 경계, 타입 방언 차이, 비동기 이력 캡처 등은 통합 테스트나 런타임 실측으로만 확인 가능.
- **라이브러리 동작은 추정 말고 실측**: springdoc 합성 어노테이션이 메서드 단에서만 동작하는지, `/v3/api-docs` 직접 확인 후 적용.
