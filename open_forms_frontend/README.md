# Open Forms — 프론트엔드

Open Forms 의 웹 클라이언트입니다. React 19 · TypeScript · Vite 8 · TanStack Query · Recharts 로 구성되어 있으며,
폼 제작/발행, 공개 링크 응답, 응답 집계 대시보드 화면을 담당합니다.

전체 실행 방법(DB·백엔드 포함)은 [루트 README](../README.md) 를 참고하십시오.

## 요구 사항
- Node.js 20.19+ 또는 22.12+ (Vite 8 요구 버전)

## 실행
```bash
npm install
npm run dev
```
개발 서버 주소는 http://localhost:5173 입니다.
백엔드(http://localhost:8080)가 함께 떠 있어야 로그인·조회가 동작합니다.

## 스크립트
| 명령 | 설명 |
|---|---|
| `npm run dev` | 개발 서버 실행 (HMR) |
| `npm run build` | 타입 체크(`tsc -b`) 후 프로덕션 번들 생성 → `dist/` |
| `npm run preview` | 빌드 결과물을 로컬에서 서빙 |
| `npm run lint` | oxlint 검사 |

## API 연결
API 기본 경로는 `/api` 이고, 개발 서버가 `vite.config.ts` 의 프록시로 `http://localhost:8080` 에 넘깁니다.
브라우저 입장에서 동일 출처가 되므로 별도의 CORS 설정이 필요 없습니다.

백엔드를 다른 호스트에서 띄우는 등 프록시를 우회해야 할 때만 `.env` 를 만들어 절대 URL 을 지정하십시오.

```bash
cp .env.example .env
```
그 뒤 `.env` 에서 다음 줄의 주석을 풀고 값을 조정하십시오.
```
VITE_API_BASE_URL=http://localhost:8080/api
```
