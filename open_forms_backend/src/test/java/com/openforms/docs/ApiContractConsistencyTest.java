package com.openforms.docs;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * API 계약이 <b>문서·코드·OpenAPI 스펙</b> 세 곳에서 같은 말을 하는지 검증합니다.
 *
 * <p>이 과제의 평가 기준은 "동작"이 아니라 문서와 코드가 서로 교차검증되는가입니다. 그 대조를 사람이
 * 눈으로 하면 구현이 늘어날 때마다 조용히 어긋나므로, 여기서 테스트로 고정합니다. 구체적으로
 * {@code docs/05-api-design.md} 의 표를 파싱해 실행 중인 애플리케이션의 {@code /v3/api-docs} 및
 * 소스의 에러 코드와 맞춰 봅니다.
 *
 * <p>이 테스트가 깨지면 대개 <b>문서를 갱신하지 않고 구현만 바꾼 것</b>이며, 그 반대(문서에만 있는 계약)도
 * 같이 잡습니다. 어느 쪽이 맞는지는 사람이 판단할 몫이라, 실패 메시지에 양쪽 차집합을 그대로 드러냅니다.
 */
@SpringBootTest
@AutoConfigureMockMvc
class ApiContractConsistencyTest {

    /** Gradle 테스트의 작업 디렉터리는 {@code open_forms_backend/} 이므로 문서는 한 단계 위에 있습니다. */
    private static final Path API_DOC = Path.of("..", "docs", "05-api-design.md");
    private static final Path SOURCE_ROOT = Path.of("src", "main", "java");

    /** 표의 엔드포인트 행: {@code | GET | `/api/forms` | 설명 | 🔒 | 200 | 401 / 403 |} */
    private static final Pattern ENDPOINT_ROW = Pattern.compile(
            "^\\|\\s*(GET|POST|PUT|PATCH|DELETE)\\s*\\|\\s*`(/api/[^`]+)`\\s*\\|([^|]*)\\|([^|]*)\\|([^|]*)\\|([^|]*)\\|");

    /** 에러 코드 일람 행: {@code | `FORM_NOT_FOUND` | 404 | 폼 없음 |} */
    private static final Pattern ERROR_CODE_ROW = Pattern.compile(
            "^\\|\\s*`([A-Z][A-Z_]{4,})`\\s*\\|\\s*(\\d{3})\\s*\\|");

    /** 소스의 대문자 문자열 리터럴 — 에러 코드 후보입니다. */
    private static final Pattern UPPERCASE_LITERAL = Pattern.compile("\"([A-Z][A-Z_]{4,})\"");

    /**
     * 대문자 리터럴이지만 에러 코드가 아닌 값들입니다. 문자열 스캔이라 정확히 걸러낼 수 없어 명시적으로
     * 제외합니다. {@code ANONYMOUS} 는 익명 제출 시 감사 컬럼에 남기는 작성자 값입니다.
     */
    private static final Set<String> NOT_ERROR_CODES = Set.of("ANONYMOUS");

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    private JsonNode spec;

    @BeforeEach
    void loadSpec() throws Exception {
        String body = mockMvc.perform(get("/v3/api-docs"))
                .andReturn().getResponse().getContentAsString();
        spec = objectMapper.readTree(body);
    }

    @Test
    @DisplayName("docs/05 의 엔드포인트와 OpenAPI 스펙의 오퍼레이션이 1:1 로 대응한다")
    void documentedEndpointsMatchSpec() throws IOException {
        Set<String> documented = documentedEndpoints().keySet();
        Set<String> implemented = specOperations().keySet();

        assertThat(documented)
                .as("문서에는 있으나 구현되지 않은 엔드포인트 (계획을 커밋 트리에 남기지 않는다)")
                .containsExactlyInAnyOrderElementsOf(implemented);
        assertThat(documented).as("대조된 엔드포인트 수").hasSize(20);
    }

    @Test
    @DisplayName("docs/05 의 🔒/🔓 표기가 스펙의 bearerAuth 보안 요구와 일치한다")
    void securityMarkersMatchSpec() throws IOException {
        Map<String, JsonNode> operations = specOperations();

        documentedEndpoints().forEach((key, documented) -> {
            JsonNode operation = operations.get(key);
            boolean requiresBearer = operation != null && operation.has("security");
            assertThat(requiresBearer)
                    .as("%s — 문서 표기 %s", key, documented.authenticated() ? "🔒" : "🔓")
                    .isEqualTo(documented.authenticated());
        });
    }

    @Test
    @DisplayName("docs/05 가 약속한 실패 상태 코드가 스펙의 응답에도 모두 있다")
    void documentedFailureStatusesArePresentInSpec() throws IOException {
        Map<String, JsonNode> operations = specOperations();

        documentedEndpoints().forEach((key, documented) -> {
            Set<String> declared = new TreeSet<>();
            operations.get(key).get("responses").propertyNames().forEach(declared::add);

            assertThat(declared)
                    .as("%s — 문서가 약속한 상태 코드가 스펙에 없음", key)
                    .contains(documented.statuses().toArray(new String[0]));
        });
    }

    @Test
    @DisplayName("모든 오퍼레이션에 summary 와 tag 가 있다")
    void everyOperationIsLabelled() {
        specOperations().forEach((key, operation) -> {
            assertThat(operation.path("summary").asString(""))
                    .as("%s — Swagger 목록에서 이름 없이 보임", key).isNotBlank();
            assertThat(operation.path("tags").isEmpty())
                    .as("%s — 태그가 없어 그룹에 묶이지 않음", key).isFalse();
        });
    }

    @Test
    @DisplayName("코드가 던지는 에러 코드와 docs/05 의 에러 코드 일람이 서로 빠짐없이 대응한다")
    void errorCodesMatchDocument() throws IOException {
        Set<String> inCode = errorCodesInSource();
        Set<String> inDoc = documentedErrorCodes();

        assertThat(inDoc)
                .as("코드에는 있으나 문서에 없는 에러 코드 / 문서에만 있는 에러 코드")
                .containsExactlyInAnyOrderElementsOf(inCode);
    }

    // --- docs/05 파싱 ---

    /** 문서에 적힌 엔드포인트를 {@code "GET /api/forms"} 키로 모읍니다. */
    private Map<String, DocumentedEndpoint> documentedEndpoints() throws IOException {
        Map<String, DocumentedEndpoint> endpoints = new LinkedHashMap<>();
        for (String line : documentLines()) {
            Matcher matcher = ENDPOINT_ROW.matcher(line);
            if (!matcher.find()) {
                continue;
            }
            String authCell = matcher.group(4);
            List<String> statuses = new ArrayList<>();
            statuses.add(statusIn(matcher.group(5)));
            statuses.addAll(statusesIn(matcher.group(6)));
            endpoints.put(matcher.group(1) + " " + matcher.group(2),
                    new DocumentedEndpoint(authCell.contains("🔒"), statuses));
        }
        assertThat(endpoints).as("docs/05 에서 엔드포인트 표를 읽지 못했습니다").isNotEmpty();
        return endpoints;
    }

    private Set<String> documentedErrorCodes() throws IOException {
        Set<String> codes = new TreeSet<>();
        for (String line : documentLines()) {
            Matcher matcher = ERROR_CODE_ROW.matcher(line);
            if (matcher.find()) {
                codes.add(matcher.group(1));
            }
        }
        assertThat(codes).as("docs/05 에서 에러 코드 일람을 읽지 못했습니다").isNotEmpty();
        return codes;
    }

    private List<String> documentLines() throws IOException {
        assertThat(API_DOC).as("커밋 대상 API 문서가 있어야 합니다").exists();
        return Files.readAllLines(API_DOC);
    }

    private String statusIn(String cell) {
        List<String> found = statusesIn(cell);
        assertThat(found).as("성공 상태 코드를 읽지 못했습니다: %s", cell).isNotEmpty();
        return found.getFirst();
    }

    private List<String> statusesIn(String cell) {
        Matcher matcher = Pattern.compile("\\b([1-5]\\d{2})\\b").matcher(cell);
        List<String> found = new ArrayList<>();
        while (matcher.find()) {
            found.add(matcher.group(1));
        }
        return found;
    }

    // --- OpenAPI 스펙 파싱 ---

    /** 스펙의 오퍼레이션을 문서와 같은 {@code "GET /api/forms"} 키로 모읍니다. */
    private Map<String, JsonNode> specOperations() {
        Map<String, JsonNode> operations = new LinkedHashMap<>();
        JsonNode paths = spec.get("paths");
        paths.propertyNames().forEach(path ->
                paths.get(path).propertyNames().forEach(method ->
                        operations.put(method.toUpperCase() + " " + path, paths.get(path).get(method))));
        assertThat(operations).as("/v3/api-docs 에서 오퍼레이션을 읽지 못했습니다").isNotEmpty();
        return operations;
    }

    // --- 소스 스캔 ---

    /**
     * 소스의 대문자 문자열 리터럴에서 에러 코드를 모읍니다. 예외 생성자·에러 핸들러가 코드를 문자열로
     * 넘기는 구조라 이 방식이 가장 단순한데, 대문자 상수를 새로 쓰면 오탐이 날 수 있어
     * {@link #NOT_ERROR_CODES} 로 제외 목록을 명시해 둡니다.
     */
    private Set<String> errorCodesInSource() throws IOException {
        Set<String> codes = new TreeSet<>();
        try (Stream<Path> files = Files.walk(SOURCE_ROOT)) {
            for (Path file : files.filter(p -> p.toString().endsWith(".java")).toList()) {
                Matcher matcher = UPPERCASE_LITERAL.matcher(Files.readString(file));
                while (matcher.find()) {
                    String candidate = matcher.group(1);
                    if (!NOT_ERROR_CODES.contains(candidate)) {
                        codes.add(candidate);
                    }
                }
            }
        }
        assertThat(codes).as("소스에서 에러 코드를 읽지 못했습니다").isNotEmpty();
        return codes;
    }

    /** 문서 한 행에서 읽은 계약입니다. {@code statuses} 는 성공 코드와 주요 실패 코드를 모두 담습니다. */
    private record DocumentedEndpoint(boolean authenticated, List<String> statuses) {
    }
}
