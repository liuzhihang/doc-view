package com.liuzhihang.doc.view.contract;

import com.intellij.testFramework.fixtures.LightJavaCodeInsightFixtureTestCase;
import com.liuzhihang.doc.view.DocViewBundle;
import icons.DocViewIcons;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class PluginIconContractTest extends LightJavaCodeInsightFixtureTestCase {

    public void testVisibleDescriptorActionsUseResolvablePlatformIcons() throws Exception {
        Document descriptor = loadPluginDescriptor();
        for (String tag : new String[]{"action", "group"}) {
            NodeList elements = descriptor.getElementsByTagName(tag);
            for (int index = 0; index < elements.getLength(); index++) {
                Element element = (Element) elements.item(index);
                String icon = element.getAttribute("icon");
                if (icon.isBlank()) {
                    continue;
                }
                assertTrue(tag + " " + element.getAttribute("id")
                                + " must use a platform AllIcons field: " + icon,
                        icon.startsWith("AllIcons."));
                assertPlatformIconExists(icon);
            }
        }

        Element toolWindow = (Element) descriptor.getElementsByTagName("toolWindow").item(0);
        assertEquals("Tool Window branding must use its dedicated size variant",
                "DocViewIcons.DOC_VIEW_TOOL_WINDOW", toolWindow.getAttribute("icon"));
    }

    public void testToolWindowExportsShareOneNamedDropdownAndKeepActionIds() throws Exception {
        Document descriptor = loadPluginDescriptor();
        Element exportGroup = findById(descriptor, "group", "liuzhihang.doc.tool.window.toolbar.action.export.menu");
        assertEquals("true", exportGroup.getAttribute("popup"));
        assertEquals("AllIcons.ToolbarDecorator.Export", exportGroup.getAttribute("icon"));
        Element toolbar = findById(descriptor, "group", "liuzhihang.doc.tool.window.toolbar.action");
        assertSame("The Export dropdown must be a direct Tool Window toolbar item",
                toolbar, exportGroup.getParentNode());

        Set<String> toolbarActionChildren = new LinkedHashSet<>();
        for (Node child = toolbar.getFirstChild(); child != null; child = child.getNextSibling()) {
            if (child instanceof Element element && "action".equals(element.getTagName())) {
                toolbarActionChildren.add(element.getAttribute("id"));
            }
        }
        assertFalse("Individual export actions must not remain as duplicate toolbar buttons",
                toolbarActionChildren.contains("liuzhihang.doc.tool.window.toolbar.action.export"));
        assertFalse("Project export must be available through the same dropdown",
                toolbarActionChildren.contains("liuzhihang.doc.tool.window.toolbar.action.project.export"));

        Set<String> children = new LinkedHashSet<>();
        for (Node child = exportGroup.getFirstChild(); child != null; child = child.getNextSibling()) {
            if (child instanceof Element element && "action".equals(element.getTagName())) {
                children.add(element.getAttribute("id"));
            }
        }
        assertEquals(Set.of("liuzhihang.doc.tool.window.toolbar.action.export",
                        "liuzhihang.doc.tool.window.toolbar.action.project.export"), children);
        Element markdownAction = findById(descriptor, "action", "liuzhihang.doc.tool.window.toolbar.action.export");
        Element projectAction = findById(descriptor, "action", "liuzhihang.doc.tool.window.toolbar.action.project.export");
        assertFalse(DocViewBundle.message("action.export.markdown").isBlank());
        assertFalse(DocViewBundle.message("action.export.project.docs").isBlank());
        assertEquals(DocViewBundle.message("action.export.markdown"), markdownAction.getAttribute("text"));
        assertEquals(DocViewBundle.message("action.export.project.docs"), projectAction.getAttribute("text"));
        assertEquals("AllIcons.ToolbarDecorator.Export", markdownAction.getAttribute("icon"));
        assertEquals("AllIcons.ToolbarDecorator.Export", projectAction.getAttribute("icon"));
    }

    public void testOnlyBrandIconsRemainInThePluginIconHolder() {
        Set<String> fields = Arrays.stream(DocViewIcons.class.getDeclaredFields())
                .map(java.lang.reflect.Field::getName)
                .collect(java.util.stream.Collectors.toSet());
        assertEquals(Set.of("DOC_VIEW", "DOC_VIEW_TOOL_WINDOW", "DOC_VIEW_GUTTER"), fields);
    }

    public void testBrandIconAssetsMatchPlatformSizesAndThemes() throws Exception {
        assertSvgSize("icons/docView.svg", 16, 16);
        assertSvgSize("icons/docView_dark.svg", 16, 16);
        assertSvgSize("icons/docViewToolWindow.svg", 13, 13);
        assertSvgSize("icons/docViewToolWindow_dark.svg", 13, 13);
        assertSvgSize("icons/docViewGutter.svg", 12, 12);
        assertSvgSize("icons/docViewGutter_dark.svg", 12, 12);
        assertSvgSize("icons/expui/docViewToolWindow@20x20.svg", 20, 20);
        assertSvgSize("icons/expui/docViewToolWindow@20x20_dark.svg", 20, 20);
        assertSvgSize("icons/expui/docViewToolWindow.svg", 16, 16);
        assertSvgSize("icons/expui/docViewToolWindow_dark.svg", 16, 16);
        assertSvgSize("icons/expui/docViewGutter.svg", 14, 14);
        assertSvgSize("icons/expui/docViewGutter_dark.svg", 14, 14);
        assertSvgSize("META-INF/pluginIcon.svg", 40, 40);
    }

    public void testNewUiBrandIconsAreMappedToLegacyResources() throws Exception {
        String mappings = readResource("DocViewIconMappings.json");
        assertTrue(mappings.contains("\"icons\""));
        assertTrue(mappings.contains("\"expui\""));
        assertTrue(mappings.contains("\"docViewToolWindow.svg\": \"icons/docViewToolWindow.svg\""));
        assertTrue(mappings.contains("\"docViewGutter.svg\": \"icons/docViewGutter.svg\""));
        assertFalse("Tool Window size and theme variants are selected from the base mapping",
                mappings.contains("@20x20") || mappings.contains("_dark"));
        Element mapper = (Element) loadPluginDescriptor().getElementsByTagName("iconMapper").item(0);
        assertNotNull("The mapping file must be registered with the public iconMapper extension", mapper);
        assertEquals("DocViewIconMappings.json", mapper.getAttribute("mappingFile"));
    }

    private Document loadPluginDescriptor() throws Exception {
        try (InputStream descriptor = getClass().getClassLoader().getResourceAsStream("META-INF/plugin.xml")) {
            assertNotNull("The plugin descriptor must be available to the test", descriptor);
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            return factory.newDocumentBuilder().parse(descriptor);
        }
    }

    private Element findById(Document document, String tag, String id) {
        NodeList elements = document.getElementsByTagName(tag);
        for (int index = 0; index < elements.getLength(); index++) {
            Element element = (Element) elements.item(index);
            if (id.equals(element.getAttribute("id"))) {
                return element;
            }
        }
        fail("Missing " + tag + " descriptor: " + id);
        throw new AssertionError("Unreachable");
    }

    private void assertPlatformIconExists(String iconPath) throws Exception {
        String[] parts = iconPath.split("\\.");
        assertEquals("An AllIcons reference must include holder and field: " + iconPath, 3, parts.length);
        Class<?> holder = Class.forName("com.intellij.icons.AllIcons$" + parts[1]);
        assertNotNull("Missing AllIcons field " + iconPath, holder.getField(parts[2]));
    }

    private void assertSvgSize(String resourcePath, int expectedWidth, int expectedHeight) throws Exception {
        String svg = readResource(resourcePath);
        Matcher width = Pattern.compile("\\bwidth=\"(\\d+)\"").matcher(svg);
        Matcher height = Pattern.compile("\\bheight=\"(\\d+)\"").matcher(svg);
        assertTrue("Missing SVG width in " + resourcePath, width.find());
        assertTrue("Missing SVG height in " + resourcePath, height.find());
        assertEquals("Unexpected SVG width for " + resourcePath, expectedWidth, Integer.parseInt(width.group(1)));
        assertEquals("Unexpected SVG height for " + resourcePath, expectedHeight, Integer.parseInt(height.group(1)));
    }

    private String readResource(String resourcePath) throws Exception {
        try (InputStream input = getClass().getClassLoader().getResourceAsStream(resourcePath)) {
            assertNotNull("Missing icon resource: " + resourcePath, input);
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
