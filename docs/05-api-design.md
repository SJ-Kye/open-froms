# 05. API 설계

- Base URL `http://localhost:8080` · 인증 헤더 `Authorization: Bearer <JWT>`
- Swagger UI: `http://localhost:8080/swagger-ui.html`
- 성공 응답은 bare 본문, 에러만 래퍼 (`code`, `message`, `traceId`, `fieldErrors?`)
- `ApiContractConsistencyTest`가 엔드포인트·에러 코드·[인증]/[공개] 표기를 OpenAPI 스펙과 소스에서 자동 대조

## 엔드포인트 목록

### 인증 (Auth)
| Method | Path | 설명 | 인증 | 성공 |
|---|---|---|---|---|
| POST | `/api/auth/register` | 회원가입 | [공개] | 201 |
| POST | `/api/auth/login` | 로그인, 토큰 쌍 발급 | [공개] | 200 |
| POST | `/api/auth/refresh` | 리프레시 토큰 회전 | [공개] | 200 |
| POST | `/api/auth/logout` | 리프레시 토큰 폐기 (멱등) | [공개] | 204 |
| GET | `/api/auth/me` | 내 정보 조회 | [인증] | 200 |

### 폼 (Forms) — 소유자만 [인증]
| Method | Path | 설명 | 성공 | 주요 실패 |
|---|---|---|---|---|
| GET | `/api/forms` | 내 폼 목록 (페이지·상태 필터) | 200 | 401 |
| POST | `/api/forms` | 폼 생성 | 201 | 400/401 |
| GET | `/api/forms/{id}` | 폼 상세 (질문·선택지 포함) | 200 | 401/403/404 |
| PUT | `/api/forms/{id}` | 폼 수정 (제목·설명) | 200 | 400/401/403/404 |
| PATCH | `/api/forms/{id}/status` | 상태 전이 (발행/종료) | 200 | 401/403/404/409 |
| DELETE | `/api/forms/{id}` | 폼 삭제 | 204 | 401/403/404 |

### 질문 (Questions) — 소유자만 [인증], DRAFT 폼에서만 쓰기 가능
| Method | Path | 설명 | 성공 |
|---|---|---|---|
| POST | `/api/forms/{formId}/questions` | 질문 추가 | 201 |
| PUT | `/api/forms/{formId}/questions/{id}` | 질문 수정 | 200 |
| DELETE | `/api/forms/{formId}/questions/{id}` | 질문 삭제 | 204 |

질문 편집은 DRAFT에서만. 발행·종료된 폼은 `409 FORM_NOT_EDITABLE`.

### 공개 폼 (Public) — 인증 불필요 [공개]
| Method | Path | 설명 | 성공 | 주요 실패 |
|---|---|---|---|---|
| GET | `/api/public/forms/{slug}` | 공개 폼 조회 | 200 | 404 (없음/미발행) |
| POST | `/api/public/forms/{slug}/responses` | 응답 제출 | 201 | 400/404/409 |

DRAFT는 존재 자체를 숨겨 404. CLOSED는 조회 허용(status 필드로 안내), 제출은 `409 FORM_CLOSED`.

### 응답·집계 (Responses & Analytics) — 소유자만 [인증]
| Method | Path | 설명 | 성공 |
|---|---|---|---|
| GET | `/api/forms/{formId}/responses` | 응답 목록 (페이지네이션) | 200 |
| GET | `/api/forms/{formId}/responses/{responseId}` | 응답 상세 | 200 |
| DELETE | `/api/forms/{formId}/responses/{responseId}` | 응답 삭제 | 204 |
| GET | `/api/forms/{formId}/summary` | 대시보드 집계 | 200 |

## 에러 코드 일람

| 코드 | 상태 | 설명 |
|---|---|---|
| VALIDATION_FAILED | 400 | `@Valid` 실패 (`fieldErrors` 포함) |
| OPTIONS_REQUIRED | 400 | 선택형 질문에 선택지 2개 미만 |
| INVALID_VALUE_RANGE | 400 | RATING/NUMBER `minValue > maxValue` |
| REQUIRED_ANSWER_MISSING | 400 | 필수 질문 미응답 (`fieldErrors`에 누락 질문 목록) |
| UNKNOWN_QUESTION | 400 | 해당 폼에 속하지 않은 questionId |
| DUPLICATE_ANSWER | 400 | 같은 질문에 답변 중복 |
| INVALID_ANSWER_VALUE | 400 | 타입 불일치·다른 폼 선택지·택1 다중 선택 |
| ANSWER_OUT_OF_RANGE | 400 | RATING/NUMBER 허용 범위 초과 |
| INVALID_CREDENTIALS | 401 | 로그인 실패 (이메일/비밀번호 구분 없음, 열거 차단) |
| INVALID_REFRESH_TOKEN | 401 | 리프레시 토큰 무효·만료·폐기 (구분 없음) |
| ACCESS_DENIED | 403 | 소유자가 아님 |
| USER_NOT_FOUND | 404 | 인증 통과했으나 주체 없음 (무상태 JWT 특성) |
| FORM_NOT_FOUND | 404 | 폼 없음 |
| QUESTION_NOT_FOUND | 404 | 질문 없음 |
| RESPONSE_NOT_FOUND | 404 | 응답 없음 |
| EMAIL_ALREADY_EXISTS | 409 | 이메일 중복 |
| FORM_NOT_EDITABLE | 409 | DRAFT 아닌 폼 편집 시도 |
| INVALID_STATUS_TRANSITION | 409 | 불허 상태 전이 |
| FORM_CLOSED | 409 | 종료된 폼에 응답 제출 |

## 응답 포맷

성공: bare 본문. 에러: `{ "code": "FORM_NOT_FOUND", "message": "...", "traceId": "uuid", "fieldErrors"?: [...] }`

`X-Trace-Id` 헤더로 모든 응답에 traceId 포함. Swagger UI: `http://localhost:8080/swagger-ui.html`
