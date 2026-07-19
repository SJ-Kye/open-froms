-- 폼 생명주기 시각: 발행/종료가 일어난 시점을 기록합니다.
--
-- 대시보드의 일별 응답 추이는 "발행일부터"의 구간을 그려야 하는데, 감사 컬럼 updated_at 은 이후 제목
-- 수정에도 갱신되므로 발행 시각의 근거가 될 수 없습니다. created_at 을 쓰면 아직 응답을 받을 수 없던
-- 작성 기간까지 차트에 포함됩니다. closed_at 은 종료된 폼의 추이가 오늘까지 0 으로 끝없이 늘어나지
-- 않도록 구간의 끝을 고정합니다.
--
-- 두 컬럼 모두 NULL 을 허용합니다. DRAFT 폼은 아직 발행되지 않았고, 발행 폼은 아직 종료되지 않았으므로
-- "해당 사건이 일어나지 않았음"을 NULL 로 표현하는 것이 정확합니다.
ALTER TABLE forms ADD COLUMN published_at TIMESTAMP;
ALTER TABLE forms ADD COLUMN closed_at TIMESTAMP;
