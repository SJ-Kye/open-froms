-- 데모 시드 데이터 — 클론 직후 첫 기동에서 바로 둘러볼 수 있는 상태를 만듭니다.
--
-- 이 스크립트는 스키마가 아니라 «데이터»이므로 db/migration 이 아니라 db/seed 에 둡니다. 두 폴더를
-- 나눈 이유는 테스트 격리입니다 — 테스트(H2)는 db/migration 만 읽으므로 시드가 테스트 기대값을
-- 흔들지 않고, 그 대가로 이 파일은 PostgreSQL 전용 문법(generate_series·setseed·배열 인덱싱)을
-- 마음껏 쓸 수 있습니다. 버전 V900 은 앞으로 추가될 스키마 마이그레이션(V5…)보다 항상 뒤에 오도록
-- 크게 띄운 값입니다.
--
-- 값의 배치는 04 DB 설계의 저장 규칙을 그대로 따릅니다: answers 는 «값 하나가 한 행»이고,
-- 체크박스(MULTIPLE_CHOICE)만 고른 선택지 개수만큼 행이 생깁니다.
--
-- 날짜는 모두 실행 시점(localtimestamp) 기준 상대값입니다. 언제 기동하더라도 «최근 몇 주간 응답이 쌓인 폼»으로
-- 보여야 대시보드의 일별 추이가 의미를 갖기 때문입니다.

-- 난수를 고정합니다. 같은 스키마에 다시 시드하면 같은 분포가 나옵니다.
SELECT setseed(0.4242);

-- ── 계정 3개 ──
-- PK 는 UUIDv7 형태의 고정 리터럴입니다(애플리케이션이 생성하는 값과 형식이 같습니다).
-- password_hash 는 셋 다 평문 «demo1234!» 의 BCrypt 해시입니다.
INSERT INTO users (id, email, password_hash, name, created_by, created_at, updated_by, updated_at)
VALUES
    ('01920000-0000-7000-8000-000000000001', 'demo1@openforms.dev',
     '$2a$10$JnyFjN9aQCuMXnKS9tlKRexs.2w34q7T9fSSHqvAZ29OphSE.3axG', '김하늘',
     'demo1@openforms.dev', localtimestamp - interval '60 days', 'demo1@openforms.dev', localtimestamp - interval '60 days'),
    ('01920000-0000-7000-8000-000000000002', 'demo2@openforms.dev',
     '$2a$10$JnyFjN9aQCuMXnKS9tlKRexs.2w34q7T9fSSHqvAZ29OphSE.3axG', '이도윤',
     'demo2@openforms.dev', localtimestamp - interval '58 days', 'demo2@openforms.dev', localtimestamp - interval '58 days'),
    ('01920000-0000-7000-8000-000000000003', 'demo3@openforms.dev',
     '$2a$10$JnyFjN9aQCuMXnKS9tlKRexs.2w34q7T9fSSHqvAZ29OphSE.3axG', '박서연',
     'demo3@openforms.dev', localtimestamp - interval '55 days', 'demo3@openforms.dev', localtimestamp - interval '55 days');

-- ── 폼 18개(계정당 6개: 발행 4 · 종료 1 · 작성 중 1) ──
-- 작성 중(DRAFT)을 하나씩 남겨 두는 이유는, 발행 전 폼의 화면(응답 링크 없음, 추이 차트 빈 상태)도
-- 로그인 직후 바로 확인할 수 있게 하기 위함입니다.
INSERT INTO forms (user_id, title, description, status, slug,
                   published_at, closed_at, created_by, created_at, updated_by, updated_at)
SELECT u.id, s.title, s.description, s.status, s.slug,
       CASE WHEN s.published_days IS NULL THEN NULL
            ELSE localtimestamp - make_interval(days => s.published_days) END,
       CASE WHEN s.closed_days IS NULL THEN NULL
            ELSE localtimestamp - make_interval(days => s.closed_days) END,
       u.email, localtimestamp - make_interval(days => s.created_days),
       u.email, localtimestamp - make_interval(days => s.created_days)
FROM (VALUES
    -- (소유자, slug, 제목, 설명, 상태, 생성 N일 전, 발행 N일 전, 종료 N일 전)
    ('demo1@openforms.dev', 'hr2026h1', '사내 만족도 조사 2026 상반기',
     '반기마다 진행하는 정기 조사입니다. 응답은 익명으로 집계됩니다.', 'PUBLISHED', 45, 42::int, NULL::int),
    ('demo1@openforms.dev', 'wfhpolic', '재택근무 제도 개선 의견',
     '내년도 근무 제도 개편에 반영할 의견을 받습니다.', 'PUBLISHED', 37, 35, NULL),
    ('demo1@openforms.dev', 'lunchmnu', '구내식당 메뉴 선호도',
     '다음 분기 식단 구성에 참고할 선호도를 조사합니다.', 'PUBLISHED', 30, 28, NULL),
    ('demo1@openforms.dev', 'teamevnt', '팀 워크숍 장소 투표',
     '가을 워크숍 장소와 프로그램을 함께 정합니다.', 'PUBLISHED', 23, 21, NULL),
    ('demo1@openforms.dev', 'onboard1', '신입 온보딩 경험 조사',
     '최근 6개월 내 입사자를 대상으로 한 조사입니다.', 'CLOSED', 47, 45, 7),
    ('demo1@openforms.dev', 'hr2026h2', '사내 만족도 조사 2026 하반기',
     '상반기 문항을 다듬어 준비 중입니다.', 'DRAFT', 5, NULL, NULL),

    ('demo2@openforms.dev', 'cafemenu', '카페 신메뉴 선호도 조사',
     '겨울 시즌 메뉴 후보에 대한 선호도를 조사합니다.', 'PUBLISHED', 42, 40, NULL),
    ('demo2@openforms.dev', 'deliverq', '배송 서비스 만족도',
     '최근 3개월 내 주문 경험을 기준으로 답해 주세요.', 'PUBLISHED', 32, 30, NULL),
    ('demo2@openforms.dev', 'appusabl', '모바일 앱 사용성 피드백',
     '앱 개편 전 사용 경험을 듣습니다.', 'PUBLISHED', 26, 24, NULL),
    ('demo2@openforms.dev', 'cuponevt', '쿠폰 이벤트 참여 신청',
     '신청자 중 추첨하여 쿠폰을 지급합니다.', 'PUBLISHED', 18, 16, NULL),
    ('demo2@openforms.dev', 'storeloc', '신규 매장 위치 의견 조사',
     '내년 상반기 출점 후보지를 정하기 위한 조사입니다.', 'CLOSED', 40, 38, 5),
    ('demo2@openforms.dev', 'brandrnw', '브랜드 리뉴얼 시안 평가',
     '시안 이미지가 확정되면 발행할 예정입니다.', 'DRAFT', 4, NULL, NULL),

    ('demo3@openforms.dev', 'studysgn', '알고리즘 스터디 참가 신청',
     '10주 과정으로 진행하는 스터디 신청서입니다.', 'PUBLISHED', 35, 33, NULL),
    ('demo3@openforms.dev', 'confrevw', '개발자 컨퍼런스 후기',
     '행사 운영을 개선하기 위한 후기 조사입니다.', 'PUBLISHED', 28, 26, NULL),
    ('demo3@openforms.dev', 'lecturef', '온라인 강의 만족도',
     '수강을 마친 과정에 대해 답해 주세요.', 'PUBLISHED', 21, 19, NULL),
    ('demo3@openforms.dev', 'meetuptm', '정기 밋업 시간대 조사',
     '참석률이 가장 높은 요일과 시간을 찾습니다.', 'PUBLISHED', 14, 12, NULL),
    ('demo3@openforms.dev', 'bootcamp', '부트캠프 수료 설문',
     '수료생 대상 만족도 및 진로 조사입니다.', 'CLOSED', 46, 44, 3),
    ('demo3@openforms.dev', 'mentorpg', '멘토링 프로그램 기획',
     '멘토 모집 문항을 정리하는 중입니다.', 'DRAFT', 3, NULL, NULL)
) AS s(email, slug, title, description, status, created_days, published_days, closed_days)
JOIN users u ON u.email = s.email;

-- ── 질문 80개 ──
-- 폼 slug 를 키로 한 번에 삽입합니다. min_value/max_value 는 RATING·NUMBER 에만 채웁니다
-- (QuestionType.hasRange() 규칙과 동일). 9종 타입이 모두 최소 한 번씩 등장합니다.
INSERT INTO questions (form_id, type, title, required, position, min_value, max_value)
SELECT f.id, s.type, s.title, s.required, s.position, s.min_value, s.max_value
FROM (VALUES
    -- 사내 만족도 상반기
    ('hr2026h1', 1, 'RATING', '전반적인 업무 만족도를 평가해 주세요', true, 1::int, 5::int),
    ('hr2026h1', 2, 'SINGLE_CHOICE', '소속 부서를 선택해 주세요', true, NULL, NULL),
    ('hr2026h1', 3, 'MULTIPLE_CHOICE', '개선이 가장 필요한 항목을 모두 골라 주세요', false, NULL, NULL),
    ('hr2026h1', 4, 'RATING', '동료와의 협업은 원활한가요', true, 1, 5),
    ('hr2026h1', 5, 'LONG_TEXT', '회사에 바라는 점을 자유롭게 적어 주세요', false, NULL, NULL),
    -- 재택근무 제도
    ('wfhpolic', 1, 'SINGLE_CHOICE', '현재 근무 형태를 선택해 주세요', true, NULL, NULL),
    ('wfhpolic', 2, 'NUMBER', '주당 희망하는 재택 일수는 며칠인가요', true, 0, 5),
    ('wfhpolic', 3, 'RATING', '재택근무 시 생산성은 어떤가요', true, 1, 5),
    ('wfhpolic', 4, 'MULTIPLE_CHOICE', '재택근무에서 겪은 어려움을 모두 골라 주세요', false, NULL, NULL),
    ('wfhpolic', 5, 'SHORT_TEXT', '추가로 필요한 지원이 있다면 적어 주세요', false, NULL, NULL),
    -- 구내식당
    ('lunchmnu', 1, 'DROPDOWN', '가장 선호하는 점심 메뉴 종류는 무엇인가요', true, NULL, NULL),
    ('lunchmnu', 2, 'RATING', '현재 구내식당 만족도를 평가해 주세요', true, 1, 5),
    ('lunchmnu', 3, 'NUMBER', '한 주에 구내식당을 이용하는 횟수는 몇 번인가요', true, 0, 5),
    ('lunchmnu', 4, 'TIME', '선호하는 점심 시작 시간을 골라 주세요', false, NULL, NULL),
    ('lunchmnu', 5, 'SHORT_TEXT', '추가되었으면 하는 메뉴가 있나요', false, NULL, NULL),
    -- 워크숍 투표
    ('teamevnt', 1, 'SINGLE_CHOICE', '워크숍 장소로 선호하는 곳을 골라 주세요', true, NULL, NULL),
    ('teamevnt', 2, 'DATE', '참석 가능한 날짜를 선택해 주세요', true, NULL, NULL),
    ('teamevnt', 3, 'MULTIPLE_CHOICE', '함께 하고 싶은 프로그램을 모두 골라 주세요', false, NULL, NULL),
    ('teamevnt', 4, 'SHORT_TEXT', '이동 수단에 대한 의견을 적어 주세요', false, NULL, NULL),
    -- 온보딩(종료)
    ('onboard1', 1, 'RATING', '온보딩 과정 전반에 만족하셨나요', true, 1, 5),
    ('onboard1', 2, 'NUMBER', '업무에 적응하기까지 몇 주가 걸렸나요', true, 1, 12),
    ('onboard1', 3, 'SINGLE_CHOICE', '가장 도움이 된 것을 하나만 골라 주세요', true, NULL, NULL),
    ('onboard1', 4, 'MULTIPLE_CHOICE', '보완이 필요한 부분을 모두 골라 주세요', false, NULL, NULL),
    ('onboard1', 5, 'LONG_TEXT', '온보딩 경험을 자유롭게 적어 주세요', false, NULL, NULL),
    -- 사내 만족도 하반기(작성 중)
    ('hr2026h2', 1, 'RATING', '전반적인 업무 만족도를 평가해 주세요', true, 1, 5),
    ('hr2026h2', 2, 'LONG_TEXT', '상반기 대비 달라졌다고 느낀 점을 적어 주세요', false, NULL, NULL),

    -- 카페 신메뉴
    ('cafemenu', 1, 'DROPDOWN', '가장 자주 마시는 음료는 무엇인가요', true, NULL, NULL),
    ('cafemenu', 2, 'MULTIPLE_CHOICE', '신메뉴로 출시되면 좋겠는 음료를 모두 골라 주세요', false, NULL, NULL),
    ('cafemenu', 3, 'RATING', '현재 메뉴 구성에 대한 만족도를 평가해 주세요', true, 1, 5),
    ('cafemenu', 4, 'NUMBER', '음료 한 잔에 지불할 의향이 있는 금액은 몇 천원인가요', true, 3, 10),
    ('cafemenu', 5, 'SHORT_TEXT', '매장에 바라는 점을 적어 주세요', false, NULL, NULL),
    -- 배송 만족도
    ('deliverq', 1, 'RATING', '배송 속도에 만족하셨나요', true, 1, 5),
    ('deliverq', 2, 'RATING', '포장 상태에 만족하셨나요', true, 1, 5),
    ('deliverq', 3, 'SINGLE_CHOICE', '주로 이용하는 배송 옵션을 골라 주세요', true, NULL, NULL),
    ('deliverq', 4, 'MULTIPLE_CHOICE', '불편했던 점을 모두 골라 주세요', false, NULL, NULL),
    ('deliverq', 5, 'LONG_TEXT', '개선 의견을 남겨 주세요', false, NULL, NULL),
    -- 앱 사용성
    ('appusabl', 1, 'RATING', '앱 사용이 편리했나요', true, 1, 5),
    ('appusabl', 2, 'SINGLE_CHOICE', '주로 사용하는 기기를 골라 주세요', true, NULL, NULL),
    ('appusabl', 3, 'MULTIPLE_CHOICE', '자주 사용하는 기능을 모두 골라 주세요', false, NULL, NULL),
    ('appusabl', 4, 'NUMBER', '최근 한 달간 앱을 몇 번 사용했나요', false, 0, 60),
    ('appusabl', 5, 'SHORT_TEXT', '가장 불편했던 화면은 어디인가요', false, NULL, NULL),
    -- 쿠폰 이벤트
    ('cuponevt', 1, 'SHORT_TEXT', '성함을 입력해 주세요', true, NULL, NULL),
    ('cuponevt', 2, 'SINGLE_CHOICE', '받고 싶은 쿠폰을 선택해 주세요', true, NULL, NULL),
    ('cuponevt', 3, 'DATE', '쿠폰 사용 예정일을 알려 주세요', false, NULL, NULL),
    ('cuponevt', 4, 'TIME', '연락 가능한 시간대를 알려 주세요', false, NULL, NULL),
    -- 매장 위치(종료)
    ('storeloc', 1, 'SINGLE_CHOICE', '신규 매장이 생기면 좋겠는 지역을 골라 주세요', true, NULL, NULL),
    ('storeloc', 2, 'RATING', '현재 매장 접근성에 만족하시나요', true, 1, 5),
    ('storeloc', 3, 'MULTIPLE_CHOICE', '매장에서 중요하게 보는 요소를 모두 골라 주세요', false, NULL, NULL),
    ('storeloc', 4, 'NUMBER', '매장까지 이동할 수 있는 시간은 몇 분인가요', false, 5, 60),
    ('storeloc', 5, 'LONG_TEXT', '추천하고 싶은 위치와 이유를 적어 주세요', false, NULL, NULL),
    -- 브랜드 리뉴얼(작성 중)
    ('brandrnw', 1, 'SINGLE_CHOICE', '선호하는 시안을 골라 주세요', true, NULL, NULL),
    ('brandrnw', 2, 'LONG_TEXT', '그 시안을 고른 이유를 적어 주세요', false, NULL, NULL),

    -- 스터디 신청
    ('studysgn', 1, 'SHORT_TEXT', '닉네임을 입력해 주세요', true, NULL, NULL),
    ('studysgn', 2, 'SINGLE_CHOICE', '희망하는 난이도를 골라 주세요', true, NULL, NULL),
    ('studysgn', 3, 'MULTIPLE_CHOICE', '다루고 싶은 주제를 모두 골라 주세요', false, NULL, NULL),
    ('studysgn', 4, 'DROPDOWN', '참여 가능한 요일을 골라 주세요', true, NULL, NULL),
    ('studysgn', 5, 'TIME', '선호하는 시작 시간을 알려 주세요', true, NULL, NULL),
    ('studysgn', 6, 'NUMBER', '주당 투자할 수 있는 시간은 몇 시간인가요', false, 1, 20),
    -- 컨퍼런스 후기
    ('confrevw', 1, 'RATING', '컨퍼런스 전반에 만족하셨나요', true, 1, 5),
    ('confrevw', 2, 'SINGLE_CHOICE', '가장 좋았던 세션을 골라 주세요', true, NULL, NULL),
    ('confrevw', 3, 'MULTIPLE_CHOICE', '개선되면 좋을 점을 모두 골라 주세요', false, NULL, NULL),
    ('confrevw', 4, 'RATING', '다음 행사에도 참여할 의향이 있나요', true, 1, 5),
    ('confrevw', 5, 'LONG_TEXT', '기억에 남는 순간을 적어 주세요', false, NULL, NULL),
    -- 온라인 강의
    ('lecturef', 1, 'RATING', '강의 내용에 만족하셨나요', true, 1, 5),
    ('lecturef', 2, 'RATING', '강사의 전달력은 어땠나요', true, 1, 5),
    ('lecturef', 3, 'DROPDOWN', '수강한 과정을 선택해 주세요', true, NULL, NULL),
    ('lecturef', 4, 'NUMBER', '완강까지 몇 주가 걸렸나요', false, 1, 16),
    ('lecturef', 5, 'SHORT_TEXT', '추가로 듣고 싶은 주제를 적어 주세요', false, NULL, NULL),
    -- 밋업 시간대
    ('meetuptm', 1, 'DROPDOWN', '선호하는 요일을 골라 주세요', true, NULL, NULL),
    ('meetuptm', 2, 'TIME', '선호하는 시작 시간을 알려 주세요', true, NULL, NULL),
    ('meetuptm', 3, 'MULTIPLE_CHOICE', '참여하고 싶은 형식을 모두 골라 주세요', false, NULL, NULL),
    ('meetuptm', 4, 'DATE', '다음 참석 예정일을 알려 주세요', false, NULL, NULL),
    -- 부트캠프(종료)
    ('bootcamp', 1, 'RATING', '부트캠프 전반에 만족하셨나요', true, 1, 5),
    ('bootcamp', 2, 'NUMBER', '하루 평균 학습 시간은 몇 시간이었나요', true, 1, 14),
    ('bootcamp', 3, 'SINGLE_CHOICE', '수료 후 진로를 골라 주세요', true, NULL, NULL),
    ('bootcamp', 4, 'MULTIPLE_CHOICE', '가장 도움이 된 활동을 모두 골라 주세요', false, NULL, NULL),
    ('bootcamp', 5, 'LONG_TEXT', '후배 수강생에게 남기고 싶은 조언을 적어 주세요', false, NULL, NULL),
    ('bootcamp', 6, 'DATE', '수료일을 입력해 주세요', false, NULL, NULL),
    -- 멘토링(작성 중)
    ('mentorpg', 1, 'SHORT_TEXT', '관심 분야를 입력해 주세요', true, NULL, NULL),
    ('mentorpg', 2, 'RATING', '멘토링이 얼마나 필요하다고 느끼시나요', true, 1, 5)
) AS s(slug, position, type, title, required, min_value, max_value)
JOIN forms f ON f.slug = s.slug;

-- ── 선택지 ──
-- (폼 slug, 질문 position) 으로 질문을 찾아 한 번에 삽입합니다.
INSERT INTO question_options (question_id, label, position)
SELECT q.id, s.label, s.position
FROM (VALUES
    ('hr2026h1', 2, '개발', 1), ('hr2026h1', 2, '디자인', 2), ('hr2026h1', 2, '기획', 3),
    ('hr2026h1', 2, '영업', 4), ('hr2026h1', 2, '경영지원', 5),
    ('hr2026h1', 3, '급여와 복지', 1), ('hr2026h1', 3, '업무량', 2), ('hr2026h1', 3, '조직문화', 3),
    ('hr2026h1', 3, '성장 기회', 4), ('hr2026h1', 3, '근무 환경', 5), ('hr2026h1', 3, '부서 간 소통', 6),

    ('wfhpolic', 1, '전면 출근', 1), ('wfhpolic', 1, '주 1~2일 재택', 2),
    ('wfhpolic', 1, '주 3일 이상 재택', 3), ('wfhpolic', 1, '전면 재택', 4),
    ('wfhpolic', 4, '소통 지연', 1), ('wfhpolic', 4, '집중 어려움', 2), ('wfhpolic', 4, '장비 부족', 3),
    ('wfhpolic', 4, '고립감', 4), ('wfhpolic', 4, '특별한 어려움 없음', 5),

    ('lunchmnu', 1, '한식', 1), ('lunchmnu', 1, '중식', 2), ('lunchmnu', 1, '일식', 3),
    ('lunchmnu', 1, '양식', 4), ('lunchmnu', 1, '분식', 5), ('lunchmnu', 1, '샐러드', 6),

    ('teamevnt', 1, '강원 속초', 1), ('teamevnt', 1, '경기 가평', 2),
    ('teamevnt', 1, '충남 태안', 3), ('teamevnt', 1, '제주', 4),
    ('teamevnt', 3, '팀 빌딩 게임', 1), ('teamevnt', 3, '자유 시간', 2), ('teamevnt', 3, '회고 세션', 3),
    ('teamevnt', 3, '야외 활동', 4), ('teamevnt', 3, '맛집 투어', 5),

    ('onboard1', 3, '멘토 제도', 1), ('onboard1', 3, '온보딩 문서', 2),
    ('onboard1', 3, '팀 소개 세션', 3), ('onboard1', 3, '사내 교육', 4),
    ('onboard1', 4, '문서 최신화', 1), ('onboard1', 4, '장비 지급', 2), ('onboard1', 4, '권한 신청 절차', 3),
    ('onboard1', 4, '입사 전 사전 안내', 4), ('onboard1', 4, '멘토 매칭', 5),
    ('cafemenu', 1, '아메리카노', 1), ('cafemenu', 1, '라떼', 2), ('cafemenu', 1, '콜드브루', 3),
    ('cafemenu', 1, '티', 4), ('cafemenu', 1, '스무디', 5), ('cafemenu', 1, '에이드', 6),
    ('cafemenu', 2, '흑임자 라떼', 1), ('cafemenu', 2, '청포도 에이드', 2), ('cafemenu', 2, '말차 프라페', 3),
    ('cafemenu', 2, '딸기 라떼', 4), ('cafemenu', 2, '오렌지 콜드브루', 5),

    ('deliverq', 3, '일반 배송', 1), ('deliverq', 3, '새벽 배송', 2),
    ('deliverq', 3, '당일 배송', 3), ('deliverq', 3, '편의점 픽업', 4),
    ('deliverq', 4, '배송 지연', 1), ('deliverq', 4, '상품 파손', 2), ('deliverq', 4, '오배송', 3),
    ('deliverq', 4, '기사 응대', 4), ('deliverq', 4, '불편한 점 없음', 5),

    ('appusabl', 2, 'Android', 1), ('appusabl', 2, 'iPhone', 2),
    ('appusabl', 2, '태블릿', 3), ('appusabl', 2, '웹 브라우저', 4),
    ('appusabl', 3, '검색', 1), ('appusabl', 3, '장바구니', 2), ('appusabl', 3, '알림', 3),
    ('appusabl', 3, '리뷰', 4), ('appusabl', 3, '위시리스트', 5), ('appusabl', 3, '간편 결제', 6),

    ('cuponevt', 2, '5천원 할인 쿠폰', 1), ('cuponevt', 2, '무료 배송 쿠폰', 2),
    ('cuponevt', 2, '1+1 쿠폰', 3), ('cuponevt', 2, '포인트 3천점', 4),

    ('storeloc', 1, '강남', 1), ('storeloc', 1, '홍대', 2), ('storeloc', 1, '성수', 3),
    ('storeloc', 1, '잠실', 4), ('storeloc', 1, '판교', 5),
    ('storeloc', 3, '좌석 수', 1), ('storeloc', 3, '주차 공간', 2), ('storeloc', 3, '콘센트', 3),
    ('storeloc', 3, '조용한 분위기', 4), ('storeloc', 3, '영업 시간', 5),

    ('brandrnw', 1, 'A안', 1), ('brandrnw', 1, 'B안', 2), ('brandrnw', 1, 'C안', 3),

    ('studysgn', 2, '입문', 1), ('studysgn', 2, '초급', 2),
    ('studysgn', 2, '중급', 3), ('studysgn', 2, '고급', 4),
    ('studysgn', 3, '그래프', 1), ('studysgn', 3, '동적 계획법', 2), ('studysgn', 3, '문자열', 3),
    ('studysgn', 3, '그리디', 4), ('studysgn', 3, '자료구조', 5), ('studysgn', 3, '트리', 6),
    ('studysgn', 4, '월요일', 1), ('studysgn', 4, '화요일', 2), ('studysgn', 4, '수요일', 3),
    ('studysgn', 4, '목요일', 4), ('studysgn', 4, '금요일', 5), ('studysgn', 4, '토요일', 6),
    ('studysgn', 4, '일요일', 7),

    ('confrevw', 2, '키노트', 1), ('confrevw', 2, '백엔드 트랙', 2), ('confrevw', 2, '프론트엔드 트랙', 3),
    ('confrevw', 2, 'AI 트랙', 4), ('confrevw', 2, '라이트닝 토크', 5),
    ('confrevw', 3, '좌석 배치', 1), ('confrevw', 3, '음향', 2), ('confrevw', 3, '네트워킹 시간', 3),
    ('confrevw', 3, '식사', 4), ('confrevw', 3, '사전 안내', 5), ('confrevw', 3, '개선할 점 없음', 6),

    ('lecturef', 3, '백엔드 입문', 1), ('lecturef', 3, '프론트엔드 입문', 2),
    ('lecturef', 3, '데이터 분석', 3), ('lecturef', 3, '클라우드', 4), ('lecturef', 3, '알고리즘', 5),

    ('meetuptm', 1, '화요일', 1), ('meetuptm', 1, '수요일', 2), ('meetuptm', 1, '목요일', 3),
    ('meetuptm', 1, '금요일', 4), ('meetuptm', 1, '토요일', 5),
    ('meetuptm', 3, '발표', 1), ('meetuptm', 3, '토론', 2), ('meetuptm', 3, '코드 리뷰', 3),
    ('meetuptm', 3, '네트워킹', 4), ('meetuptm', 3, '스터디', 5),

    ('bootcamp', 3, '취업', 1), ('bootcamp', 3, '이직', 2), ('bootcamp', 3, '창업', 3),
    ('bootcamp', 3, '진학', 4), ('bootcamp', 3, '아직 미정', 5),
    ('bootcamp', 4, '팀 프로젝트', 1), ('bootcamp', 4, '코드 리뷰', 2), ('bootcamp', 4, '멘토링', 3),
    ('bootcamp', 4, '현업자 특강', 4), ('bootcamp', 4, '취업 지원', 5)
) AS s(slug, question_position, label, position)
JOIN forms f ON f.slug = s.slug
JOIN questions q ON q.form_id = f.id AND q.position = s.question_position;

-- ── 응답 227건 ──
-- 폼마다 개수를 다르게 주어 목록 화면의 응답 수가 한눈에 구분되게 합니다. 제출 시각은 발행일부터
-- 종료일(종료 전이면 오늘) 사이에 흩뿌리되 살짝 앞쪽으로 치우치게 해(power(random(), 0.8)),
-- 발행 직후 응답이 몰렸다가 잦아드는 실제 곡선에 가깝게 만듭니다.
INSERT INTO responses (form_id, created_by, created_at)
SELECT f.id, 'ANONYMOUS',
       f.published_at
           + (LEAST(COALESCE(f.closed_at, localtimestamp), localtimestamp) - f.published_at) * power(random(), 0.8)
FROM (VALUES
    ('hr2026h1', 24), ('wfhpolic', 18), ('lunchmnu', 15), ('teamevnt', 11), ('onboard1', 13),
    ('cafemenu', 22), ('deliverq', 17), ('appusabl', 14), ('cuponevt', 9), ('storeloc', 12),
    ('studysgn', 16), ('confrevw', 20), ('lecturef', 13), ('meetuptm', 8), ('bootcamp', 15)
) AS s(slug, response_count)
JOIN forms f ON f.slug = s.slug
CROSS JOIN LATERAL generate_series(1, s.response_count);

-- ── 답변 ──
-- 응답 × 질문 조합을 먼저 만들어 두고 타입별로 나눠 채웁니다. 필수 문항은 항상, 선택 문항은 78% 만
-- 채워 완료율이 1.0 에 붙지 않게 합니다(대시보드의 완료율 카드가 의미를 갖도록).
--
-- 여기서만 random() 이 아니라 (응답 id, 질문 id) 해시를 씁니다. 조건이 질문 컬럼만 참조하면 플래너가
-- 그 조건을 조인 전 questions 스캔으로 내려버려, 질문 단위로 «전부 채움/전부 건너뜀»이 되기 때문입니다
-- (실제로 그렇게 되어 폼마다 완료율이 정확히 0.8 또는 1.0 으로만 나왔습니다). 응답 id 를 함께 섞으면
-- 조건이 조인 결과에서 평가되고, 값도 실행할 때마다 같아 재현 가능합니다.
CREATE TEMP TABLE seed_answer_targets AS
SELECT r.id AS response_id, q.id AS question_id, q.type, q.title, q.min_value, q.max_value
FROM responses r
JOIN forms f ON f.id = r.form_id
JOIN questions q ON q.form_id = f.id
WHERE f.created_by IN ('demo1@openforms.dev', 'demo2@openforms.dev', 'demo3@openforms.dev')
  AND (q.required
       OR abs(hashtext(r.id::text || '#' || q.id::text)) % 100 < 78);

-- 단답: 이름을 묻는 문항만 사람 이름으로, 나머지는 짧은 의견으로 채웁니다.
INSERT INTO answers (response_id, question_id, text_value)
SELECT t.response_id, t.question_id,
       CASE WHEN t.title LIKE '%성함%' OR t.title LIKE '%닉네임%' THEN
           (ARRAY['김민준', '이서준', '박지호', '최수아', '정하윤', '강도현', '조예린', '윤시우',
                  '임채원', '한지훈', '오유진', '서준우'])[1 + floor(random() * 12)::int]
       ELSE
           (ARRAY['지금도 충분히 만족합니다', '선택지가 조금 더 많으면 좋겠어요', '결제 화면이 조금 헷갈립니다',
                  '가격이 합리적이면 좋겠습니다', '알림이 너무 자주 옵니다', '샐러드 메뉴 추가 부탁드립니다',
                  '주차 공간이 아쉽습니다', '검색 기능을 자주 씁니다', '클라우드 실습 과정이 궁금합니다',
                  '전반적으로 만족스러웠습니다', '모바일에서 글씨가 작게 보입니다',
                  '특별히 불편한 점은 없었습니다'])[1 + floor(random() * 12)::int]
       END
FROM seed_answer_targets t
WHERE t.type = 'SHORT_TEXT';

-- 장문
INSERT INTO answers (response_id, question_id, text_value)
SELECT t.response_id, t.question_id,
       (ARRAY[
           '전반적으로 만족하고 있습니다. 다만 부서 간 소통이 조금 더 활발해지면 좋겠습니다.',
           '업무량이 시기별로 크게 몰리는 편이라 인력 배분이 조금 더 유연했으면 합니다.',
           '교육 기회가 늘어난 점이 좋았습니다. 다음에는 실습 비중이 더 높았으면 합니다.',
           '처음에는 낯설었지만 멘토가 꼼꼼히 챙겨 주셔서 빠르게 적응할 수 있었습니다.',
           '문서가 잘 정리되어 있어 도움이 되었습니다. 최신 내용으로 갱신만 자주 되면 좋겠습니다.',
           '배송은 대체로 빨랐지만 포장이 조금 헐거운 경우가 있었습니다.',
           '행사 운영이 매끄러웠습니다. 네트워킹 시간이 조금 더 길었으면 좋겠습니다.',
           '실무에 바로 적용할 수 있는 내용이 많아 유익했습니다.',
           '팀 프로젝트에서 배운 것이 가장 많았습니다. 시간이 조금 부족했던 점만 아쉽습니다.',
           '조급해하지 말고 기본기를 탄탄히 다지는 것을 추천드립니다.',
           '접근성이 좋은 위치라면 조금 비싸더라도 자주 방문할 것 같습니다.',
           '전체적인 흐름은 좋았고, 안내 메일이 조금 더 일찍 오면 준비하기 편할 것 같습니다.'
       ])[1 + floor(random() * 12)::int]
FROM seed_answer_targets t
WHERE t.type = 'LONG_TEXT';

-- 택1형(단일 선택·드롭다운)
-- 선택지를 «질문마다 다른 순서»로 줄 세운 뒤 앞쪽에 치우친 난수(power(random(), 1.8))로 고릅니다.
-- 균등하게 뽑으면 모든 막대가 비슷해져 차트가 밋밋해지고, 위치 순서 그대로 치우치게 하면 어느 폼에서나
-- 첫 선택지만 1위가 됩니다. 치우침 덕분에 응답 수가 적은 폼에서는 0표 선택지도 자연스럽게 남습니다.
INSERT INTO answers (response_id, question_id, option_id)
SELECT t.response_id, t.question_id, picked.id
FROM seed_answer_targets t
JOIN LATERAL (
    SELECT o.id
    FROM question_options o
    CROSS JOIN (SELECT count(*) AS total FROM question_options
                 WHERE question_id = t.question_id) c
    WHERE o.question_id = t.question_id
    ORDER BY (o.position * 5 + t.question_id * 3) % c.total, o.position
    OFFSET floor(power(random(), 1.8) * (SELECT count(*) FROM question_options
                                          WHERE question_id = t.question_id))::int
    LIMIT 1
) picked ON true
WHERE t.type IN ('SINGLE_CHOICE', 'DROPDOWN');

-- 체크박스: 선택지마다 독립적으로 고릅니다(값 하나가 한 행 → 한 응답이 여러 행).
INSERT INTO answers (response_id, question_id, option_id)
SELECT t.response_id, t.question_id, o.id
FROM seed_answer_targets t
JOIN question_options o ON o.question_id = t.question_id
WHERE t.type = 'MULTIPLE_CHOICE'
  AND random() < 0.34;

-- 하나도 고르지 못한 체크박스 응답은 첫 선택지로 채웁니다. 아무 행도 없으면 «답하지 않은 문항»이 되어
-- 완료율이 필요 이상으로 낮아집니다.
INSERT INTO answers (response_id, question_id, option_id)
SELECT t.response_id, t.question_id,
       (SELECT o.id FROM question_options o WHERE o.question_id = t.question_id
         ORDER BY o.position LIMIT 1)
FROM seed_answer_targets t
WHERE t.type = 'MULTIPLE_CHOICE'
  AND NOT EXISTS (SELECT 1 FROM answers a
                   WHERE a.response_id = t.response_id AND a.question_id = t.question_id);

-- 평점: 질문마다 분포를 달리합니다(지수 < 1 이면 높은 점수로, > 1 이면 낮은 점수로 치우침).
-- 모든 문항이 똑같이 4~5점에 몰리면 평균 카드가 전부 같은 값이 되어 비교가 되지 않습니다.
INSERT INTO answers (response_id, question_id, number_value)
SELECT t.response_id, t.question_id,
       t.min_value + round((t.max_value - t.min_value)
           * power(random(), CASE WHEN t.question_id % 3 = 0 THEN 1.5 ELSE 0.55 END))::int
FROM seed_answer_targets t
WHERE t.type = 'RATING';

-- 숫자: 문항이 정한 범위 안에서 낮은 쪽에 살짝 치우치게 뽑습니다.
INSERT INTO answers (response_id, question_id, number_value)
SELECT t.response_id, t.question_id,
       t.min_value + floor((t.max_value - t.min_value + 1) * power(random(), 1.3))::int
FROM seed_answer_targets t
WHERE t.type = 'NUMBER';

-- 날짜: 최근 2주 전 ~ 4주 후 범위
INSERT INTO answers (response_id, question_id, date_value)
SELECT t.response_id, t.question_id,
       current_date - 14 + floor(random() * 42)::int
FROM seed_answer_targets t
WHERE t.type = 'DATE';

-- 시간: 09:00 ~ 19:00 의 30분 단위
INSERT INTO answers (response_id, question_id, time_value)
SELECT t.response_id, t.question_id,
       time '09:00' + make_interval(mins => (floor(random() * 21) * 30)::int)
FROM seed_answer_targets t
WHERE t.type = 'TIME';

DROP TABLE seed_answer_targets;
