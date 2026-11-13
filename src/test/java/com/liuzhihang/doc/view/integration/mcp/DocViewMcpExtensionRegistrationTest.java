package com.liuzhihang.doc.view.integration.mcp;

import junit.framework.TestCase;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.stream.Collectors;
import javax.xml.parsers.DocumentBuilderFactory;
import com.intellij.mcpserver.McpTool;

public class DocViewMcpExtensionRegistrationTest extends TestCase {
    public void testOnlyOptionalDescriptorLoadsProvider() throws Exception {
        var doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse("src/main/resources/META-INF/plugin.xml");
        var deps = doc.getElementsByTagName("depends"); boolean found = false;
        for (int i=0;i<deps.getLength();i++) {
            var e=(org.w3c.dom.Element)deps.item(i);
            if ("com.intellij.mcpServer".equals(e.getTextContent())) {
                assertEquals("true",e.getAttribute("optional")); assertEquals("com.liuzhihang.doc-view-mcp.xml",e.getAttribute("config-file")); found=true;
            }
        }
        assertTrue(found);
        assertFalse(Files.readString(Path.of("src/main/resources/META-INF/plugin.xml")).contains("integration.mcp"));
        assertTrue(Files.readString(Path.of("src/main/resources/META-INF/com.liuzhihang.doc-view-mcp.xml")).contains("mcpServer.mcpToolsProvider"));
    }
    public void testToolsDescribeOneProjectPropertyAndSafeAnnotations() {
        var tools = new DocViewMcpToolsProvider().getTools();
        assertEquals(Set.of("doc_view_status","doc_view_search","doc_view_get","doc_view_generate"),tools.stream()
                .map(McpTool::getDescriptor).map(d->d.getName()).collect(Collectors.toSet()));
        for(var tool:tools) {
            var d=tool.getDescriptor(); assertTrue(d.getInputSchema().getRequiredProperties().contains("projectPath"));
            assertEquals(1,d.getInputSchema().getPropertiesSchema().keySet().stream().filter("projectPath"::equals).count());
            boolean write = "doc_view_generate".equals(d.getName());
            assertEquals(!write,d.getAnnotations().getReadOnlyHint().booleanValue()); assertEquals(write,d.getAnnotations().getDestructiveHint().booleanValue());
            assertNull(d.getOutputSchema());
        }
    }
}
