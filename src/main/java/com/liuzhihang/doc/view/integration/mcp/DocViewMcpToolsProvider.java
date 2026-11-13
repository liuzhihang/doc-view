package com.liuzhihang.doc.view.integration.mcp;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.intellij.mcpserver.*;
import com.intellij.openapi.project.ProjectManager;
import com.liuzhihang.doc.view.service.mcp.DocViewMcpExecutor;
import io.modelcontextprotocol.kotlin.sdk.types.ToolAnnotations;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import kotlinx.coroutines.CancellableContinuationKt;
import kotlinx.serialization.json.Json;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Loaded only by the optional IDEA MCP descriptor, inside the Doc View plugin. */
public final class DocViewMcpToolsProvider implements McpToolsProvider {
    @Override public List<McpTool> getTools() {
        List<McpTool> tools = new ArrayList<>();
        try (var stream = DocViewMcpToolsProvider.class.getResourceAsStream("/mcp/tool-contracts.json")) {
            if (stream == null) throw new IllegalStateException("Missing Doc View MCP contract");
            var contract = JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
            for (var value : contract.getAsJsonArray("tools")) {
                JsonObject tool = value.getAsJsonObject();
                if (!Set.of(DocViewMcpExecutor.STATUS, DocViewMcpExecutor.SEARCH, DocViewMcpExecutor.GET,
                        DocViewMcpExecutor.GENERATE).contains(tool.get("name").getAsString())) {
                    throw new IllegalStateException("Unsupported Doc View MCP contract tool");
                }
                tools.add(new Tool(tool));
            }
        } catch (java.io.IOException failure) { throw new IllegalStateException("Cannot read Doc View MCP contract"); }
        return List.copyOf(tools);
    }
    static McpToolCallResult result(JsonObject result) {
        return new McpToolCallResult(new McpToolCallResultContent[]{new McpToolCallResultContent.Text(result.toString())},
                null, "error".equals(result.get("status").getAsString()));
    }
    static McpToolCallResult error(JsonObject arguments, String code) {
        String path = arguments.has("projectPath") && arguments.get("projectPath").isJsonPrimitive()
                && arguments.getAsJsonPrimitive("projectPath").isString() ? arguments.get("projectPath").getAsString() : null;
        return result(DocViewMcpExecutor.error(path, code));
    }
    private static final class Tool implements McpTool {
        private final McpToolDescriptor descriptor;
        Tool(JsonObject contract) {
            String name = contract.get("name").getAsString();
            var schema = contract.getAsJsonObject("inputSchema");
            var properties = (kotlinx.serialization.json.JsonObject) Json.Default.parseToJsonElement(schema.get("properties").toString());
            var required = new LinkedHashSet<String>();
            schema.getAsJsonArray("required").forEach(value -> required.add(value.getAsString()));
            boolean readOnly = !DocViewMcpExecutor.GENERATE.equals(name);
            String title = switch (name) {
                case "doc_view_status" -> "Project documentation status";
                case "doc_view_search" -> "Search project documentation";
                case "doc_view_get" -> "Read interface documentation";
                default -> "Generate or update project documentation";
            };
            String description = readOnly
                    ? "Reads the existing Doc View Markdown/JSON snapshot without scanning source or generating files. Preserve snapshotId and freshness."
                    : "Explicitly generates or updates docs/doc-view from project source. Writes managed files, preserves conflicts, and may report partial publication. Use only when the user requests generation or updates.";
            descriptor = new McpToolDescriptor(name, title, description, description,
                    new McpToolCategory("Doc View", "com.liuzhihang.doc.view", false, false, "Doc View", "Project API documentation"),
                    "com.liuzhihang.doc.view." + name,
                    new McpToolSchema(properties, required, Map.of(), "#/$defs"), null,
                    new ToolAnnotations(title, readOnly, !readOnly, true, false));
        }
        @Override public McpToolDescriptor getDescriptor() { return descriptor; }
        @Override public Object call(kotlinx.serialization.json.JsonObject args, Continuation<? super McpToolCallResult> continuation) {
            JsonObject parameters = JsonParser.parseString(args.toString()).getAsJsonObject();
            DocViewMcpProjectScope.Selection selection;
            try {
                selection = DocViewMcpProjectScope.resolve(parameters,
                        McpCallInfoKt.getProjectOrNull(continuation.getContext()), ProjectManager.getInstance().getOpenProjects());
                DocViewMcpExecutor.validateArguments(descriptor.getName(), selection.arguments());
            } catch (IllegalArgumentException rejected) {
                String code = "PROJECT_NOT_OPEN".equals(rejected.getMessage()) ? "PROJECT_NOT_OPEN" : "INVALID_ARGUMENT";
                return error(parameters, code);
            }
            return CancellableContinuationKt.suspendCancellableCoroutine(cancellable -> {
                selection.project().getService(DocViewMcpTaskService.class)
                        .submit(descriptor.getName(), selection.arguments(), cancellable);
                return Unit.INSTANCE;
            }, continuation);
        }
    }
}
