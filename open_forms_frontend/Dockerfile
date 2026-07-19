# 빌드 컨텍스트는 리포지터리 루트입니다(백엔드와 동일하게 맞춥니다):
# `fly deploy --config open_forms_frontend/fly.toml` 를 루트에서 실행합니다.

# --- 1단계: 정적 번들 빌드 ---------------------------------------------------
FROM node:22-alpine AS build
WORKDIR /app

# lockfile 기준 재현 가능한 설치. 의존성이 안 바뀌면 이 레이어를 재사용합니다.
COPY open_forms_frontend/package.json open_forms_frontend/package-lock.json ./
RUN npm ci

COPY open_forms_frontend/ ./
# VITE_API_BASE_URL 을 주지 않습니다 — apiClient 의 기본값 '/api' 로 두면 nginx 가
# 같은 출처에서 프록시하므로 절대 URL 이 번들에 박히지 않습니다.
RUN npm run build

# --- 2단계: 서빙 -------------------------------------------------------------
FROM nginx:1.27-alpine
COPY open_forms_frontend/nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist /usr/share/nginx/html

EXPOSE 8080
