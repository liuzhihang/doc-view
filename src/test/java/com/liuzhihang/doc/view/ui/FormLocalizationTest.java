package com.liuzhihang.doc.view.ui;

import com.intellij.DynamicBundle;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.testFramework.EdtTestUtil;
import com.intellij.testFramework.fixtures.LightJavaCodeInsightFixtureTestCase;
import com.intellij.ui.ContextHelpLabel;
import com.liuzhihang.doc.view.DocViewBundle;
import com.liuzhihang.doc.view.config.IntegrationSettingsConfigurable;
import org.jetbrains.annotations.NotNull;

import javax.swing.AbstractButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.TitledBorder;
import java.awt.Component;
import java.awt.Container;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class FormLocalizationTest extends LightJavaCodeInsightFixtureTestCase {

    public void testSupportFormTextsFollowDynamicBundleLocale() throws Exception {
        withOppositeDefaultLocale(() -> EdtTestUtil.runInEdtAndWait(() -> {
            SupportForm form = new SupportForm(getProject());
            try {
                assertTexts(form.createCenterPanel(), "support.title", "support.start", "support.issues",
                        "support.discussions", "support.leave.message", "support.follow");
            } finally {
                form.close(DialogWrapper.CANCEL_EXIT_CODE);
            }
        }));
    }

    public void testWindowFilterTextToolTipAndBordersFollowDynamicBundleLocale() throws Exception {
        withOppositeDefaultLocale(() -> EdtTestUtil.runInEdtAndWait(() -> {
            WindowFilterForm form = new WindowFilterForm(getProject());
            JPanel root = field(form, "rootPane", JPanel.class);
            assertTexts(root, "settings.window.interface");
            assertToolTip(root, "settings.window.interface.tip");
            assertBorderTitle(field(form, "scopePanel", JPanel.class), "settings.window.scope");
            assertBorderTitle(field(form, "otherSettingsPanel", JPanel.class), "settings.window.other");
        }));
    }

    public void testIntegrationFormLabelsAndButtonsFollowDynamicBundleLocale() throws Exception {
        withOppositeDefaultLocale(() -> EdtTestUtil.runInEdtAndWait(() -> {
            ShowDocSettingForm showDoc = new ShowDocSettingForm(getProject());
            YApiSettingForm yapi = new YApiSettingForm(getProject());
            YuQueSettingForm yuque = new YuQueSettingForm(getProject());
            try {
                assertTexts(showDoc.getRootPanel(), "showdoc.url", "showdoc.api.key", "showdoc.api.token",
                        "integration.credentials.clear");
                assertTextCount(showDoc.getRootPanel(), "integration.credentials.clear", 2);
                assertToolTip(showDoc.getRootPanel(), "integration.credentials.blank.keeps");
                assertGuide(showDoc.getRootPanel(), "integrationHelpIcon",
                        "integration.help.description", "integration.help.open");

                assertTexts(yapi.getRootPanel(), "yapi.url", "yapi.project.id", "yapi.token",
                        "integration.credentials.clear");
                assertToolTip(yapi.getRootPanel(), "integration.credentials.blank.keeps");
                assertGuide(yapi.getRootPanel(), "integrationHelpIcon",
                        "integration.help.description", "integration.help.open");

                assertTexts(yuque.getRootPanel(), "yuque.url", "yuque.api.url", "yuque.username",
                        "yuque.repos", "yuque.token", "integration.credentials.clear");
                assertToolTips(yuque.getRootPanel(), "yuque.site.url.tip", "yuque.api.url.tip",
                        "yuque.login.tip", "yuque.repos.tip");
                assertGuide(yuque.getRootPanel(), "integrationHelpIcon",
                        "integration.help.description", "integration.help.open");
            } finally {
                showDoc.dispose();
                yapi.dispose();
                yuque.dispose();
            }
        }));
    }

    public void testSettingsGuidesFollowDynamicBundleLocale() throws Exception {
        withOppositeDefaultLocale(() -> EdtTestUtil.runInEdtAndWait(() -> {
            SettingsForm settings = new SettingsForm(getProject());
            GenerationRulesForm rules = new GenerationRulesForm(getProject());
            IntegrationSettingsConfigurable integrations = new IntegrationSettingsConfigurable(getProject());
            try {
                assertGuide(settings.getRootPanel(), "settingsGuideHelpIcon",
                        "settings.help.description", "settings.help.open");
                assertGuide(rules.getRootPanel(), "settingsGuideHelpIcon",
                        "settings.help.description", "settings.help.open");
                assertGuide(integrations.createComponent(), "integrationGuideHelpIcon",
                        "integration.help.description", "integration.help.open");
            } finally {
                integrations.disposeUIResources();
            }
        }));
    }

    private void withOppositeDefaultLocale(CheckedRunnable test) throws Exception {
        Locale originalDefaultLocale = Locale.getDefault();
        Locale originalDisplayLocale = Locale.getDefault(Locale.Category.DISPLAY);
        Locale originalFormatLocale = Locale.getDefault(Locale.Category.FORMAT);
        Locale ideLocale = DynamicBundle.getLocale();
        Locale oppositeDefaultLocale = "zh".equals(ideLocale.getLanguage())
                ? Locale.US
                : Locale.SIMPLIFIED_CHINESE;
        try {
            Locale.setDefault(oppositeDefaultLocale);
            assertFalse("The test must create a JVM-default / IDE-UI locale mismatch",
                    ideLocale.getLanguage().equals(Locale.getDefault().getLanguage()));
            assertEquals("Changing JVM default locale must not change IDEA's selected UI locale",
                    ideLocale, DynamicBundle.getLocale());
            test.run();
        } finally {
            Locale.setDefault(originalDefaultLocale);
            Locale.setDefault(Locale.Category.DISPLAY, originalDisplayLocale);
            Locale.setDefault(Locale.Category.FORMAT, originalFormatLocale);
        }
    }

    private void assertTexts(JComponent root, String... keys) {
        List<String> texts = texts(root);
        for (String key : keys) {
            assertTrue("Missing localized text for " + key + ": " + DocViewBundle.message(key),
                    texts.contains(DocViewBundle.message(key)));
        }
    }

    private void assertTextCount(JComponent root, String key, int expectedCount) {
        long actualCount = texts(root).stream().filter(DocViewBundle.message(key)::equals).count();
        assertEquals("Unexpected localized text count for " + key, expectedCount, actualCount);
    }

    private void assertToolTip(JComponent root, String key) {
        assertToolTips(root, key);
    }

    private void assertToolTips(JComponent root, String... keys) {
        List<String> toolTips = new ArrayList<>();
        collectToolTips(root, toolTips);
        for (String key : keys) {
            assertTrue("Missing localized tooltip for " + key + ": " + DocViewBundle.message(key),
                    toolTips.contains(DocViewBundle.message(key)));
        }
    }

    private void assertGuide(Component root, String componentName, String descriptionKey, String linkTextKey) {
        Component component = findByName(root, componentName);
        assertTrue("Missing native context guide icon " + componentName,
                component instanceof ContextHelpLabel);
        var accessible = component.getAccessibleContext();
        assertEquals(DocViewBundle.message(linkTextKey), accessible.getAccessibleName());
        assertEquals(DocViewBundle.message(descriptionKey), accessible.getAccessibleDescription());
    }

    private static Component findByName(Component component, String name) {
        if (name.equals(component.getName())) {
            return component;
        }
        if (component instanceof Container container) {
            for (Component child : container.getComponents()) {
                Component found = findByName(child, name);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    private static List<String> texts(Component component) {
        List<String> result = new ArrayList<>();
        collectTexts(component, result);
        return result;
    }

    private static void collectTexts(Component component, List<String> result) {
        if (component instanceof JLabel label && label.getText() != null) {
            result.add(label.getText());
        } else if (component instanceof AbstractButton button && button.getText() != null) {
            result.add(button.getText());
        }
        if (component instanceof Container container) {
            for (Component child : container.getComponents()) {
                collectTexts(child, result);
            }
        }
    }

    private static void collectToolTips(Component component, List<String> result) {
        if (component instanceof JComponent swingComponent && swingComponent.getToolTipText() != null) {
            result.add(swingComponent.getToolTipText());
        }
        if (component instanceof Container container) {
            for (Component child : container.getComponents()) {
                collectToolTips(child, result);
            }
        }
    }

    private void assertBorderTitle(JPanel panel, String key) {
        assertTrue("Expected a titled border for " + key, panel.getBorder() instanceof TitledBorder);
        assertEquals(DocViewBundle.message(key), ((TitledBorder) panel.getBorder()).getTitle());
    }

    private static <T> T field(@NotNull Object target, @NotNull String name, @NotNull Class<T> type) {
        try {
            Field field = target.getClass().getDeclaredField(name);
            field.setAccessible(true);
            return type.cast(field.get(target));
        } catch (ReflectiveOperationException exception) {
            throw new AssertionError("Cannot read form field " + name, exception);
        }
    }

    @FunctionalInterface
    private interface CheckedRunnable {
        void run() throws Exception;
    }
}
