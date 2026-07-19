# 빌드 컨텍스트는 리포지터리 루트입니다: `fly deploy --config open_forms_backend/fly.toml`
# 을 루트에서 실행합니다. build.gradle 의 test 태스크가 ../docs/05-api-design.md 를 입력으로
# 선언하기 때문에 백엔드 디렉터리만 컨텍스트로 잡으면 그 경로가 사라집니다.

# --- 1단계: 빌드 -------------------------------------------------------------
# 베이스가 이미 JDK 21 이라 settings.gradle 의 toolchain resolver 가 별도 JDK 를
# 내려받지 않습니다(이미지 크기·빌드 시간 절약).
FROM eclipse-temurin:21-jdk-noble AS build
WORKDIR /workspace/open_forms_backend

# 의존성 해석을 소스 복사보다 앞에 두어, 소스만 바뀐 배포에서 이 레이어를 재사용합니다.
COPY open_forms_backend/gradlew ./
COPY open_forms_backend/gradle ./gradle
COPY open_forms_backend/settings.gradle open_forms_backend/build.gradle ./
RUN ./gradlew --no-daemon dependencies --configuration runtimeClasspath

COPY docs /workspace/docs
COPY open_forms_backend/src ./src
RUN ./gradlew --no-daemon bootJar

# --- 2단계: 실행 -------------------------------------------------------------
FROM eclipse-temurin:21-jre-noble
WORKDIR /app

# 루트로 실행하지 않습니다.
RUN useradd --system --uid 1001 --create-home app
COPY --from=build --chown=app:app /workspace/open_forms_backend/build/libs/*.jar app.jar
USER app

# 512MB 머신 기준. 컨테이너 상한을 그대로 힙으로 잡으면 메타스페이스·스레드 스택이
# 밀려 OOMKill 이 나므로 70% 로 제한하고, 코어 1개짜리 머신에서는 SerialGC 가 G1 보다
# 오버헤드가 작습니다.
ENV JAVA_OPTS="-XX:MaxRAMPercentage=70.0 -XX:+UseSerialGC -Djava.security.egd=file:/dev/./urandom"

EXPOSE 8080
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
