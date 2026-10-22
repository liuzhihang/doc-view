package org.docview.mcp.poc;

import com.intellij.mcpserver.McpToolCallResultContent;
import com.intellij.mcpserver.McpToolset;
import com.intellij.mcpserver.annotations.McpDescription;

/** Test-only extension. It accepts and returns fictional strings and never reads project data. */
public final class DocViewMcpTestToolset implements McpToolset {
    @com.intellij.mcpserver.annotations.McpTool(name = "doc_view_java_poc_describe_demo")
    @McpDescription(description = "Returns a fixed fictional API example for MCP registration testing.")
    public McpToolCallResultContent.Text describeDemo(
            @McpDescription(description = "Fictional API name") String query,
            @McpDescription(description = "Small sample limit from 1 to 5") int limit) {
        if (limit < 1 || limit > 5) {
            throw new IllegalArgumentException("limit must be between 1 and 5");
        }
        return new McpToolCallResultContent.Text("# " + query + "\nlimit=" + limit);
    }
}
