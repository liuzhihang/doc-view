package com.liuzhihang.doc.view.contract;

import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.testFramework.LightPlatformTestCase;
import com.liuzhihang.doc.view.DocViewBundle;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;

public final class ProjectDocumentLibraryUiTest extends LightPlatformTestCase {

    public void testExportMenuContainsFourProjectAndMarkdownActionsWithoutAddingToolbarControls() throws Exception {
        Document descriptor = loadPluginDescriptor();
        Element exportMenu = findById(descriptor, "group", "liuzhihang.doc.tool.window.toolbar.action.export.menu");
        assertEquals(List.of(
                        "action:liuzhihang.doc.tool.window.toolbar.action.export",
                        "action:liuzhihang.doc.tool.window.toolbar.action.project.export",
                        "action:liuzhihang.doc.tool.window.toolbar.action.project.open",
                        "action:liuzhihang.doc.tool.window.toolbar.action.project.search"),
                directEntries(exportMenu));

        Element toolbar = findById(descriptor, "group", "liuzhihang.doc.tool.window.toolbar.action");
        assertEquals("Project document operations stay inside the existing Export dropdown",
                List.of(
                        "action:liuzhihang.doc.tool.window.toolbar.action.refresh",
                        "separator",
                        "group:liuzhihang.doc.tool.window.toolbar.action.export.menu",
                        "action:liuzhihang.doc.tool.window.toolbar.action.upload",
                        "separator",
                        "action:liuzhihang.doc.tool.window.toolbar.action.expand",
                        "action:liuzhihang.doc.tool.window.toolbar.action.collapse"),
                directEntries(toolbar));
    }

    public void testProjectDocumentActionsAreRegisteredAndLocalizedInEnglishAndChinese() {
        ActionManager manager = ActionManager.getInstance();
        assertAction(manager, "liuzhihang.doc.tool.window.toolbar.action.project.export",
                "Generate or Update Project Docs", "生成或更新项目文档");
        assertAction(manager, "liuzhihang.doc.tool.window.toolbar.action.project.open",
                "Open Project Docs", "打开项目文档");
        assertAction(manager, "liuzhihang.doc.tool.window.toolbar.action.project.search",
                "Search Project Docs", "搜索项目文档");
        assertAction(manager, "liuzhihang.doc.tool.window.toolbar.action.refresh",
                "Refresh API Catalog", "刷新接口目录");

        assertEquals("The live tool-window action text must follow the active IDEA locale",
                DocViewBundle.message("action.liuzhihang.doc.tool.window.toolbar.action.project.open.text"),
                manager.getAction("liuzhihang.doc.tool.window.toolbar.action.project.open")
                        .getTemplatePresentation().getText());
    }

    public void testSearchAndOpenBundleKeysExistInBothSupportedLocales() {
        ResourceBundle english = loadBundle("messages/DocViewBundle.properties");
        ResourceBundle chinese = loadBundle("messages/DocViewBundle_zh_CN.properties");

        for (String key : List.of(
                "project.library.not.generated",
                "project.library.status.current",
                "project.library.status.stale",
                "project.library.status.unknown",
                "project.library.status.unsupported",
                "project.library.status.invalid",
                "project.library.status.inconsistent",
                "project.library.search.no.results",
                "project.library.search.ready",
                "project.library.search.failed",
                "project.library.open.failed",
                "project.library.open.markdown",
                "project.library.search.query",
                "project.library.search",
                "project.library.back.catalog")) {
            assertTrue("English bundle is missing " + key, english.containsKey(key));
            assertTrue("Chinese bundle is missing " + key, chinese.containsKey(key));
            assertFalse("English value must not be empty for " + key, english.getString(key).isBlank());
            assertFalse("Chinese value must not be empty for " + key, chinese.getString(key).isBlank());
        }

        assertEquals("Project documentation has not been generated.", english.getString("project.library.not.generated"));
    }

    private void assertAction(ActionManager manager, String id, String englishText, String chineseText) {
        AnAction action = manager.getAction(id);
        assertNotNull("The plugin.xml action must be registered: " + id, action);
        String key = "action." + id;
        ResourceBundle english = loadBundle("messages/DocViewBundle.properties");
        ResourceBundle chinese = loadBundle("messages/DocViewBundle_zh_CN.properties");
        assertEquals(englishText, english.getString(key + ".text"));
        assertFalse("English action description should be localized: " + id,
                english.getString(key + ".description").isBlank());
        assertEquals(chineseText, chinese.getString(key + ".text"));
        assertFalse("Chinese description should be localized: " + id,
                chinese.getString(key + ".description").isBlank());
        assertEquals("ActionManager should resolve the active localized label: " + id,
                DocViewBundle.message(key + ".text"), action.getTemplatePresentation().getText());
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

    private ResourceBundle loadBundle(String resourcePath) {
        try (InputStream resource = getClass().getClassLoader().getResourceAsStream(resourcePath)) {
            assertNotNull("Missing localization resource " + resourcePath, resource);
            return new PropertyResourceBundle(new InputStreamReader(resource, StandardCharsets.UTF_8));
        } catch (Exception exception) {
            throw new IllegalStateException("Could not read localization resource " + resourcePath, exception);
        }
    }

    private Element findById(Document document, String tag, String id) {
        Element element = findByIdOrNull(document.getDocumentElement(), tag, id);
        assertNotNull("Missing " + tag + " descriptor: " + id, element);
        return element;
    }

    private Element findByIdOrNull(Element parent, String tag, String id) {
        if (tag.equals(parent.getTagName()) && id.equals(parent.getAttribute("id"))) {
            return parent;
        }
        for (Node child = parent.getFirstChild(); child != null; child = child.getNextSibling()) {
            if (child instanceof Element childElement) {
                Element found = findByIdOrNull(childElement, tag, id);
                if (found != null) return found;
            }
        }
        return null;
    }

    private List<String> directEntries(Element group) {
        List<String> entries = new ArrayList<>();
        for (Node child = group.getFirstChild(); child != null; child = child.getNextSibling()) {
            if (!(child instanceof Element element)) continue;
            if ("separator".equals(element.getTagName())) {
                entries.add("separator");
            } else if ("action".equals(element.getTagName()) || "group".equals(element.getTagName())) {
                entries.add(element.getTagName() + ":" + element.getAttribute("id"));
            }
        }
        return entries;
    }
}
