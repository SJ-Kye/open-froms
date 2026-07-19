# 05. API 설계

- Base URL: `http://localhost:8080`
- 모든 요청/응답 본문: `application/json`
- 인증: `Authorization: Bearer <JWT>` (인증이 필요한 API 에만)
- 목록 API: `page`(0부터) · `size` · `sort` 쿼리를 지원합니다.
- Swagger UI: `http://localhost:8080/swagger-ui.html` (OpenAPI 문서: `/v3/api-docs`)

> 이 문서의 요청/응답 예시는 **실제로 실행하여 캡처한 값**입니다(로컬 PostgreSQL 기동 → curl 호출). 필드 구성·상태 코드·날짜 표기 모두 실행 결과 그대로이며, 추정으로 적은 값은 없습니다.
>
> 이 문서는 **현재 구현이 완료된 엔드포인트만** 담습니다. 응답 조회·대시보드 집계 API 는 설계가 확정되어 있으나 아직 구현 전이라 의도적으로 넣지 않았으며, 구현 시점에 이 문서로 승격합니다(문서-코드 정합 유지).

## 엔드포인트 목록

### 인증 (Auth)
| Method | Path | 설명 | 인증 | 성공 | 주요 실패 |
|---|---|---|---|---|---|
| POST | `/api/auth/register` | 회원가입 | 🔓 | 201 | 400(검증) / 409(이메일 중복) |
| POST | `/api/auth/login` | 로그인, JWT 발급 | 🔓 | 200 | 400 / 401(자격 실패) |
| GET | `/api/auth/me` | 내 정보 조회 | 🔒 | 200 | 401 |

로그인 실패는 **이메일이 없는 경우와 비밀번호가 틀린 경우를 구분하지 않고** 동일한 `401 INVALID_CREDENTIALS` 로 답합니다. 두 경우를 다르게 답하면 로그인 폼이 곧 "가입 여부 조회 API" 가 되어 계정 열거(account enumeration)가 가능해지기 때문입니다.

### 폼 (Forms) — 소유자만
| Method | Path | 설명 | 인증 | 성공 | 주요 실패 |
|---|---|---|---|---|---|
| GET | `/api/forms` | 내 폼 목록 (페이지네이션·상태 필터·정렬) | 🔒 | 200 | 401 |
| POST | `/api/forms` | 폼 생성 | 🔒 | 201 | 400 / 401 |
| GET | `/api/forms/{id}` | 폼 상세 (질문·선택지 포함) | 🔒 | 200 | 401 / 403 / 404 |
| PUT | `/api/forms/{id}` | 폼 수정 (제목·설명) | 🔒 | 200 | 400 / 401 / 403 / 404 |
| PATCH | `/api/forms/{id}/status` | 상태 전이 (발행/종료) | 🔒 | 200 | 400 / 401 / 403 / 404 / 409 |
| DELETE | `/api/forms/{id}` | 폼 삭제 | 🔒 | 204 | 401 / 403 / 404 |

**404 와 403 의 순서**를 규칙으로 고정했습니다. 폼이 존재하지 않으면 `404 FORM_NOT_FOUND`, 존재하지만 내 것이 아니면 `403 ACCESS_DENIED` 입니다. 남의 폼을 404 로 감추는 선택지도 있었지만, 그러면 소유자 본인의 오탈자 요청과 타인의 접근 시도가 같은 응답이 되어 클라이언트가 사용자에게 정확한 안내를 할 수 없습니다. 폼 `id` 는 인증을 통과한 뒤에만 대입할 수 있고 공개 경로는 `slug` 를 쓰므로(04 "식별자(PK) 전략"), 존재 사실 자체를 감출 실익이 크지 않다고 판단했습니다.

**상태 전이는 `DRAFT → PUBLISHED → CLOSED` 단방향**입니다. 되돌리는 전이(`PUBLISHED → DRAFT`)나 건너뛰는 전이는 `409 INVALID_STATUS_TRANSITION` 으로 거부합니다. 발행을 되돌릴 수 있게 하면 이미 배포된 공개 링크가 갑자기 404 가 되고, 수집된 응답과 이후 편집된 질문이 어긋나기 때문입니다.

### 질문 (Questions) — 소유자만, 폼 하위 리소스
| Method | Path | 설명 | 인증 | 성공 | 주요 실패 |
|---|---|---|---|---|---|
| POST | `/api/forms/{formId}/questions` | 질문 추가 | 🔒 | 201 | 400 / 401 / 403 / 404 / 409 |
| PUT | `/api/forms/{formId}/questions/{id}` | 질문 수정 | 🔒 | 200 | 400 / 401 / 403 / 404 / 409 |
| DELETE | `/api/forms/{formId}/questions/{id}` | 질문 삭제 | 🔒 | 204 | 401 / 403 / 404 / 409 |

**질문 편집은 `DRAFT` 폼에서만 가능합니다.** 이미 발행(`PUBLISHED`)되었거나 종료(`CLOSED`)된 폼의 질문을 바꾸면 이미 수집된 응답과 어긋나므로 — 예를 들어 선택지를 지우면 그 선택지를 고른 응답이 의미를 잃고, 질문을 추가하면 기존 응답이 모두 "미응답" 이 됩니다 — 세 엔드포인트 모두 `409 FORM_NOT_EDITABLE` 로 막습니다. 이는 04 의 **응답 불변 정책**과 짝을 이루는 규칙입니다. 응답을 고칠 수 없게 했다면, 응답의 전제인 질문도 고칠 수 없어야 집계 정합이 유지됩니다.

**400 은 두 갈래입니다.** 필드 형식 위반(제목 공백, `position` 누락 등)은 `@Valid` 가 잡아 `VALIDATION_FAILED` 와 `fieldErrors` 로 답하고, **질문 타입에 따라 달라지는 규칙**은 서비스가 전용 코드로 답합니다 — 선택형인데 선택지가 2개 미만이면 `OPTIONS_REQUIRED`, `RATING`·`NUMBER` 의 `minValue > maxValue` 면 `INVALID_VALUE_RANGE` 입니다. 타입별 규칙은 필드 하나만 봐서는 판정할 수 없어(타입과 선택지를 함께 봐야 합니다) 어노테이션 검증으로 표현하기 어렵기 때문입니다.

404 도 두 갈래로 구분합니다. 폼이 없으면 `FORM_NOT_FOUND`, 폼은 있지만 그 폼에 속하지 않은 질문 `id` 면 `QUESTION_NOT_FOUND` 입니다. 중첩 경로에서는 상위·하위 중 무엇이 잘못되었는지 클라이언트가 알 수 있어야 합니다.

### 공개 폼 (Public) — 인증 불필요
| Method | Path | 설명 | 인증 | 성공 | 주요 실패 |
|---|---|---|---|---|---|
| GET | `/api/public/forms/{slug}` | 공개 폼 조회(응답 화면용) | 🔓 | 200 | 404(없음/미발행) |
| POST | `/api/public/forms/{slug}/responses` | 응답 제출 | 🔓 | 201 | 400(검증/필수 누락) / 404 / 409(종료됨) |

**조회의 기준은 발행 여부입니다.** `PUBLISHED` 와 `CLOSED` 는 200 으로 열리고, 아직 발행되지 않은 `DRAFT` 는 **존재하지 않는 slug 와 똑같은 404** 입니다. 미발행을 다른 코드로 구분하면 slug 를 넣어보는 것만으로 "준비 중인 폼이 있다"는 사실이 드러나기 때문입니다. 인증이 없는 경로에서는 제작자 API 의 "존재 확인 후 소유권 확인" 규칙을 그대로 쓸 수 없으므로, 같은 취지를 **발행 여부**로 옮겨 적용했습니다.

종료된 폼까지 조회를 허용하는 이유는 응답 화면이 `status` 를 보고 입력 폼 대신 "종료된 설문입니다" 를 안내할 수 있어야 하기 때문입니다. 종료 폼을 404 로 막으면 응답자는 "링크가 잘못되었다"고 오해합니다. 그래서 **조회는 열되 제출만 `409 FORM_CLOSED`** 로 막습니다. 공개 응답 DTO 에 `status` 를 포함시킨 것이 이 결정이 남긴 흔적입니다.

**제출 실패 코드**는 원인별로 나눕니다. 응답 화면이 어느 문항을 어떻게 고쳐야 하는지 표시해야 하므로 전부 `VALIDATION_FAILED` 로 뭉뚱그리지 않았습니다.

| 코드 | 상태 | 발생 조건 |
|---|---|---|
| `REQUIRED_ANSWER_MISSING` | 400 | 필수 질문에 응답이 없거나 값이 빔. **누락된 질문마다 `fieldErrors` 한 줄** |
| `UNKNOWN_QUESTION` | 400 | 이 폼에 속하지 않은 `questionId` |
| `DUPLICATE_ANSWER` | 400 | 같은 질문에 대한 항목이 두 번 이상 |
| `INVALID_ANSWER_VALUE` | 400 | 타입에 맞는 값이 없음, 다른 질문의 선택지, 택1 질문에 선택지 2개 이상 |
| `ANSWER_OUT_OF_RANGE` | 400 | `RATING`·`NUMBER` 값이 질문의 `minValue`~`maxValue` 밖 |

검증 순서는 **404 → 409 → 400** 입니다. 종료된 폼에 잘못된 값을 보내면 400 이 아니라 409 가 나옵니다. 값을 고쳐도 어차피 제출할 수 없는 상황이라면, 응답자에게 먼저 알려야 할 사실은 "폼이 종료되었다" 이기 때문입니다.

**사용 상태 코드**: 200, 201, 204, 400, 401, 403, 404, 409, 500

## 공통 에러 포맷

모든 에러는 `GlobalExceptionHandler`(`@RestControllerAdvice`) **한 곳에서** 아래 포맷으로 변환합니다. 성공 응답은 별도 래퍼 없이 각 API 의 본문을 그대로 반환하고, 에러에만 공통 포맷을 씌웁니다. 성공까지 래핑하면 모든 클라이언트가 `data` 한 겹을 벗겨야 하는데, HTTP 상태 코드가 이미 성공/실패를 구분하고 있어 그 비용에 대응하는 이득이 없습니다.

```json
{
  "timestamp": "2026-07-19T08:54:19.409836Z",
  "status": 409,
  "error": "Conflict",
  "code": "FORM_CLOSED",
  "message": "종료된 폼에는 응답할 수 없습니다.",
  "path": "/api/public/forms/0q0z8vve/responses",
  "traceId": "01a6b585-5f0d-4f73-b31b-fc4fbe756691",
  "fieldErrors": []
}
```

검증 실패(400)에서는 `fieldErrors` 에 항목별 사유가 채워집니다.

```json
{
  "timestamp": "2026-07-19T08:54:18.534440Z",
  "status": 400,
  "error": "Bad Request",
  "code": "VALIDATION_FAILED",
  "message": "요청 값이 올바르지 않습니다.",
  "path": "/api/forms",
  "traceId": "f65db154-9c11-4879-ae7c-64fa46a73b9f",
  "fieldErrors": [
    { "field": "title", "message": "공백일 수 없습니다" }
  ]
}
```

- **`code` 와 `message` 의 역할이 다릅니다.** `code` 는 클라이언트가 분기에 쓰는 안정적인 식별자이고, `message` 는 사람이 읽는 설명입니다. 문구는 바뀔 수 있으므로 클라이언트는 `message` 문자열을 조건문에 쓰지 않습니다.
- **`traceId`** 는 요청마다 발급되며 응답 헤더 `X-Trace-Id` 및 서버 로그의 `[traceId]` 와 같은 값입니다. 사용자가 캡처한 에러 화면 하나로 해당 요청의 서버 로그를 바로 찾을 수 있게 하기 위한 필드입니다.
- **`fieldErrors` 는 `@Valid` 실패 전용이 아닙니다.** 필수 응답 누락(`REQUIRED_ANSWER_MISSING`)처럼 도메인 규칙 위반도 항목별 사유를 담아야 하는 경우가 있습니다. 이를 위해 예외 핸들러를 새로 추가하는 대신 **`BusinessException` 이 `fieldErrors` 를 지닐 수 있게 확장**했습니다. 핸들러를 늘리면 예외를 응답으로 바꾸는 지점이 흩어지지만, 예외 쪽을 확장하면 변환 지점은 계속 한 곳입니다.
- **누락은 한 번에 모아서 답합니다.** 필수 질문이 3개 빠졌다면 첫 번째에서 멈추지 않고 3건을 모두 담아 반환합니다. 하나씩 알려주면 응답자가 제출-오류를 반복하게 됩니다.
- **500 은 내부 메시지를 노출하지 않습니다.** 예외 원문은 서버 로그에만 남기고 응답에는 `INTERNAL_ERROR` 와 일반 문구만 담습니다.

### 에러 코드 일람

| 코드 | 상태 | 의미 |
|---|---|---|
| `VALIDATION_FAILED` | 400 | 요청 본문 형식 검증 실패 (`fieldErrors` 포함) |
| `MALFORMED_REQUEST` | 400 | JSON 자체를 해석할 수 없음 |
| `OPTIONS_REQUIRED` | 400 | 선택형 질문의 선택지가 2개 미만 |
| `INVALID_VALUE_RANGE` | 400 | 질문의 `minValue > maxValue` |
| `REQUIRED_ANSWER_MISSING` | 400 | 필수 질문 미응답 (`fieldErrors` 포함) |
| `UNKNOWN_QUESTION` | 400 | 폼에 속하지 않은 질문에 대한 응답 |
| `DUPLICATE_ANSWER` | 400 | 같은 질문에 응답 항목이 중복 |
| `INVALID_ANSWER_VALUE` | 400 | 질문 타입에 맞지 않는 응답 값 |
| `ANSWER_OUT_OF_RANGE` | 400 | 응답 값이 질문의 허용 범위 밖 |
| `UNAUTHORIZED` | 401 | 토큰이 없거나 유효하지 않음 (필터 단계) |
| `INVALID_CREDENTIALS` | 401 | 이메일 또는 비밀번호 불일치 |
| `ACCESS_DENIED` | 403 | 소유자가 아닌 리소스 접근 |
| `FORM_NOT_FOUND` | 404 | 폼 없음 (공개 경로에서는 미발행 폼 포함) |
| `QUESTION_NOT_FOUND` | 404 | 해당 폼에 그 질문이 없음 |
| `USER_NOT_FOUND` | 404 | 토큰의 주체에 해당하는 사용자 없음 |
| `RESOURCE_NOT_FOUND` | 404 | 매핑된 핸들러가 없는 경로 |
| `EMAIL_ALREADY_EXISTS` | 409 | 이미 가입된 이메일 |
| `INVALID_STATUS_TRANSITION` | 409 | 허용되지 않는 폼 상태 전이 |
| `FORM_NOT_EDITABLE` | 409 | `DRAFT` 가 아닌 폼의 질문 편집 시도 |
| `FORM_CLOSED` | 409 | 종료된 폼에 응답 제출 |
| `INTERNAL_ERROR` | 500 | 처리되지 않은 예외 |

`UNAUTHORIZED`·`ACCESS_DENIED` 는 **두 경로에서 발생**합니다. 토큰 자체가 없거나 깨진 경우는 시큐리티 필터가(`RestAuthenticationEntryPoint`·`RestAccessDeniedHandler`), 인증은 통과했지만 소유자가 아닌 경우는 서비스가 던진 예외를 `GlobalExceptionHandler` 가 처리합니다. 발생 지점은 다르지만 **`code` 와 포맷을 일부러 동일하게 맞췄습니다.** 클라이언트 입장에서 "인증이 필요하다"는 사실은 같은데 응답 형태가 다르면 에러 처리 코드를 두 벌 써야 하기 때문입니다.

### 날짜·시각 표기

| 필드 | 타입 | 표기 |
|---|---|---|
| 에러의 `timestamp` | `Instant` | UTC, `Z` 접미사 — `2026-07-19T08:54:19.409836Z` |
| 리소스의 `createdAt`·`updatedAt`·`submittedAt` | `LocalDateTime` | 서버 로컬 시각, 오프셋 없음 — `2026-07-19T17:54:18.820619` |

리소스 시각에 오프셋이 없는 것은 DB 컬럼이 타임존 없는 `TIMESTAMP` 이고 엔티티가 `LocalDateTime` 이기 때문입니다. 단일 서버·단일 타임존 전제에서는 문제가 없지만, 두 표기가 한 API 안에 섞여 있는 것은 개선 여지입니다(→ [07. 미완성 / 개선하고 싶은 점](07-limitations.md)).

## 요청/응답 예시

아래 예시는 모두 실제 실행 결과이며, 가독성을 위해 토큰 문자열만 줄였습니다.

### 회원가입 — POST /api/auth/register 🔓
요청
```json
{ "email": "creator@example.com", "password": "password1234", "name": "홍길동" }
```
응답 `201 Created`
```json
{
  "id": "019f7995-5581-7bb2-82b7-7cdfdf8c12f6",
  "email": "creator@example.com",
  "name": "홍길동",
  "createdAt": "2026-07-19T17:54:17.735821"
}
```
> `id` 는 UUID(UUIDv7) 문자열입니다. `users.id` 만 UUID 이며 `/api/auth/me` 응답도 같은 형태입니다. 폼·질문 등 나머지 리소스의 `id` 는 정수입니다(04 "식별자(PK) 전략" 참고). 비밀번호 해시는 어떤 응답에도 포함되지 않습니다.

### 로그인 — POST /api/auth/login 🔓
요청
```json
{ "email": "creator@example.com", "password": "password1234" }
```
응답 `200 OK`
```json
{ "accessToken": "eyJhbGciOiJIUzM4NCJ9....", "tokenType": "Bearer", "expiresIn": 3600 }
```

### 폼 생성 — POST /api/forms 🔒
요청
```json
{ "title": "고객 만족도 조사", "description": "서비스 개선을 위한 설문입니다." }
```
응답 `201 Created` (헤더 `Location: /api/forms/1`)
```json
{
  "id": 1,
  "title": "고객 만족도 조사",
  "description": "서비스 개선을 위한 설문입니다.",
  "status": "DRAFT",
  "slug": "ui39fhjm",
  "questions": [],
  "createdAt": "2026-07-19T17:54:18.141039",
  "updatedAt": "2026-07-19T17:54:18.141039"
}
```
생성 시 상태는 항상 `DRAFT` 이고 **`slug` 는 서버가 발급**합니다. 클라이언트가 slug 를 정하게 하면 추측 가능한 링크(`/public/forms/2026-survey`)가 만들어져 공개 폼이 열거될 수 있습니다.

### 질문 추가 — POST /api/forms/1/questions 🔒

`type` 은 9종입니다: `SHORT_TEXT`, `LONG_TEXT`, `SINGLE_CHOICE`, `DROPDOWN`, `MULTIPLE_CHOICE`(체크박스), `RATING`, `NUMBER`, `DATE`, `TIME`.

- 선택형(`SINGLE_CHOICE`·`DROPDOWN`·`MULTIPLE_CHOICE`)만 `options` 를 가지며 **2개 이상** 이어야 합니다.
- `RATING`·`NUMBER` 는 `minValue`·`maxValue` 로 허용 범위를 정의합니다. **선택 사항이며 둘 다 있을 때만** 대소 관계를 검증합니다.
- 타입에 해당하지 않는 필드가 함께 오면 **무시**합니다. 예컨대 `SHORT_TEXT` 에 `options` 를 보내도 400 이 아니라 그냥 버려집니다. 폼 빌더 UI 가 타입을 바꿀 때 이전 타입의 입력값이 남아 있는 것은 흔한 일이라, 이를 오류로 취급하면 클라이언트가 전송 직전에 필드를 청소해야 합니다.

선택형 예 — 요청
```json
{
  "type": "SINGLE_CHOICE",
  "title": "서비스에 만족하시나요?",
  "required": true,
  "position": 1,
  "options": [
    { "label": "매우 만족", "position": 1 },
    { "label": "보통", "position": 2 },
    { "label": "불만족", "position": 3 }
  ]
}
```
응답 `201 Created` (헤더 `Location: /api/forms/1/questions/1`)
```json
{
  "id": 1,
  "type": "SINGLE_CHOICE",
  "title": "서비스에 만족하시나요?",
  "required": true,
  "position": 1,
  "minValue": null,
  "maxValue": null,
  "options": [
    { "id": 1, "label": "매우 만족", "position": 1 },
    { "id": 2, "label": "보통", "position": 2 },
    { "id": 3, "label": "불만족", "position": 3 }
  ]
}
```

평점 예 (선택지 없음, 범위 메타 사용) — 요청
```json
{ "type": "RATING", "title": "추천 점수", "required": true, "position": 3, "minValue": 1, "maxValue": 5 }
```
응답 `201 Created`
```json
{
  "id": 3, "type": "RATING", "title": "추천 점수", "required": true, "position": 3,
  "minValue": 1, "maxValue": 5, "options": []
}
```
> 선택지가 없는 타입도 `options` 는 `null` 이 아니라 **빈 배열**입니다. 클라이언트가 `options?.map` 같은 방어 코드를 쓰지 않아도 되게 하기 위함입니다.

**질문 수정(PUT)은 선택지를 전량 교체합니다.** 선택지별 부분 수정(추가/삭제/순서 변경)을 받으면 요청 형태가 복잡해지고, 클라이언트가 보낸 최종 상태와 서버 상태가 어긋날 여지가 생깁니다. 폼 빌더는 어차피 편집 중인 질문 전체를 들고 있으므로 "이 질문의 최종 모습" 을 그대로 보내는 편이 단순합니다. 편집이 `DRAFT` 에서만 허용되므로 교체로 인해 유실될 응답도 없습니다.

### 발행 — PATCH /api/forms/1/status 🔒
요청
```json
{ "status": "PUBLISHED" }
```
응답 `200 OK`
```json
{ "id": 1, "status": "PUBLISHED", "slug": "ui39fhjm", "updatedAt": "2026-07-19T17:54:18.442476" }
```
상태 변경은 폼 전체를 덮어쓰는 `PUT` 이 아니라 **`PATCH`** 로 둡니다. 바뀌는 것은 상태 하나이고, 발행/종료는 제목·설명 수정과 검증 규칙이 다른 별개의 행위이기 때문입니다. 응답도 상세 전체가 아니라 변경 결과만 담습니다.

### 폼 목록 — GET /api/forms?status=DRAFT&page=0&size=10&sort=createdAt,desc 🔒

| 파라미터 | 기본값 | 설명 |
|---|---|---|
| `status` | (없음 = 전체) | `DRAFT` / `PUBLISHED` / `CLOSED` 필터 |
| `page` | `0` | 0-기반 페이지 번호 |
| `size` | `20` | 페이지 크기 |
| `sort` | `createdAt,desc` | 정렬 기준(최신순) |

응답 `200 OK`
```json
{
  "content": [
    {
      "id": 2,
      "title": "고객 만족도 조사(개정)",
      "status": "PUBLISHED",
      "responseCount": 1,
      "createdAt": "2026-07-19T17:54:18.182847"
    }
  ],
  "page": 0,
  "size": 10,
  "totalElements": 2,
  "totalPages": 1
}
```
목록 항목은 상세와 **다른 DTO** 입니다. 질문·선택지를 생략하는 대신 목록 화면이 실제로 필요로 하는 `responseCount` 를 담습니다. 이 값은 폼마다 질의하지 않고 현재 페이지의 폼 `id` 를 모아 **한 번에 집계**하여 N+1 을 피합니다.

### 공개 폼 조회 — GET /api/public/forms/{slug} 🔓
응답 `200 OK`
```json
{
  "slug": "0q0z8vve",
  "title": "고객 만족도 조사",
  "description": "서비스 개선을 위한 설문입니다.",
  "status": "PUBLISHED",
  "questions": [
    {
      "id": 1,
      "type": "SINGLE_CHOICE",
      "title": "서비스에 만족하시나요?",
      "required": true,
      "minValue": null,
      "maxValue": null,
      "options": [
        { "id": 1, "label": "매우 만족" },
        { "id": 2, "label": "보통" },
        { "id": 3, "label": "불만족" }
      ]
    },
    {
      "id": 3,
      "type": "RATING",
      "title": "추천 점수",
      "required": true,
      "minValue": 1,
      "maxValue": 5,
      "options": []
    }
  ]
}
```

**공개 응답은 제작자용 DTO 를 재사용하지 않고 별도로 둡니다.** 같은 엔티티에서 나오지만 노출 범위가 다르기 때문입니다.

- 소유자 정보·감사 컬럼(`createdAt` 등)·폼 `id` 를 담지 않습니다. 응답자에게 필요 없는 정보이며, 제작자 API 의 식별자를 공개 경로로 흘리지 않습니다.
- 질문의 `position` 을 노출하지 않습니다. **배열 순서가 곧 표시 순서**이므로 같은 정보를 두 번 보낼 이유가 없습니다.
- 선택지는 `id` 와 `label` 만 보냅니다. `id` 는 제출에 필요하고 `label` 은 화면에 필요한 전부입니다.
- 대신 `status` 를 **추가**합니다. 종료된 폼도 열리므로 화면이 이 값으로 입력 폼과 종료 안내를 갈라야 합니다.

하나의 DTO 를 공유하면서 필드를 `null` 로 비우는 방법도 있지만, 그러면 "이 필드가 왜 비었는가" 가 코드에 드러나지 않고 실수로 노출될 위험이 남습니다.

### 응답 제출 — POST /api/public/forms/{slug}/responses 🔓

`answers` 의 각 항목은 질문 타입에 맞는 필드 **하나**를 채웁니다. 서버는 이를 `answers` 테이블의 값 행으로 저장하며, **선택형은 고른 선택지 수만큼 행**, 그 외는 1행입니다(04 "질문 타입과 응답 저장 매핑" 참고).

| 질문 타입 | 응답 필드 |
|---|---|
| SHORT_TEXT / LONG_TEXT | `textValue` (문자열) |
| SINGLE_CHOICE / DROPDOWN | `selectedOptionIds` (원소 1개) |
| MULTIPLE_CHOICE | `selectedOptionIds` (원소 N개) |
| RATING / NUMBER | `numberValue` (정수) |
| DATE | `dateValue` (`YYYY-MM-DD`) |
| TIME | `timeValue` (`HH:mm`) |

답하지 않은 **선택 질문은 항목 자체를 생략**합니다. 값이 비어 있는 항목을 보내도 미응답으로 취급합니다.

요청
```json
{
  "answers": [
    { "questionId": 1, "selectedOptionIds": [1] },
    { "questionId": 2, "textValue": "친절한 상담이 좋았습니다." },
    { "questionId": 3, "numberValue": 5 }
  ]
}
```
응답 `201 Created`
```json
{ "responseId": 1, "submittedAt": "2026-07-19T17:54:18.820619" }
```

생성 응답이지만 **`Location` 헤더는 두지 않습니다.** 제출된 응답을 조회하는 경로는 제작자 전용이라 익명 응답자가 따라갈 수 없고, 따라갈 수 없는 위치를 알려주는 헤더는 오해만 부릅니다. 응답 본문도 제출 내용을 되돌려주지 않고 **접수 사실만** 확인해 줍니다.

실패 예 — 필수 질문 누락 `400 Bad Request`
```json
{
  "timestamp": "2026-07-19T08:54:18.859903Z",
  "status": 400,
  "error": "Bad Request",
  "code": "REQUIRED_ANSWER_MISSING",
  "message": "필수 질문에 응답하지 않았습니다.",
  "path": "/api/public/forms/0q0z8vve/responses",
  "traceId": "ed61d518-0b12-47e0-9b20-6851ab5a5439",
  "fieldErrors": [
    { "field": "questionId:1", "message": "필수 응답입니다." },
    { "field": "questionId:3", "message": "필수 응답입니다." }
  ]
}
```
`field` 를 `questionId:{id}` 형태로 둔 이유는, 질문이 동적으로 생성되어 **고정된 필드명이 없기** 때문입니다. 요청 배열의 인덱스(`answers[0]`)로 지목할 수도 없습니다 — 누락된 질문은 애초에 배열에 없습니다. 응답 화면은 이 값을 파싱해 해당 문항에 오류를 표시합니다.

실패 예 — 종료된 폼 `409 Conflict`
```json
{
  "timestamp": "2026-07-19T08:54:19.409836Z",
  "status": 409,
  "error": "Conflict",
  "code": "FORM_CLOSED",
  "message": "종료된 폼에는 응답할 수 없습니다.",
  "path": "/api/public/forms/0q0z8vve/responses",
  "traceId": "01a6b585-5f0d-4f73-b31b-fc4fbe756691",
  "fieldErrors": []
}
```

## 인증 필요/불필요 요약

- 🔓 **불필요**: `/api/auth/register`, `/api/auth/login`, `/api/public/**`, Swagger UI 및 OpenAPI 문서
- 🔒 **필요**: 그 외 전부 (`/api/auth/me`, `/api/forms/**`)
- 🔒 중 **소유자만**: 폼·질문 — 소유자가 아니면 403

인증은 **무상태 JWT** 입니다. 서버가 세션을 들고 있지 않으므로 요청마다 `Authorization` 헤더로 주체를 판별하며, 공개 응답 경로는 이 헤더 없이 동작합니다. `/api/public/**` 전체를 익명 허용으로 두어도 안전한 이유는, 이 경로가 다루는 리소스가 **발행된 폼과 그 폼에 대한 신규 응답뿐**이기 때문입니다. 수집된 응답을 읽는 경로는 공개 경로에 두지 않습니다.

Swagger 문서에도 이 구분이 그대로 드러납니다. 인증이 필요한 컨트롤러에만 `@SecurityRequirement(name = "bearerAuth")` 를 붙였고 공개 컨트롤러에는 붙이지 않아, Swagger UI 에서 자물쇠 표시로 구분됩니다. 전역 보안 설정으로 일괄 적용하면 공개 API 까지 인증이 필요한 것처럼 보입니다.

## 관련 문서
- [01. 서비스 개요](01-service-overview.md)
- [03. 시스템 구성도](03-architecture.md) — 요청 흐름·예외 처리
- [04. DB 설계](04-db-design.md) — 응답 데이터 모델·질문 타입별 저장 매핑
- [07. 미완성 / 개선하고 싶은 점](07-limitations.md)
