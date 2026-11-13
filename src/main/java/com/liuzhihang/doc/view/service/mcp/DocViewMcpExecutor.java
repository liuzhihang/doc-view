package com.liuzhihang.doc.view.service.mcp;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.intellij.openapi.progress.EmptyProgressIndicator;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.IndexNotReadyException;
import com.liuzhihang.doc.view.DocViewBuildInfo;
import com.liuzhihang.doc.view.service.document.ProjectDocumentData.ApiData;
import com.liuzhihang.doc.view.service.document.ProjectDocumentData.BodyData;
import com.liuzhihang.doc.view.service.document.ProjectDocumentData.EntryData;
import com.liuzhihang.doc.view.service.document.ProjectDocumentData.FieldData;
import com.liuzhihang.doc.view.service.document.ProjectDocumentData.FileData;
import com.liuzhihang.doc.view.service.document.ProjectDocumentData.HeaderData;
import com.liuzhihang.doc.view.service.document.ProjectDocumentData.IdentityData;
import com.liuzhihang.doc.view.service.document.ProjectDocumentDetailResult;
import com.liuzhihang.doc.view.service.document.ProjectDocumentIndexService;
import com.liuzhihang.doc.view.service.document.ProjectDocumentIndexStatus;
import com.liuzhihang.doc.view.service.document.ProjectDocumentMarkdownResult;
import com.liuzhihang.doc.view.service.document.ProjectDocumentQuery;
import com.liuzhihang.doc.view.service.document.ProjectDocumentQueryException;
import com.liuzhihang.doc.view.service.document.ProjectDocumentQueryResult;
import com.liuzhihang.doc.view.service.document.ProjectDocumentData.SourceData;
import com.liuzhihang.doc.view.service.export.ProjectDocumentConflictException;
import com.liuzhihang.doc.view.service.export.ProjectDocumentExportResult;
import com.liuzhihang.doc.view.service.export.ProjectDocumentGenerationException;
import com.liuzhihang.doc.view.service.export.ProjectDocumentGenerationService;
import com.liuzhihang.doc.view.service.export.ProjectDocumentIdentity;
import com.liuzhihang.doc.view.service.export.ProjectDocumentIndexNotReadyException;
import com.liuzhihang.doc.view.service.export.ProjectDocumentPublishException;
import com.liuzhihang.doc.view.service.export.ProjectDocumentSnapshot;
import com.liuzhihang.doc.view.service.export.ProjectDocumentTemporaryCleanupException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.InvalidPathException;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.function.BooleanSupplier;
import java.util.regex.Pattern;

/** Executes the Doc View MCP contract against the same project document services used by the UI. */
public final class DocViewMcpExecutor {

    public static final String STATUS = "doc_view_status";
    public static final String SEARCH = "doc_view_search";
    public static final String GET = "doc_view_get";
    public static final String GENERATE = "doc_view_generate";

    public static final int TOOL_CONTRACT_VERSION = 1;
    public static final int MAX_RESULT_BYTES = 4 * 1024 * 1024;

    private static final int MAX_DISPLAY_PATHS = 100;
    private static final int MAX_PATH_CODE_POINTS = 512;
    private static final Pattern SHA_256 = Pattern.compile("[a-f0-9]{64}");
    private static final Set<String> TOOLS = Set.of(STATUS, SEARCH, GET, GENERATE);
    private static final Set<String> FRAMEWORKS = Set.of("spring", "feign", "dubbo", "interface");
    private static final Set<String> HTTP_METHODS = Set.of(
            "GET", "HEAD", "POST", "PUT", "DELETE", "CONNECT", "OPTIONS", "TRACE", "PATCH");
    private static final Set<String> ERROR_CODES = Set.of(
            "INVALID_ARGUMENT", "PROJECT_NOT_OPEN", "INDEX_NOT_FOUND", "UNSUPPORTED_SCHEMA", "INDEX_INVALID",
            "INDEX_INCONSISTENT", "API_NOT_FOUND", "CURSOR_STALE", "SNAPSHOT_CHANGED", "INDEXING", "BUSY",
            "CONFLICT", "SOURCE_CHANGED", "LIMIT_EXCEEDED", "RESULT_TOO_LARGE", "CANCELLED", "TIMEOUT",
            "PARTIAL_PUBLICATION", "IO_ERROR");

    private final BooleanSupplier generating;

    public DocViewMcpExecutor(BooleanSupplier generating) {
        this.generating = Objects.requireNonNull(generating, "generating");
    }

    /** Creates the same safe error envelope for provider routing and scheduling failures. */
    public static JsonObject error(String projectPath, String code) {
        String safeCode = ERROR_CODES.contains(code) ? code : "IO_ERROR";
        return errorEnvelope(safeProjectPath(projectPath), null, "unknown", null, safeCode, message(safeCode), List.of(),
                retryable(safeCode), List.of(), null, List.of());
    }

    /** Validates the exact input contract before the provider resolves or schedules a project call. */
    public static void validateArguments(String toolName, JsonObject arguments) {
        if (!TOOLS.contains(toolName)) {
            throw invalid("Unknown Doc View MCP tool");
        }
        if (arguments == null) {
            throw invalid("Tool arguments must be a JSON object");
        }

        Set<String> required = switch (toolName) {
            case STATUS, GENERATE -> Set.of("projectPath");
            case SEARCH -> Set.of("projectPath", "query");
            case GET -> Set.of("projectPath", "apiId", "snapshotId");
            default -> throw invalid("Unknown Doc View MCP tool");
        };
        Set<String> allowed = switch (toolName) {
            case STATUS, GENERATE -> required;
            case SEARCH -> Set.of("projectPath", "query", "module", "framework", "httpMethod", "limit", "cursor");
            case GET -> Set.of("projectPath", "apiId", "snapshotId", "includeMarkdown");
            default -> throw invalid("Unknown Doc View MCP tool");
        };

        for (String key : arguments.keySet()) {
            if (!allowed.contains(key)) {
                throw invalid("Unknown argument: " + key);
            }
        }
        for (String key : required) {
            if (!arguments.has(key) || arguments.get(key).isJsonNull()) {
                throw invalid("Missing required argument: " + key);
            }
        }

        String projectPath = requiredString(arguments, "projectPath", 4096);
        try {
            Path path = Path.of(projectPath);
            if (!path.isAbsolute() || !path.normalize().equals(path)) {
                throw invalid("projectPath must be an absolute normalized path");
            }
        } catch (InvalidPathException invalidPath) {
            throw invalid("projectPath is invalid");
        }

        if (toolName.equals(SEARCH)) {
            String query = requiredString(arguments, "query", 256, false);
            String normalized = java.text.Normalizer.normalize(query, java.text.Normalizer.Form.NFKC)
                    .toLowerCase(Locale.ROOT).strip();
            if (normalized.codePointCount(0, normalized.length()) > 256) {
                throw invalid("query exceeds 256 normalized Unicode code points");
            }
            if (!normalized.isEmpty() && normalized.split("(?U)\\s+").length > 16) {
                throw invalid("query exceeds 16 normalized search terms");
            }
            optionalString(arguments, "module", 4096, true);
            String framework = optionalString(arguments, "framework", 32, false);
            if (framework != null && !FRAMEWORKS.contains(framework)) {
                throw invalid("framework is not a supported exact value");
            }
            String httpMethod = optionalString(arguments, "httpMethod", 16, false);
            if (httpMethod != null && !HTTP_METHODS.contains(httpMethod)) {
                throw invalid("httpMethod is not a supported exact value");
            }
            if (arguments.has("limit")) {
                JsonElement limitElement = arguments.get("limit");
                if (limitElement == null || !limitElement.isJsonPrimitive()
                        || !limitElement.getAsJsonPrimitive().isNumber()) {
                    throw invalid("limit must be an integer between 1 and 100");
                }
                try {
                    String text = limitElement.getAsString();
                    if (!text.matches("-?(0|[1-9][0-9]*)")) {
                        throw invalid("limit must be an integer between 1 and 100");
                    }
                    int limit = Integer.parseInt(text);
                    if (limit < 1 || limit > 100) {
                        throw invalid("limit must be an integer between 1 and 100");
                    }
                } catch (NumberFormatException invalidLimit) {
                    throw invalid("limit must be an integer between 1 and 100");
                }
            }
            String cursor = optionalString(arguments, "cursor", 65_536, false);
            if (cursor != null && cursor.isEmpty()) {
                throw invalid("cursor must not be empty");
            }
        } else if (toolName.equals(GET)) {
            requireSha(arguments, "apiId");
            requireSha(arguments, "snapshotId");
            if (arguments.has("includeMarkdown") && !isBoolean(arguments.get("includeMarkdown"))) {
                throw invalid("includeMarkdown must be a boolean");
            }
        }
    }

    /** Returns a versioned JSON envelope. Schema-invalid arguments are thrown for MCP InvalidParams mapping. */
    public JsonObject execute(Project project, String toolName, JsonObject arguments, ProgressIndicator indicator) {
        Objects.requireNonNull(project, "project");
        Objects.requireNonNull(indicator, "indicator");
        validateArguments(toolName, arguments);
        indicator.checkCanceled();

        String requestedPath = arguments.get("projectPath").getAsString();
        ProjectRoot root = resolveProjectRoot(project, requestedPath);
        if (root == null) {
            return errorEnvelope(requestedPath, null, "unknown", null, "PROJECT_NOT_OPEN",
                    "指定的项目没有在当前 IDEA 实例中打开。", List.of(), false,
                    List.of(), null, List.of());
        }
        if (project.isDisposed()) {
            return errorEnvelope(root.path(), null, "unknown", null, "PROJECT_NOT_OPEN",
                    "指定的项目已经关闭。", List.of(), false,
                    List.of(), null, List.of());
        }

        try {
            JsonObject result = switch (toolName) {
                case STATUS -> status(project, root, indicator);
                case SEARCH -> search(project, root, arguments, indicator);
                case GET -> get(project, root, arguments, indicator);
                case GENERATE -> generate(project, root, indicator);
                default -> throw invalid("Unknown Doc View MCP tool");
            };
            if (jsonBytes(result) > MAX_RESULT_BYTES) {
                return errorEnvelope(root.path(), readString(result, "snapshotId"),
                        readStringOrDefault(result, "freshness", "unknown"), readBooleanOrNull(result, "hasUnsavedDocuments"),
                        "RESULT_TOO_LARGE", "工具结果超过 4 MiB 上限，未返回截断内容。", List.of(), false,
                        List.of(), null, List.of());
            }
            return result;
        } catch (ProcessCanceledException cancelled) {
            throw cancelled;
        } catch (ProjectDocumentQueryException failure) {
            if (failure.code() == ProjectDocumentQueryException.Code.CANCELLED) {
                throw new ProcessCanceledException();
            }
            return queryFailure(root.path(), failure);
        } catch (ProjectDocumentGenerationException failure) {
            String code = switch (failure.code()) {
                case SOURCE_CHANGED -> "SOURCE_CHANGED";
                case TIMEOUT -> "TIMEOUT";
            };
            return errorEnvelope(root.path(), null, "unknown", null, code,
                    message(code), List.of(), true, List.of(), false, List.of());
        } catch (ProjectDocumentIndexNotReadyException | IndexNotReadyException indexing) {
            return errorEnvelope(root.path(), null, "unknown", null, "INDEXING",
                    message("INDEXING"), List.of(), true, List.of(), false, List.of());
        } catch (ProjectDocumentConflictException conflict) {
            return errorEnvelope(root.path(), null, "unknown", null, "CONFLICT",
                    message("CONFLICT"), conflict.conflicts(), false, List.of(), false, List.of());
        } catch (ProjectDocumentPublishException partial) {
            List<String> published = toDocumentPaths(partial.publishedPaths());
            List<String> cleanup = toDocumentPaths(partial.temporaryPaths());
            List<String> failed = partial.failedPath() == null ? List.of() : List.of(partial.failedPath());
            return errorEnvelope(root.path(), null, "unknown", null, "PARTIAL_PUBLICATION",
                    message("PARTIAL_PUBLICATION"), failed, true, published, false, cleanup);
        } catch (ProjectDocumentTemporaryCleanupException cleanupFailure) {
            return errorEnvelope(root.path(), null, "unknown", null, "IO_ERROR",
                    message("IO_ERROR"), List.of(), true, List.of(), false,
                    toDocumentPaths(cleanupFailure.temporaryPaths()));
        } catch (IOException failure) {
            return errorEnvelope(root.path(), null, "unknown", null, "IO_ERROR",
                    message("IO_ERROR"), List.of(), true, List.of(), false, List.of());
        } catch (CancellationException cancelled) {
            throw cancelled;
        } catch (RuntimeException failure) {
            return errorEnvelope(root.path(), null, "unknown", null, "IO_ERROR",
                    message("IO_ERROR"), List.of(), true, List.of(), null, List.of());
        }
    }

    private JsonObject status(Project project, ProjectRoot root, ProgressIndicator indicator) {
        ProjectDocumentIndexStatus indexStatus = ProjectDocumentIndexService.getInstance(project).status(indicator);
        JsonObject data = new JsonObject();
        data.addProperty("root", ProjectDocumentIndexService.DOCUMENT_ROOT);
        data.addProperty("indexState", indexStatus.indexState().value());
        Integer entryCount = indexStatus.indexState() == ProjectDocumentIndexStatus.IndexState.NOT_GENERATED
                ? Integer.valueOf(0) : indexStatus.entryCount();
        addNullable(data, "entryCount", entryCount);
        addNullable(data, "generatedAt", indexStatus.generatedAt());
        data.addProperty("generating", isGenerating());
        data.addProperty("toolContractVersion", TOOL_CONTRACT_VERSION);
        data.addProperty("pluginVersion", DocViewBuildInfo.version());
        return successEnvelope(root.path(), indexStatus.snapshotId(), indexStatus.freshness().value(),
                indexStatus.hasUnsavedDocuments(), data);
    }

    private JsonObject search(Project project, ProjectRoot root, JsonObject arguments, ProgressIndicator indicator) {
        ProjectDocumentQuery query = new ProjectDocumentQuery(
                arguments.get("query").getAsString(),
                optionalString(arguments, "module", 4096, true),
                optionalString(arguments, "framework", 32, false),
                optionalString(arguments, "httpMethod", 16, false),
                arguments.has("limit") ? arguments.get("limit").getAsInt() : null,
                optionalString(arguments, "cursor", 65_536, false));
        ProjectDocumentQueryResult page = ProjectDocumentIndexService.getInstance(project).search(query, indicator);
        JsonObject data = new JsonObject();
        JsonArray items = new JsonArray();
        for (EntryData entry : page.items()) {
            indicator.checkCanceled();
            items.add(entryJson(entry));
        }
        data.add("items", items);
        addNullable(data, "nextCursor", page.nextCursor());
        data.addProperty("totalMatched", page.totalMatched());
        return successEnvelope(root.path(), page.snapshotId(), page.freshness().value(),
                page.hasUnsavedDocuments(), data);
    }

    private JsonObject get(Project project, ProjectRoot root, JsonObject arguments, ProgressIndicator indicator) {
        String apiId = arguments.get("apiId").getAsString();
        String snapshotId = arguments.get("snapshotId").getAsString();
        ProjectDocumentIndexService service = ProjectDocumentIndexService.getInstance(project);
        ProjectDocumentDetailResult detail = service.getDetail(apiId, snapshotId, indicator);
        ProjectDocumentIndexStatus indexStatus = service.status(indicator);
        if (!detail.snapshotId().equals(indexStatus.snapshotId())) {
            throw new ProjectDocumentQueryException(ProjectDocumentQueryException.Code.SNAPSHOT_CHANGED,
                    "The project document snapshot changed during the query");
        }
        JsonObject data = new JsonObject();
        data.add("api", apiJson(detail.detail()));
        if (arguments.has("includeMarkdown") && arguments.get("includeMarkdown").getAsBoolean()) {
            ProjectDocumentMarkdownResult markdown = service.getMarkdown(apiId, snapshotId, indicator);
            if (!markdown.snapshotId().equals(detail.snapshotId())) {
                throw new ProjectDocumentQueryException(ProjectDocumentQueryException.Code.SNAPSHOT_CHANGED,
                        "The project document snapshot changed during the query");
            }
            data.addProperty("markdown", markdown.markdown());
        }
        return successEnvelope(root.path(), detail.snapshotId(), indexStatus.freshness().value(),
                indexStatus.hasUnsavedDocuments(), data);
    }

    private JsonObject generate(Project project, ProjectRoot root, ProgressIndicator indicator) throws IOException {
        ProjectDocumentIndexService indexService = ProjectDocumentIndexService.getInstance(project);
        ProjectDocumentGenerationService.Outcome outcome = ProjectDocumentGenerationService.getInstance(project)
                .generate(indicator);
        ProjectDocumentSnapshot snapshot = outcome.snapshot();
        ProjectDocumentExportResult publication = outcome.publication();

        String freshness = "unknown";
        try {
            ProjectDocumentIndexStatus observed = indexService.status(new EmptyProgressIndicator());
            if (publication.snapshotId().equals(observed.snapshotId())) freshness = observed.freshness().value();
        } catch (RuntimeException unavailableAfterCommit) {
            // Publication already committed. Optional status refresh must not change that outcome.
        }
        String resultOutcome = !publication.previousIndexExisted() ? "generated"
                : publication.changedPaths().isEmpty() ? "unchanged" : "updated";
        JsonObject data = new JsonObject();
        data.addProperty("outcome", resultOutcome);
        data.addProperty("scanned", snapshot.pages().size() + snapshot.skippedCount());
        data.addProperty("created", publication.createdPageCount());
        data.addProperty("updated", publication.updatedPageCount());
        data.addProperty("unchanged", publication.unchangedPageCount());
        data.addProperty("skipped", snapshot.skippedCount());
        addPathList(data, "publishedPaths", "publishedPathsTotal", publication.changedPaths());
        data.add("conflicts", new JsonArray());
        data.addProperty("conflictsTotal", 0);
        data.add("cleanupFailures", new JsonArray());
        data.addProperty("cleanupFailuresTotal", 0);
        return successEnvelope(root.path(), publication.snapshotId(), freshness, snapshot.hasUnsavedDocuments(), data);
    }

    private JsonObject queryFailure(String projectPath, ProjectDocumentQueryException failure) {
        String code = failure.code().name();
        List<String> paths = failure.relativePath() == null ? List.of() : List.of(failure.relativePath());
        if (failure.code() == ProjectDocumentQueryException.Code.INDEX_NOT_FOUND && paths.isEmpty()) {
            paths = List.of("index.json");
        }
        return errorEnvelope(projectPath, null, "unknown", null, code, message(code), paths,
                retryable(code), List.of(), null, List.of());
    }

    private static JsonObject successEnvelope(String projectPath, String snapshotId, String freshness,
                                              Boolean hasUnsavedDocuments, JsonObject data) {
        JsonObject result = new JsonObject();
        result.addProperty("schemaVersion", TOOL_CONTRACT_VERSION);
        result.addProperty("status", "ok");
        result.addProperty("projectPath", projectPath);
        addNullable(result, "snapshotId", snapshotId);
        result.addProperty("freshness", freshness);
        addNullable(result, "hasUnsavedDocuments", hasUnsavedDocuments);
        result.add("data", data);
        result.add("error", JsonNull.INSTANCE);
        return result;
    }

    private static JsonObject errorEnvelope(String projectPath, String snapshotId, String freshness,
                                            Boolean hasUnsavedDocuments, String code, String message,
                                            Collection<String> relativePaths, boolean retryable,
                                            Collection<String> publishedPaths, Boolean indexCommitted,
                                            Collection<String> cleanupFailures) {
        JsonObject error = new JsonObject();
        error.addProperty("code", code);
        error.addProperty("message", message);
        addPathList(error, "relativePaths", "relativePathsTotal", relativePaths);
        error.addProperty("retryable", retryable);
        JsonObject details = new JsonObject();
        addPathList(details, "publishedPaths", "publishedPathsTotal", publishedPaths);
        addNullable(details, "indexCommitted", indexCommitted);
        addPathList(details, "cleanupFailures", "cleanupFailuresTotal", cleanupFailures);
        error.add("details", details);

        JsonObject result = new JsonObject();
        result.addProperty("schemaVersion", TOOL_CONTRACT_VERSION);
        result.addProperty("status", "error");
        addNullable(result, "projectPath", safeProjectPath(projectPath));
        addNullable(result, "snapshotId", snapshotId);
        result.addProperty("freshness", freshness == null ? "unknown" : freshness);
        addNullable(result, "hasUnsavedDocuments", hasUnsavedDocuments);
        result.add("data", JsonNull.INSTANCE);
        result.add("error", error);
        return result;
    }

    private static JsonObject entryJson(EntryData entry) {
        JsonObject json = new JsonObject();
        json.addProperty("apiId", entry.apiId());
        json.add("identity", identityJson(entry.identity()));
        json.addProperty("name", entry.name());
        json.addProperty("description", entry.description());
        json.addProperty("framework", entry.framework());
        addNullable(json, "httpMethod", entry.httpMethod());
        addNullable(json, "httpPath", entry.httpPath());
        json.add("tags", stringArray(entry.tags()));
        json.add("source", sourceJson(entry.source()));
        json.add("markdown", fileJson(entry.markdown()));
        json.add("detail", fileJson(entry.detail()));
        return json;
    }

    private static JsonObject apiJson(ApiData api) {
        JsonObject json = new JsonObject();
        json.addProperty("apiId", api.apiId());
        json.add("identity", identityJson(api.identity()));
        json.addProperty("name", api.name());
        json.addProperty("description", api.description());
        json.addProperty("framework", api.framework());
        addNullable(json, "httpMethod", api.httpMethod());
        addNullable(json, "httpPath", api.httpPath());
        json.add("tags", stringArray(api.tags()));
        json.add("source", sourceJson(api.source()));
        JsonArray headers = new JsonArray();
        for (HeaderData header : api.headers()) {
            JsonObject item = new JsonObject();
            item.addProperty("name", header.name());
            item.addProperty("description", header.description());
            addNullable(item, "required", header.required());
            addNullable(item, "example", header.example());
            headers.add(item);
        }
        json.add("headers", headers);
        JsonArray parameters = new JsonArray();
        for (FieldData field : api.requestParameters()) {
            parameters.add(fieldJson(field));
        }
        json.add("requestParameters", parameters);
        json.add("requestBody", api.requestBody() == null ? JsonNull.INSTANCE : bodyJson(api.requestBody()));
        json.add("responseBody", api.responseBody() == null ? JsonNull.INSTANCE : bodyJson(api.responseBody()));
        JsonObject examples = new JsonObject();
        addNullable(examples, "request", api.examples().request());
        addNullable(examples, "response", api.examples().response());
        json.add("examples", examples);
        json.addProperty("remark", api.remark());
        return json;
    }

    private static JsonObject identityJson(IdentityData identity) {
        JsonObject json = new JsonObject();
        json.addProperty("module", identity.module());
        json.addProperty("qualifiedClass", identity.qualifiedClass());
        json.addProperty("methodName", identity.methodName());
        json.add("parameterTypes", stringArray(identity.parameterTypes()));
        json.addProperty("fullSignature", identity.fullSignature());
        json.addProperty("canonicalForm", identity.canonicalForm());
        return json;
    }

    private static JsonObject sourceJson(SourceData source) {
        JsonObject json = new JsonObject();
        json.addProperty("path", source.path());
        addNullable(json, "line", source.line());
        json.addProperty("capturedContentSha256", source.capturedContentSha256());
        json.addProperty("basis", source.basis());
        return json;
    }

    private static JsonObject fileJson(FileData file) {
        JsonObject json = new JsonObject();
        json.addProperty("path", file.path());
        json.addProperty("sha256", file.sha256());
        return json;
    }

    private static JsonObject fieldJson(FieldData field) {
        JsonObject json = new JsonObject();
        json.addProperty("name", field.name());
        json.addProperty("type", field.type());
        json.addProperty("description", field.description());
        addNullable(json, "required", field.required());
        addNullable(json, "example", field.example());
        addNullable(json, "since", field.since());
        addNullable(json, "version", field.version());
        json.addProperty("isCollection", field.isCollection());
        json.addProperty("isMap", field.isMap());
        JsonArray children = new JsonArray();
        for (FieldData child : field.children()) {
            children.add(fieldJson(child));
        }
        json.add("children", children);
        return json;
    }

    private static JsonObject bodyJson(BodyData body) {
        JsonObject json = new JsonObject();
        json.addProperty("type", body.type());
        JsonArray fields = new JsonArray();
        for (FieldData field : body.fields()) {
            fields.add(fieldJson(field));
        }
        json.add("fields", fields);
        return json;
    }

    private static JsonArray stringArray(Collection<String> values) {
        JsonArray array = new JsonArray();
        for (String value : values) {
            array.add(value);
        }
        return array;
    }

    private static void addPathList(JsonObject target, String arrayName, String totalName,
                                    Collection<String> paths) {
        List<String> safe = toDocumentPaths(paths);
        JsonArray array = new JsonArray();
        for (int index = 0; index < Math.min(MAX_DISPLAY_PATHS, safe.size()); index++) {
            array.add(safe.get(index));
        }
        target.add(arrayName, array);
        target.addProperty(totalName, paths == null ? 0 : paths.size());
    }

    private static List<String> toDocumentPaths(Collection<String> paths) {
        if (paths == null || paths.isEmpty()) {
            return List.of();
        }
        LinkedHashSet<String> safe = new LinkedHashSet<>();
        for (String path : paths) {
            String normalized = toDocumentPath(path);
            if (normalized != null) {
                safe.add(normalized);
            }
        }
        return List.copyOf(safe);
    }

    private static String toDocumentPath(String rawPath) {
        if (rawPath == null || rawPath.isBlank() || rawPath.indexOf('\\') >= 0 || rawPath.indexOf('\0') >= 0) {
            return null;
        }
        String relative = rawPath;
        String prefix = ProjectDocumentIndexService.DOCUMENT_ROOT + "/";
        if (relative.startsWith(prefix)) {
            relative = relative.substring(prefix.length());
        }
        if (relative.startsWith("/") || relative.matches("^[A-Za-z]:.*")) {
            return null;
        }
        try {
            Path path = Path.of(relative);
            if (path.isAbsolute() || !path.normalize().equals(path)) {
                return null;
            }
            String normalized = path.toString().replace('\\', '/');
            if (normalized.isBlank() || normalized.startsWith("../") || normalized.equals("..")
                    || normalized.contains("/../") || normalized.contains("//") || normalized.endsWith("/")) {
                return null;
            }
            if (normalized.codePointCount(0, normalized.length()) > MAX_PATH_CODE_POINTS) {
                return null;
            }
            return normalized;
        } catch (InvalidPathException invalid) {
            return null;
        }
    }

    private static ProjectRoot resolveProjectRoot(Project project, String requestedPath) {
        if (project.isDisposed() || project.getBasePath() == null) {
            return null;
        }
        try {
            Path expected = Path.of(project.getBasePath()).toRealPath();
            Path requested = Path.of(requestedPath).toRealPath();
            if (!expected.equals(requested)) {
                return null;
            }
            return new ProjectRoot(expected.toString());
        } catch (IOException | InvalidPathException invalidPath) {
            return null;
        }
    }

    private boolean isGenerating() {
        try {
            return generating.getAsBoolean();
        } catch (CancellationException cancelled) {
            throw cancelled;
        } catch (RuntimeException ignored) {
            return false;
        }
    }

    private static int jsonBytes(JsonObject value) {
        return value.toString().getBytes(StandardCharsets.UTF_8).length;
    }

    private static String readString(JsonObject json, String key) {
        return json.has(key) && json.get(key).isJsonPrimitive() && json.getAsJsonPrimitive(key).isString()
                ? json.get(key).getAsString() : null;
    }

    private static String safeProjectPath(String projectPath) {
        if (projectPath == null || projectPath.isBlank() || projectPath.indexOf('\0') >= 0
                || projectPath.codePointCount(0, projectPath.length()) > 4096) {
            return null;
        }
        return projectPath;
    }

    private static String readStringOrDefault(JsonObject json, String key, String fallback) {
        String value = readString(json, key);
        return value == null ? fallback : value;
    }

    private static Boolean readBooleanOrNull(JsonObject json, String key) {
        return json.has(key) && json.get(key).isJsonPrimitive() && json.getAsJsonPrimitive(key).isBoolean()
                ? json.get(key).getAsBoolean() : null;
    }

    private static String requiredString(JsonObject object, String name, int maximumCodePoints) {
        return requiredString(object, name, maximumCodePoints, true);
    }

    private static String requiredString(JsonObject object, String name, int maximumCodePoints,
                                         boolean requireNonBlank) {
        JsonElement element = object.get(name);
        if (!isString(element)) {
            throw invalid(name + " must be a string");
        }
        String value = element.getAsString();
        if (value.codePointCount(0, value.length()) > maximumCodePoints) {
            throw invalid(name + " exceeds its maximum length");
        }
        if ((requireNonBlank && value.isBlank()) || value.indexOf('\0') >= 0) {
            throw invalid(name + (requireNonBlank ? " must not be blank or contain NUL" : " must not contain NUL"));
        }
        return value;
    }

    private static String optionalString(JsonObject object, String name, int maximumCodePoints,
                                         boolean requireNonBlank) {
        if (!object.has(name)) {
            return null;
        }
        JsonElement element = object.get(name);
        if (!isString(element)) {
            throw invalid(name + " must be a string");
        }
        String value = element.getAsString();
        if (value.codePointCount(0, value.length()) > maximumCodePoints || value.indexOf('\0') >= 0
                || (requireNonBlank && value.isBlank())) {
            throw invalid(name + " is invalid");
        }
        return value;
    }

    private static void requireSha(JsonObject object, String name) {
        String value = requiredString(object, name, 64);
        if (!SHA_256.matcher(value).matches()) {
            throw invalid(name + " must be a lowercase SHA-256 identifier");
        }
    }

    private static boolean isString(JsonElement value) {
        return value != null && value.isJsonPrimitive() && value.getAsJsonPrimitive().isString();
    }

    private static boolean isBoolean(JsonElement value) {
        return value != null && value.isJsonPrimitive() && value.getAsJsonPrimitive().isBoolean();
    }

    private static IllegalArgumentException invalid(String message) {
        return new IllegalArgumentException(message);
    }

    private static void addNullable(JsonObject object, String name, String value) {
        object.add(name, value == null ? JsonNull.INSTANCE : new JsonPrimitive(value));
    }

    private static void addNullable(JsonObject object, String name, Boolean value) {
        object.add(name, value == null ? JsonNull.INSTANCE : new JsonPrimitive(value));
    }

    private static void addNullable(JsonObject object, String name, Integer value) {
        object.add(name, value == null ? JsonNull.INSTANCE : new JsonPrimitive(value));
    }

    private static String message(String code) {
        return switch (code) {
            case "PROJECT_NOT_OPEN" -> "指定的项目没有在当前 IDEA 实例中打开。";
            case "INDEX_NOT_FOUND" -> "尚未生成项目文档库。请先显式调用生成工具。";
            case "UNSUPPORTED_SCHEMA" -> "项目文档库版本不受当前插件支持。";
            case "INDEX_INVALID" -> "项目文档索引无效，无法安全读取。";
            case "INDEX_INCONSISTENT" -> "文档内容与索引记录不一致。";
            case "API_NOT_FOUND" -> "指定快照中不存在该接口。";
            case "CURSOR_STALE" -> "搜索游标无效或所属快照已经变化。";
            case "SNAPSHOT_CHANGED" -> "项目文档快照已经变化，请重新搜索后读取详情。";
            case "INDEXING" -> "IDEA 正在索引项目，请稍后重试生成。";
            case "BUSY" -> "项目文档任务正在运行，请稍后重试。";
            case "CONFLICT" -> "目标文档与现有内容冲突，未覆盖冲突文件。";
            case "SOURCE_CHANGED" -> "扫描期间源码或生成设置发生变化，请重新生成。";
            case "LIMIT_EXCEEDED" -> "文档库或接口内容超过支持的大小限制。";
            case "RESULT_TOO_LARGE" -> "工具结果超过 4 MiB 上限，未返回截断内容。";
            case "CANCELLED" -> "任务已取消。";
            case "TIMEOUT" -> "任务超过允许的执行时间。";
            case "PARTIAL_PUBLICATION" -> "生成在部分文件发布后失败；请检查已发布路径并重新生成。";
            case "INVALID_ARGUMENT" -> "工具参数无效。";
            default -> "项目文档操作失败，文件状态未被推断。";
        };
    }

    private static boolean retryable(String code) {
        return switch (code) {
            case "BUSY", "INDEXING", "SOURCE_CHANGED", "CANCELLED", "TIMEOUT", "PARTIAL_PUBLICATION", "IO_ERROR" -> true;
            default -> false;
        };
    }

    private record ProjectRoot(String path) {}

}
