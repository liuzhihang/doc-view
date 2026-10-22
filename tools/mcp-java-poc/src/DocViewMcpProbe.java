import com.google.gson.Gson;
import com.intellij.mcpserver.McpTool;
import com.intellij.mcpserver.McpToolCallResult;
import com.intellij.mcpserver.McpToolCallResultContent;
import com.intellij.mcpserver.McpToolset;
import com.intellij.mcpserver.annotations.McpDescription;
import com.intellij.mcpserver.impl.util.ToolsetReflection_utilKt;
import kotlin.ResultKt;
import kotlin.coroutines.Continuation;
import kotlin.coroutines.CoroutineContext;
import kotlin.coroutines.EmptyCoroutineContext;
import kotlin.coroutines.intrinsics.IntrinsicsKt;
import kotlinx.serialization.json.Json;
import kotlinx.serialization.json.JsonObject;

import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/** Isolated diagnostic harness. Never packaged as a production Doc View tool. */
public final class DocViewMcpProbe {
    public static final class TextTools implements McpToolset {
        @com.intellij.mcpserver.annotations.McpTool(name = "doc_view_java_probe")
        @McpDescription(description = "Returns fictional API documentation for a Java interoperability experiment.")
        public McpToolCallResultContent.Text describeDemo(@McpDescription(description = "Fictional interface name") String query,
                                                        @McpDescription(description = "Positive sample limit") int limit) {
            if (limit < 1 || limit > 5) throw new IllegalArgumentException("limit must be 1..5");
            return new McpToolCallResultContent.Text("# " + query + "\nlimit=" + limit);
        }
    }

    public static final class RawStringTools implements McpToolset {
        @com.intellij.mcpserver.annotations.McpTool(name = "doc_view_java_string_probe")
        public String describeString() { return "# demo-api"; }
    }

    public record PlainResult(String title) {}

    public static final class DtoTools implements McpToolset {
        @com.intellij.mcpserver.annotations.McpTool(name = "doc_view_java_dto_probe")
        @McpDescription(description = "Checks the unsupported plain Java DTO boundary.")
        public PlainResult describeDto() {
            return new PlainResult("demo-api");
        }
    }

    public static void main(String[] args) throws Exception {
        inspectBoundary("raw_java_string", new RawStringTools());
        inspectBoundary("plain_java_dto", new DtoTools());
        try {
            McpTool tool = ToolsetReflection_utilKt.asTools(new TextTools(), Json.Default).get(0);
            var schema = tool.getDescriptor().getInputSchema();
            emit("schema", "passed", Map.of("name", tool.getDescriptor().getName(),
                    "properties", schema.getPropertiesSchema().toString(),
                    "required", schema.getRequiredProperties().toString(),
                    "output", tool.getDescriptor().getOutputSchema() == null ? "<none>" : tool.getDescriptor().getOutputSchema().prettyPrint()));
            if (!schema.getPropertiesSchema().containsKey("query") || !schema.getPropertiesSchema().containsKey("limit")) {
                throw new AssertionError("Java parameter names were not preserved");
            }
            McpToolCallResult result = call(tool, "{\"query\":\"demo-api\",\"limit\":2}");
            if (result.isError() || result.getContent().length != 1
                    || !(result.getContent()[0] instanceof McpToolCallResultContent.Text text)
                    || !"# demo-api\nlimit=2".equals(text.getText())) {
                throw new AssertionError("Unexpected text result: " + result);
            }
            emit("text_call", "passed", Map.of("result", result.toString()));
            reject(tool, "missing_argument", "{\"query\":\"demo-api\"}");
            reject(tool, "wrong_type", "{\"query\":\"demo-api\",\"limit\":\"invalid\"}");
            reject(tool, "tool_validation", "{\"query\":\"demo-api\",\"limit\":0}");
        } catch (Throwable failure) {
            emit("java_text_interop", "failed", failure(failure));
            System.exit(1);
        }

        emit("ide_registration_and_lifecycle", "unverified", Map.of("reason", "Standalone JVM is not an IDE MCP service"));
    }

    private static void inspectBoundary(String stage, McpToolset toolset) {
        try {
            McpTool dto = ToolsetReflection_utilKt.asTools(toolset, Json.Default).get(0);
            McpToolCallResult result = call(dto, "{}");
            emit(stage, "accepted", Map.of("result", result.toString()));
        } catch (Throwable boundary) {
            emit(stage, "rejected", failure(boundary));
        }
    }

    private static void reject(McpTool tool, String stage, String args) throws Exception {
        try {
            McpToolCallResult result = call(tool, args);
            if (result.isError()) {
                emit(stage, "rejected", Map.of("result", result.toString()));
                return;
            }
        } catch (Exception expected) {
            emit(stage, "rejected", failure(expected));
            return;
        }
        throw new AssertionError(stage + " unexpectedly succeeded");
    }

    private static McpToolCallResult call(McpTool tool, String json) throws Exception {
        ResultContinuation continuation = new ResultContinuation();
        Object result = tool.call((JsonObject) Json.Default.parseToJsonElement(json), continuation);
        if (result == IntrinsicsKt.getCOROUTINE_SUSPENDED()) {
            if (!continuation.ready.await(5, TimeUnit.SECONDS)) throw new IllegalStateException("Probe call timed out");
            result = continuation.result;
            ResultKt.throwOnFailure(result);
        }
        return (McpToolCallResult) result;
    }

    private static final class ResultContinuation implements Continuation<McpToolCallResult> {
        private final CountDownLatch ready = new CountDownLatch(1);
        private volatile Object result;
        @Override public CoroutineContext getContext() { return EmptyCoroutineContext.INSTANCE; }
        @Override public void resumeWith(Object value) { result = value; ready.countDown(); }
    }

    private static Map<String, String> failure(Throwable error) {
        return Map.of("type", error.getClass().getName(), "message", String.valueOf(error.getMessage()));
    }

    private static void emit(String stage, String status, Map<String, ?> details) {
        System.out.println("POC\t" + new Gson().toJson(Map.of("stage", stage, "status", status, "details", details)));
    }
}
