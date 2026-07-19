# 06. AI 도구 활용 내역

> 이 문서는 개발 진행에 따라 **실시간으로 갱신됩니다**. "AI 를 썼다" 가 아니라 *어떤 도구로 / 무엇을 / AI 생성물의 무엇을 왜 고쳤는지* 를 구체적으로 기록합니다.

## 사용 도구
| 도구 | 용도 |
|---|---|
| **Claude Code (Opus)** | 설계 문서 작성, 스캐폴딩, 코드 생성/리팩토링, 디버깅, 테스트 작성 |

## 개발 원칙 (AI 협업 방식)
1. **설계 문서 우선**: 코드보다 설계를 먼저 확정하고, AI 에게 그 설계에 맞춰 구현하도록 지시합니다. 확정 전 상세 설계는 `dev-docs/`(로컬 참조)에 두고, 구현·확정된 부분만 `docs/`(커밋 대상)로 옮깁니다.
2. **하이브리드 TDD**: 도메인/서비스 규칙은 실패 테스트를 먼저 작성(red)한 뒤 구현(green)하고 리팩토링합니다. 커밋 히스토리에 red-green 이 남습니다.
3. **AI 생성물 무비판 수용 금지**: 생성된 코드를 검토하여 버그/설계 위반/불필요 코드를 직접 수정하고, 수정 이유를 아래 로그에 남깁니다.

## 작업 로그

### [Phase 0] 설계 & 스캐폴딩

**T1. 요건 분석 및 설계/프로세스 수립 — Claude Code**
- 평가표 10개 항목을 도메인/API/DB/프로세스 결정으로 매핑했습니다. 각 항목이 자연스럽게 드러나는 도메인(공개 익명 응답 + 제작자 대시보드)을 선정했습니다.

**T2. 기술 스택 검증 — Claude Code**
- AI 초안은 "Spring Boot 3.x" 였으나 **직접 수정하여 Spring Boot 4.1.0 으로 확정**했습니다.
  - **이유**: Spring Initializr 메타데이터를 실제 조회한 결과 현재 stable 기본값이 4.1.0 이었습니다. 오래된 지식(3.x)을 실측값으로 교정했습니다(문서-코드 정합).
- Java 17 을 **21(LTS)** 로 변경했습니다. 요건의 하한이 아니라 최신 LTS 를 채택했습니다.

**T3. 백엔드 스캐폴딩 — Claude Code (Spring Initializr)**
- Boot 4.x 의 모듈화된 스타터 구조(`starter-webmvc`, 분리된 `*-test` 스타터)를 확인했습니다.
- **직접 수정 ①**: `settings.gradle` 에 foojay toolchain resolver 를 추가하여, 로컬에 JDK 21 이 없어도 자동 프로비저닝되도록 했습니다. **이유**: 실행 재현성(평가 9번)입니다. JDK 21 미설치 환경에서도 `./gradlew` 빌드가 되도록 했습니다.
- **직접 수정 ②**: foojay 버전을 `0.9.0 → 1.0.0` 으로 올렸습니다. **이유**: 0.9.0 이 Gradle 9.5 에서 `IBM_SEMERU` `NoSuchFieldError` 로 빌드에 실패했습니다(버그). 스택트레이스로 원인을 확인한 뒤 호환 버전으로 교정했습니다.
- 검증: `./gradlew compileJava`, `./gradlew test`(컨텍스트 로드)가 통과했습니다.

**T4. 설계 문서 작성 — Claude Code**
- 설계 문서 7종(서비스 개요·기술 스택·아키텍처·DB·API·AI 활용·한계)을 작성했습니다. 실제 스캐폴딩으로 검증된 버전만 문서에 기입했습니다(정합성).

**T5. 백엔드 의존성·설정·초기 스키마 — Claude Code**
- springdoc 호환성을 확인했습니다. AI 초안은 버전 미지정이었으나 **직접 확인 후 3.0.3 으로 고정**했습니다. **이유**: springdoc 2.x 는 Spring Boot 3(Framework 6) 대상이라 Boot 4 에서 동작을 보장할 수 없습니다. Maven Central 조회로 Boot 4/Framework 7 호환 라인(3.0.x)을 확인했습니다.
- `application.yml`(운영: PostgreSQL)과 `src/test/resources/application.yml`(테스트: H2 PostgreSQL 호환 모드)로 프로파일을 분리했습니다. **이유**: `./gradlew test` 가 Docker 없이 통과하도록 하기 위함입니다. 운영과 동일한 Flyway 스크립트를 실행하여 스키마 정합을 유지합니다.
- `V1__init.sql` 을 작성했습니다 — DB 설계 문서의 테이블 정의서와 1:1 로 일치시켰습니다. `ddl-auto=validate` 로 스키마-엔티티 불일치를 기동 시 검출합니다.
- 검증: `./gradlew test` 가 통과했습니다(Flyway 가 H2 에 마이그레이션 성공).

**T6. 프론트엔드 스캐폴딩 — Claude Code (Vite)**
- `create-vite react-ts` 로 생성한 뒤 React Query·axios·react-router-dom·recharts 를 설치했습니다.
- **직접 수정**: 기술 스택 문서의 프론트 버전을 추정치(React 18/Vite 5/Recharts v2)에서 **실측치(React 19.2/Vite 8.1/Recharts v3/Router v7/TS 6.0)로 교정**했습니다. **이유**: 문서-코드 정합(평가 10번)입니다 — 설치된 실제 버전과 문서를 일치시켰습니다.

### [Phase 0.5] 문서 재구성 & 어체 정리

**T7. 문서 구조 정비 — Claude Code**
- 설계 문서를 2단 구조로 나눴습니다. 확정 전 상세본은 `dev-docs/`(로컬 전용, `.git/info/exclude` 로 커밋 제외)에 두고, 구현·확정된 문서만 `docs/`(커밋 대상)에 둡니다. **이유**: 커밋된 저장소에 "계획만 하고 구현하지 않은 문서" 가 남지 않도록 하여 문서-코드 정합(평가 10번)을 강화하기 위함입니다.
- 안정 문서인 서비스 개요·기술 스택 근거와, 실시간 갱신 대상인 본 AI 활용 내역을 `docs/` 로 승격했습니다. 아키텍처·DB·API·한계 문서는 해당 구현이 완료되는 시점에 승격합니다.
- 전체 문서의 어체를 "~입니다" 정중체로 통일했습니다.

### [Phase 0.6] DB 설계 정련 & V1 스키마 확정

이 단계는 사용자가 요구사항을 질문-응답으로 확정하고 Claude 가 구현·**실측 검증**하는 방식으로 진행했습니다. 설계 판단은 사용자가 내렸고, Claude 는 그 판단을 스키마로 옮긴 뒤 실제 DB 로 검증하여 문서의 주장(예: "폼 삭제 시 연쇄 삭제")이 실제로 성립하는지 확인했습니다.

**T8. 감사 컬럼 도입 (선택적 적용) — Claude Code**
- created/updated_by·at 4종을 기본으로 하되 **모든 테이블에 붙이지 않았습니다.** 적용 기준을 "그 컬럼이 새로운 정보를 담는가" 로 정하고, users·forms 는 4종, responses 는 불변이라 created_* 2종, 상위에 종속되는 questions·answers 등은 제외했습니다. **이유**: 종속 테이블의 감사값은 상위와 항상 같아 중복이며, 최다 행 테이블(answers)에 4컬럼을 더하면 저장 비용만 늘어납니다.
- 생성자/변경자를 FK(users.id) 가 아닌 VARCHAR('ANONYMOUS' 가능)로 두었습니다. **이유**: 익명 응답을 NULL 없이 표현하고, 계정 삭제가 감사 기록을 제약하지 않도록 하기 위함입니다.

**T9. 질문 타입 9종 확장 — Claude Code**
- 기존 5종에 DROPDOWN·NUMBER·DATE·TIME 을 더했습니다. **직접 판단**: 사용자가 "체크박스" 추가를 요청했으나, 체크박스는 기존 MULTIPLE_CHOICE(다중선택)와 저장 구조가 동일하므로 **신규 타입으로 만들지 않고** 그 점을 문서에 명시했습니다. **이유**: 저장 구조가 같은 타입이 둘이면 집계 로직이 중복됩니다.
- RATING/NUMBER 의 척도를 정의할 곳이 없던 문제를 questions.min_value·max_value 로 해결했습니다.

**T10. 응답 저장 모델 단순화 (answer_options 제거) — Claude Code**
- 사용자 제안("answer_options 빼도 될 것 같은데")을 받아 조인 테이블을 제거하고 answers.option_id 로 통합했습니다. **이유**: 순수 조인 테이블이라 가장 흔한 택1형이 값 하나를 저장하려고 행 두 개를 쓰고, 대시보드 핵심 질의인 선택지별 집계에 조인이 끼었습니다. answers 를 "값 1건"으로 재정의하니 N:M 이 1:N 두 개로 분해되어 3NF 를 유지하면서 조인이 사라졌습니다.

**T11. 폼 삭제 연쇄 삭제 버그 발견·수정 — Claude Code (실측 검증)**
- **버그 수정**: answers 의 FK 중 response_id 에만 ON DELETE CASCADE 가 있어, 실제 PostgreSQL 에서 폼 삭제 시 `fk_answers_question` 위반으로 **삭제가 실패**했습니다. question_id·option_id 에도 CASCADE 를 부여해 고쳤습니다.
- **주목할 점**: 이 버그는 **AI 가 생성한 원본 스키마(커밋 ac0668b)에 이미 존재**했고, 문서는 "폼 삭제 시 연쇄 삭제"를 보장한다고 적고 있었습니다. 일회용 PostgreSQL 16 컨테이너에 실데이터를 넣고 `DELETE FROM forms` 를 실행하는 회귀 시나리오로 재현하여 발견했습니다. **교훈**: 문서의 주장을 실측으로 교차검증하지 않으면 AI 생성물의 결함이 그대로 남습니다.

**T12. users.id UUID 전환 (혼합 전략) — Claude Code**
- 사용자 요청으로 users.id 를 UUID(UUIDv7, 앱 생성)로, forms.user_id 를 동일 타입으로 변경했습니다. 나머지 테이블은 BIGINT 를 유지하는 혼합 전략입니다.
- **직접 판단**: 앞서 "전부 UUID" 안을 검토했을 때는 인덱스 지역성 비용을 근거로 전량 기각했으나, **users 한 곳** 은 근거가 다름을 구분했습니다. 순차 PK 가 노출하는 가장 민감한 정보가 가입자 수이고 users 는 저볼륨이라, "이득이 가장 크고 비용이 가장 작은 한 곳" 으로 판단해 여기에만 적용했습니다. **이유**: UUID 를 인가 대체재로 오용하지 않되(IDOR 은 소유권 검사로 막음), 인가로 막을 수 없는 정보 누출은 표적 지점에서 차단하기 위함입니다.
- **검증**: H2·PostgreSQL 16 양쪽에서 UUID PK/FK 동작과, forms.user_id 에 정수 삽입이 타입 불일치로 거부되는 것을 확인했습니다.

**T13. 설계 문서(04) 본문 작성 — Claude Code**
- 사용자 지시로 docs/04 본문을 채우고 V1__init.sql 과 컬럼·인덱스 일치를 스크립트로 대조했습니다. **주의**: 통상 docs/ 설계 본문은 사용자가 직접 작성하는 것이 본 프로젝트 규율이며, 이 초안은 사용자 검토·재작성 대상입니다.

### [Phase 1] 백엔드 공통 인프라 (에러 처리·traceId·API 이력·Security/JWT) — Claude Code

패키지 구조를 **기능별+계층 하위 / 모듈러 모놀리스**로 재편(사용자 선택)한 뒤 공통 인프라를 구축했습니다. 규율은 `common ← user ← form ← response` 단방향 의존이며, 이 경계가 아래 설계 판단들의 근거가 됩니다.

**T14. 응답/추적 형식 결정 — 사용자 판단 + Claude 구현**
- 사용자 요청(공통 응답·traceId·API 호출 이력)을 받아, "에러만 래퍼 + traceId 는 헤더" 로 확정했습니다. **이유**: 성공 응답은 설계 문서(05)대로 bare 본문을 유지해 문서 변경을 최소화하고, `X-Trace-Id` 헤더 + 에러 본문 `traceId` 필드로 추적성을 확보했습니다.
- API 호출 이력(`api_call_logs`, V2)은 **메타데이터만 비동기 저장**하고 본문·비밀번호·JWT 는 저장하지 않습니다. 주체 컬럼을 FK(UUID) 대신 `principal VARCHAR` 로 둔 것은 **직접 판단**입니다. **이유**: 매 요청 email→UUID 조회를 없애고, `common` 이 `user` 모듈에 의존하지 않도록 경계를 지키기 위함입니다(감사 `created_by` 와 동일 소스).

**T15. Boot 4 = Jackson 3 발견·수정 — Claude Code (버그 수정)**
- 필터단 401/403 직렬화에 `com.fasterxml.jackson`(Jackson 2) `ObjectMapper` 를 주입했다가 `NoSuchBeanDefinitionException` 으로 컨텍스트 로드가 실패했습니다. **원인**: Spring Boot 4 는 **Jackson 3(`tools.jackson`)** 을 기본 빈으로 씁니다. import 를 `tools.jackson.databind.*` 로 교체해 해결했습니다.

**T16. 슬라이스 테스트 함정 회피 — Claude Code (설계 판단)**
- `@WebMvcTest` 슬라이스가 `WebConfig→인터셉터→라이터` 빈을 끌어와 로딩에 실패했습니다. `GlobalExceptionHandler` 검증은 애플리케이션 컨텍스트 없이 `standaloneSetup` 으로 전환해 advice 만 독립 검증했습니다. **이유**: 예외 변환 규칙은 컨텍스트 무관하므로 슬라이스 의존을 없애는 편이 견고합니다.

**T17. 모듈 경계로 인한 인증 범위 분리 — Claude Code (설계 판단)**
- Security/JWT 를 `common` 에 두되 **무상태 검증만**(토큰 subject=이메일) 담고, `UserDetailsService`·비밀번호 대조는 Phase 2(`user` 모듈)로 미뤘습니다. **이유**: `common` 이 `user` 에 의존하면 모듈러 모놀리스 경계가 깨지므로, DB 조회 없는 서명 검증만 공통에 남겼습니다.

### [Phase 2] 인증 (회원가입·로그인·본인 조회) — Claude Code (하이브리드 TDD)

서비스 규칙은 실패 테스트 먼저(이메일 중복 409·잘못된 자격 401 → red→green), 컨트롤러는 구현 후 통합 테스트로 검증했습니다.

**T18. UserDetailsService 미도입 — Claude Code (설계 판단)**
- 무상태 JWT + 커스텀 로그인 엔드포인트에서는 Spring 의 `AuthenticationManager`/`UserDetailsService` 를 쓰지 않고, `AuthService` 에서 `PasswordEncoder.matches` 로 직접 대조했습니다. **이유**: 매 요청 인증은 JWT 필터가 이미 처리하며, 직접 대조가 더 단순하고 `common ← user` 경계도 깔끔합니다.
- 로그인 실패 시 **이메일 미존재와 비밀번호 불일치를 동일한 401(`INVALID_CREDENTIALS`)** 로 통일했습니다. **이유**: 계정 존재 여부가 노출되면 사용자 열거(enumeration) 단서가 됩니다.
- 컨트롤러 흐름의 401 은 필터단(`AuthenticationEntryPoint`) 이 아니라 `UnauthorizedException`(`BusinessException` 하위)으로 던져 `GlobalExceptionHandler` 를 거칩니다. **이유**: 서비스에서 `AuthenticationException` 을 던지면 advice 폴백(500)으로 빠지므로, 상태 캐리어 예외로 통일했습니다.

**T19. `/api/auth/me` 인증 누락 버그 발견·수정 — Claude Code (버그 수정, 통합 테스트로 발견)**
- Phase 1 `SecurityConfig` 가 `/api/auth/**` 를 통째로 permitAll 하고 있어, 인증이 필요한 `/api/auth/me` 까지 미인증으로 통과해 `Authentication` 이 null → **500** 이 났습니다. permitAll 대상을 `register`·`login` 두 경로로 한정해 `/me` 가 `authenticated()` 에 걸리도록 고쳤습니다(`fix:` 커밋으로 흔적). **주목**: 실제 보안 체인을 포함한 `@SpringBootTest` 통합 테스트가 아니었으면 슬라이스로는 잡히지 않았을 결함입니다.

**T20. 런타임 실측 교차검증 — Claude Code**
- 일회용 PostgreSQL 16 컨테이너로 register→login→`/me` 전 흐름을 curl 로 재현하고 DB 를 직접 조회해, (1) `users.password_hash` 가 BCrypt(`$2a$`)로 저장(원문 아님), (2) 인증 요청의 `api_call_logs.principal` 이 **실제 로그인 이메일**로 기록됨을 확인했습니다. **교훈**: 비동기 이력 기록의 주체 캡처는 단위 테스트로 덮이지 않아 실측이 필요합니다.

<!-- 이후 Phase 3~9 진행 시 여기에 계속 추가 -->

## 수정 이유 분류 (누적)
| 분류 | 사례 |
|---|---|
| 버그 수정 | foojay 0.9.0 `IBM_SEMERU` 오류 → 1.0.0; answers 연쇄 삭제 누락(FK CASCADE) → 실측 재현 후 수정; Boot 4 Jackson 3 `ObjectMapper` 오주입 → `tools.jackson` 교체; `/api/auth/me` permitAll 과다 → 인증 필요로 축소(통합 테스트로 발견) |
| 최신성/정합 | Boot 3.x(구 지식) → 실측 4.1.0, 프론트 버전 추정치 → 실측치 |
| 재현성 개선 | foojay toolchain 자동 프로비저닝 추가 |
| 호환성 확인 | springdoc 3.0.3(Boot 4 호환 라인) 고정 |
| 설계 판단 | 감사 컬럼 선택 적용; 체크박스=MULTIPLE_CHOICE 재사용; answer_options 제거; users 만 UUID 혼합 전략; 에러만 래퍼+traceId 헤더; api_call_logs 주체 VARCHAR(모듈 경계); Security 무상태만 common; 로그인 실패 401 통일(계정 열거 차단); UserDetailsService 미도입 |
