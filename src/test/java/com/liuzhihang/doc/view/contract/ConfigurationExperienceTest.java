package com.liuzhihang.doc.view.contract;

import com.intellij.ide.ui.search.SearchableOptionProcessor;
import com.intellij.openapi.project.Project;
import com.intellij.testFramework.fixtures.LightJavaCodeInsightFixtureTestCase;
import com.intellij.testFramework.EdtTestUtil;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.util.JDOMUtil;
import com.intellij.ui.ContextHelpLabel;
import com.intellij.ui.components.JBPasswordField;
import com.intellij.ui.components.JBTextField;
import com.intellij.util.xmlb.XmlSerializer;
import com.liuzhihang.doc.view.DocViewBundle;
import com.liuzhihang.doc.view.config.DocViewSearchableOptionContributor;
import com.liuzhihang.doc.view.config.GenerationRulesConfigurable;
import com.liuzhihang.doc.view.config.IntegrationSettingsConfigurable;
import com.liuzhihang.doc.view.config.ShowDocSettings;
import com.liuzhihang.doc.view.config.Settings;
import com.liuzhihang.doc.view.config.SettingsConfigurable;
import com.liuzhihang.doc.view.config.TemplateConfigurable;
import com.liuzhihang.doc.view.config.TemplateSettings;
import com.liuzhihang.doc.view.config.YApiSettings;
import com.liuzhihang.doc.view.config.YuQueSettings;
import com.liuzhihang.doc.view.ui.ShowDocSettingForm;
import com.liuzhihang.doc.view.ui.TemplateSettingForm;
import com.liuzhihang.doc.view.ui.YApiSettingForm;
import com.liuzhihang.doc.view.ui.YuQueSettingForm;

import javax.swing.JCheckBox;
import javax.swing.JComponent;
import java.awt.Component;
import java.awt.Container;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

public final class ConfigurationExperienceTest extends LightJavaCodeInsightFixtureTestCase {

    private Settings settings;
    private boolean originalLineMarker;
    private boolean originalIncludeNormalInterface;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        settings = Settings.getInstance(getProject());
        originalLineMarker = settings.getLineMarker();
        originalIncludeNormalInterface = settings.getIncludeNormalInterface();
    }

    @Override
    protected void tearDown() throws Exception {
        try {
            if (settings != null) {
                settings.setLineMarker(originalLineMarker);
                settings.setIncludeNormalInterface(originalIncludeNormalInterface);
            }
        } finally {
            super.tearDown();
        }
    }

    public void testProjectSettingsUseNestedPagesForRulesTemplatesAndIntegrations() throws Exception {
        Document descriptor = loadPluginDescriptor();
        Map<String, Element> pages = configurableElements(descriptor);

        Element settingsPage = pages.get("liuzhihang.api.doc.SettingsConfigurable");
        assertNotNull("The existing project settings page ID must remain stable", settingsPage);
        assertEquals("tools", settingsPage.getAttribute("parentId"));
        assertConfigurableBundleKey(settingsPage, "settings.page.root");
        assertEquals("Doc View", new SettingsConfigurable(getProject()).getDisplayName());

        assertPageParent(pages, "liuzhihang.api.doc.GenerationRulesConfigurable",
                "liuzhihang.api.doc.SettingsConfigurable");
        assertPageParent(pages, "liuzhihang.api.doc.TemplateConfigurable",
                "liuzhihang.api.doc.SettingsConfigurable");
        assertPageParent(pages, "liuzhihang.api.doc.IntegrationSettingsConfigurable",
                "liuzhihang.api.doc.SettingsConfigurable");
        assertPageParent(pages, "liuzhihang.api.doc.ShowDocSettingsConfigurable",
                "liuzhihang.api.doc.IntegrationSettingsConfigurable");
        assertPageParent(pages, "liuzhihang.api.doc.YApiSettingsConfigurable",
                "liuzhihang.api.doc.IntegrationSettingsConfigurable");
        assertPageParent(pages, "liuzhihang.api.doc.YuQueSettingsConfigurable",
                "liuzhihang.api.doc.IntegrationSettingsConfigurable");
        assertConfigurableBundleKey(pages.get("liuzhihang.api.doc.GenerationRulesConfigurable"), "settings.page.rules");
        assertConfigurableBundleKey(pages.get("liuzhihang.api.doc.TemplateConfigurable"), "settings.page.template");
        assertConfigurableBundleKey(pages.get("liuzhihang.api.doc.IntegrationSettingsConfigurable"), "settings.page.integrations");
        assertConfigurableBundleKey(pages.get("liuzhihang.api.doc.YApiSettingsConfigurable"), "settings.page.yapi");
        assertConfigurableBundleKey(pages.get("liuzhihang.api.doc.ShowDocSettingsConfigurable"), "settings.page.showdoc");
        assertConfigurableBundleKey(pages.get("liuzhihang.api.doc.YuQueSettingsConfigurable"), "settings.page.yuque");
        assertEquals("Template and Output", new TemplateConfigurable(getProject()).getDisplayName());

        NodeList contributors = descriptor.getElementsByTagName("search.optionContributor");
        assertEquals("A public searchable-options contributor must be registered", 1, contributors.getLength());
        assertEquals("com.liuzhihang.doc.view.config.DocViewSearchableOptionContributor",
                ((Element) contributors.item(0)).getAttribute("implementation"));
    }

    public void testUnusedSettingsAreRemovedButActiveParameterFilterRemains() {
        Map<String, Boolean> fields = new HashMap<>();
        for (var field : Settings.class.getDeclaredFields()) {
            fields.put(field.getName(), true);
        }

        assertFalse("The removed hideLeft option must not remain in persisted settings", fields.containsKey("hideLeft"));
        assertFalse("The unused plural parameter-filter set must not remain in settings", fields.containsKey("excludeParamTypes"));
        assertTrue("The active parameter filter must remain available", fields.containsKey("excludeParameterType"));
    }

    public void testLineMarkerAndNormalInterfaceControlsAreIndependent() throws Exception {
        settings.setLineMarker(false);
        settings.setIncludeNormalInterface(false);

        Class<?> configurableType = Class.forName(
                "com.liuzhihang.doc.view.config.SettingsConfigurable");
        Constructor<?> constructor = configurableType.getConstructor(Project.class);
        Object configurable = constructor.newInstance(getProject());
        JComponent component = (JComponent) configurableType.getMethod("createComponent").invoke(configurable);
        configurableType.getMethod("reset").invoke(configurable);

        JCheckBox lineMarker = findCheckBox(component, "lineMarkerCheckBox");
        JCheckBox includeNormalInterface = findCheckBox(component, "includeNormalInterfaceCheckBox");
        assertNotNull("The generation-rules page must expose the line-marker option", lineMarker);
        assertNotNull("The generation-rules page must expose the ordinary-interface option", includeNormalInterface);
        assertFalse(lineMarker.isSelected());
        assertTrue("Disabling gutter markers must not disable interface recognition and scanning",
                includeNormalInterface.isEnabled());

        includeNormalInterface.setSelected(true);
        invoke(configurableType.getMethod("apply"), configurable);
        assertFalse(settings.getLineMarker());
        assertTrue(settings.getIncludeNormalInterface());

        lineMarker.setSelected(true);
        invoke(configurableType.getMethod("reset"), configurable);
        assertFalse("Reset must restore the separately saved gutter-marker value", lineMarker.isSelected());
        assertTrue("Reset must preserve the separately saved ordinary-interface value", includeNormalInterface.isSelected());
        assertTrue(includeNormalInterface.isEnabled());
    }

    public void testSearchTermsAreMappedToTheirSettingsPages() {
        List<SearchEntry> entries = new ArrayList<>();
        new DocViewSearchableOptionContributor().processOptions(new SearchableOptionProcessor() {
            @Override
            public void addOptions(String option, String path, String hit, String configurableId,
                                  String displayName, boolean applyStemming) {
                entries.add(new SearchEntry(option, path, hit, configurableId, displayName));
            }
        });

        assertTrue(entries.stream().anyMatch(entry -> entry.option().equals("普通接口")
                && entry.configurableId().equals("liuzhihang.api.doc.SettingsConfigurable")));
        SearchEntry rootEntry = entries.stream().filter(entry -> entry.option().equals("行标记")
                && entry.configurableId().equals("liuzhihang.api.doc.SettingsConfigurable"))
                .findFirst().orElseThrow();
        assertEquals("Doc View", rootEntry.path());
        assertEquals("Doc View", rootEntry.hit());
        assertEquals("Doc View", rootEntry.displayName());
        assertTrue(entries.stream().anyMatch(entry -> entry.option().equals("模板变量")
                && entry.configurableId().equals("liuzhihang.api.doc.TemplateConfigurable")));
        assertTrue(entries.stream().anyMatch(entry -> entry.option().equals("变量")
                && entry.configurableId().equals("liuzhihang.api.doc.TemplateConfigurable")));
        assertTrue(entries.stream().anyMatch(entry -> entry.option().equals("导出")
                && entry.configurableId().equals("liuzhihang.api.doc.TemplateConfigurable")));
        assertTrue(entries.stream().anyMatch(entry -> entry.option().equals("platform integrations")
                && entry.configurableId().equals("liuzhihang.api.doc.IntegrationSettingsConfigurable")));
    }

    public void testSharedSettingsGuideBaseTargetsMaster() throws Exception {
        var base = com.liuzhihang.doc.view.ui.SettingsGuideHelp.class.getDeclaredField("GUIDE_BASE_URL");
        base.setAccessible(true);
        assertEquals("https://github.com/liuzhihang/doc-view/blob/master", base.get(null));
    }

    public void testGuideIconsAreAccessibleAndDoNotChangeDraftsOrSavedSettings() {
        EdtTestUtil.runInEdtAndWait(() -> {
            SettingsConfigurable general = new SettingsConfigurable(getProject());
            GenerationRulesConfigurable rules = new GenerationRulesConfigurable(getProject());
            IntegrationSettingsConfigurable integrations = new IntegrationSettingsConfigurable(getProject());
            TemplateSettingForm template = new TemplateSettingForm(getProject());
            ShowDocSettingForm showDoc = new ShowDocSettingForm(getProject());
            YApiSettingForm yapi = new YApiSettingForm(getProject());
            YuQueSettingForm yuque = new YuQueSettingForm(getProject());
            String savedSettings = serializeState(settings.getState());
            TemplateSettings templates = TemplateSettings.getInstance(getProject());
            String savedTemplateSettings = serializeState(templates.getState());
            String savedShowDocSettings = serializeState(ShowDocSettings.getInstance(getProject()).getState());
            String savedYApiSettings = serializeState(YApiSettings.getInstance(getProject()).getState());
            String savedYuQueSettings = serializeState(YuQueSettings.getInstance(getProject()).getState());
            String savedSpring = templates.getSpringTemplate();
            String savedDubbo = templates.getDubboTemplate();
            try {
                JComponent generalUi = general.createComponent();
                JComponent rulesUi = rules.createComponent();
                JComponent templateUi = template.createCenterPanel();
                JComponent integrationUi = integrations.createComponent();
                general.reset();
                rules.reset();
                JCheckBox marker = findCheckBox(generalUi, "lineMarkerCheckBox");
                JCheckBox title = findCheckBox(rulesUi, "titleCommentTagCheckBox");
                marker.setSelected(!marker.isSelected());
                title.setSelected(!title.isSelected());
                boolean markerDraft = marker.isSelected();
                boolean titleDraft = title.isSelected();
                String springDraft = savedSpring + "\n# UNSAVED SPRING\n";
                String dubboDraft = savedDubbo + "\n# UNSAVED DUBBO\n";
                WriteCommandAction.runWriteCommandAction(getProject(), () -> {
                    template.getSpringTemplateEditor().getDocument().setText(springDraft);
                    template.getDubboTemplateEditor().getDocument().setText(dubboDraft);
                });

                JBTextField showDocUrl = field(showDoc, "urlTextField", JBTextField.class);
                JBPasswordField showDocApiKey = field(showDoc, "apiKeyTextField", JBPasswordField.class);
                JBPasswordField showDocApiToken = field(showDoc, "apiTokenTextField", JBPasswordField.class);
                JBTextField yapiUrl = field(yapi, "urlTextField", JBTextField.class);
                JBTextField yapiProjectId = field(yapi, "projectIdTextField", JBTextField.class);
                JBPasswordField yapiToken = field(yapi, "tokenTextField", JBPasswordField.class);
                JBTextField yuqueUrl = field(yuque, "urlTextField", JBTextField.class);
                JBTextField yuqueApiUrl = field(yuque, "apiUrlTextField", JBTextField.class);
                JBTextField yuqueLogin = field(yuque, "loginTextField", JBTextField.class);
                JBTextField yuqueRepos = field(yuque, "reposTextField", JBTextField.class);
                JBPasswordField yuqueToken = field(yuque, "tokenTextField", JBPasswordField.class);
                showDocUrl.setText("https://showdoc.draft.example.test");
                showDocApiKey.setText("synthetic-showdoc-key-draft");
                showDocApiToken.setText("synthetic-showdoc-token-draft");
                yapiUrl.setText("https://yapi.draft.example.test");
                yapiProjectId.setText("42");
                yapiToken.setText("synthetic-yapi-token-draft");
                yuqueUrl.setText("https://yuque.draft.example.test");
                yuqueApiUrl.setText("https://yuque.draft.example.test/api/v2");
                yuqueLogin.setText("draft-owner");
                yuqueRepos.setText("draft-docs");
                yuqueToken.setText("synthetic-yuque-token-draft");
                String[] integrationDrafts = {
                        showDocUrl.getText(), password(showDocApiKey), password(showDocApiToken),
                        yapiUrl.getText(), yapiProjectId.getText(), password(yapiToken),
                        yuqueUrl.getText(), yuqueApiUrl.getText(), yuqueLogin.getText(), yuqueRepos.getText(),
                        password(yuqueToken)
                };

                Component[] helpIcons = {
                        findComponentByName(generalUi, "settingsGuideHelpIcon"),
                        findComponentByName(rulesUi, "settingsGuideHelpIcon"),
                        findComponentByName(templateUi, "templateHelpLabel"),
                        findComponentByName(field(showDoc, "integrationHelpContainer", JComponent.class),
                                "integrationHelpIcon"),
                        findComponentByName(field(yapi, "integrationHelpContainer", JComponent.class),
                                "integrationHelpIcon"),
                        findComponentByName(field(yuque, "integrationHelpContainer", JComponent.class),
                                "integrationHelpIcon"),
                        findComponentByName(integrationUi, "integrationGuideHelpIcon")
                };
                String[] accessibleNameKeys = {"settings.help.open", "settings.help.open", "template.help.open",
                        "integration.help.open", "integration.help.open", "integration.help.open",
                        "integration.help.open"};
                String[] descriptionKeys = {"settings.help.description", "settings.help.description",
                        "template.help.description", "integration.help.description", "integration.help.description",
                        "integration.help.description", "integration.help.description"};
                for (int index = 0; index < helpIcons.length; index++) {
                    Component icon = helpIcons[index];
                    assertTrue("Every settings guide should be an IDEA context help icon at index " + index,
                            icon instanceof ContextHelpLabel);
                    assertEquals(DocViewBundle.message(accessibleNameKeys[index]),
                            icon.getAccessibleContext().getAccessibleName());
                    assertEquals(DocViewBundle.message(descriptionKeys[index]),
                            icon.getAccessibleContext().getAccessibleDescription());
                    assertEquals(markerDraft, marker.isSelected());
                    assertEquals(titleDraft, title.isSelected());
                    assertEquals(springDraft, template.getSpringTemplateEditor().getDocument().getText());
                    assertEquals(dubboDraft, template.getDubboTemplateEditor().getDocument().getText());
                    assertTrue(general.isModified());
                    assertTrue(rules.isModified());
                    assertTrue(template.isModified());
                    assertFalse(integrations.isModified());
                    assertTrue(showDoc.isModified());
                    assertTrue(yapi.isModified());
                    assertTrue(yuque.isModified());
                    assertEquals(savedSettings, serializeState(settings.getState()));
                    assertEquals(savedTemplateSettings, serializeState(templates.getState()));
                    assertEquals(savedShowDocSettings,
                            serializeState(ShowDocSettings.getInstance(getProject()).getState()));
                    assertEquals(savedYApiSettings, serializeState(YApiSettings.getInstance(getProject()).getState()));
                    assertEquals(savedYuQueSettings,
                            serializeState(YuQueSettings.getInstance(getProject()).getState()));
                    assertEquals(savedSpring, templates.getSpringTemplate());
                    assertEquals(savedDubbo, templates.getDubboTemplate());
                    org.junit.Assert.assertArrayEquals(integrationDrafts, new String[]{
                            showDocUrl.getText(), password(showDocApiKey), password(showDocApiToken),
                            yapiUrl.getText(), yapiProjectId.getText(), password(yapiToken),
                            yuqueUrl.getText(), yuqueApiUrl.getText(), yuqueLogin.getText(), yuqueRepos.getText(),
                            password(yuqueToken)
                    });
                }
            } finally {
                template.disposeIfNeeded();
                general.disposeUIResources();
                rules.disposeUIResources();
                integrations.disposeUIResources();
                showDoc.dispose();
                yapi.dispose();
                yuque.dispose();
            }
        });
    }

    private static String serializeState(Object state) {
        return JDOMUtil.writeElement(XmlSerializer.serialize(state));
    }

    private static String password(JBPasswordField field) {
        return new String(field.getPassword());
    }

    private static <T> T field(Object instance, String fieldName, Class<T> fieldType) {
        try {
            var field = instance.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            return fieldType.cast(field.get(instance));
        } catch (ReflectiveOperationException exception) {
            throw new AssertionError("Could not access configuration draft field " + fieldName, exception);
        }
    }

    private static Component findComponentByName(Container root, String name) {
        for (Component child : root.getComponents()) {
            if (name.equals(child.getName())) return child;
            if (child instanceof Container container) {
                Component result = findComponentByName(container, name);
                if (result != null) return result;
            }
        }
        return null;
    }

    private record SearchEntry(String option, String path, String hit, String configurableId, String displayName) {
    }

    private static Document loadPluginDescriptor() throws Exception {
        try (InputStream stream = ConfigurationExperienceTest.class.getClassLoader()
                .getResourceAsStream("META-INF/plugin.xml")) {
            assertNotNull("The packaged plugin descriptor must be present on the test classpath", stream);
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            return factory.newDocumentBuilder().parse(stream);
        }
    }

    private static Map<String, Element> configurableElements(Document descriptor) {
        Map<String, Element> pages = new HashMap<>();
        NodeList entries = descriptor.getElementsByTagName("projectConfigurable");
        for (int index = 0; index < entries.getLength(); index++) {
            Element element = (Element) entries.item(index);
            pages.put(element.getAttribute("id"), element);
        }
        return pages;
    }

    private static void assertPageParent(Map<String, Element> pages, String id, String parentId) {
        Element page = pages.get(id);
        assertNotNull("Missing project settings page " + id, page);
        assertEquals("Unexpected parent for " + id, parentId, page.getAttribute("parentId"));
        assertFalse("The deprecated groupId attribute must not define settings hierarchy for " + id,
                page.hasAttribute("groupId"));
        assertEquals("Project settings scope must be preserved for " + id,
                "true", page.getAttribute("nonDefaultProject"));
    }

    private static void assertConfigurableBundleKey(Element page, String key) {
        assertNotNull("Missing project settings page with bundle key " + key, page);
        assertFalse("Configurable names must use ConfigurableEP bundle/key, not a literal displayName",
                page.hasAttribute("displayName"));
        assertEquals("messages.DocViewBundle", page.getAttribute("bundle"));
        assertEquals(key, page.getAttribute("key"));
    }

    private static JCheckBox findCheckBox(Container root, String name) {
        for (Component child : root.getComponents()) {
            if (child instanceof JCheckBox checkBox && name.equals(checkBox.getName())) {
                return checkBox;
            }
            if (child instanceof Container container) {
                JCheckBox nested = findCheckBox(container, name);
                if (nested != null) {
                    return nested;
                }
            }
        }
        return null;
    }

    private static void invoke(Method method, Object target) {
        try {
            method.invoke(target);
        } catch (ReflectiveOperationException exception) {
            throw new AssertionError("Could not invoke settings lifecycle method " + method.getName(), exception);
        }
    }
}
