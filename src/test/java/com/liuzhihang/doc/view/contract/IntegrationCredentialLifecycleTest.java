package com.liuzhihang.doc.view.contract;

import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.testFramework.EdtTestUtil;
import com.intellij.testFramework.fixtures.LightJavaCodeInsightFixtureTestCase;
import com.intellij.ui.ContextHelpLabel;
import com.intellij.ui.components.JBPasswordField;
import com.intellij.util.xmlb.XmlSerializer;
import com.liuzhihang.doc.view.DocViewBundle;
import com.liuzhihang.doc.view.config.ShowDocSettings;
import com.liuzhihang.doc.view.config.ShowDocSettingsConfigurable;
import com.liuzhihang.doc.view.config.YApiSettings;
import com.liuzhihang.doc.view.config.YApiSettingsConfigurable;
import com.liuzhihang.doc.view.config.YuQueSettings;
import com.liuzhihang.doc.view.config.YuQueSettingsConfigurable;
import com.liuzhihang.doc.view.service.IntegrationCredentialService;
import com.liuzhihang.doc.view.ui.ShowDocSettingForm;
import com.liuzhihang.doc.view.ui.YApiSettingForm;
import com.liuzhihang.doc.view.ui.YuQueSettingForm;
import org.jdom.Element;

import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.Component;
import java.awt.Container;
import java.lang.reflect.Field;
import java.util.List;
import java.util.concurrent.FutureTask;

public final class IntegrationCredentialLifecycleTest extends LightJavaCodeInsightFixtureTestCase {

    public void testFatalCredentialStorageErrorPropagates() throws Exception {
        IntegrationCredentialService service = IntegrationCredentialService.getInstance(getProject());
        AssertionError fatal = new AssertionError("synthetic fatal storage failure");
        FutureTask<Throwable> update = new FutureTask<>(() -> {
            try {
                service.runCredentialUpdate("synthetic", () -> {
                    throw fatal;
                });
                return null;
            } catch (Throwable actual) {
                return actual;
            }
        });
        ApplicationManager.getApplication().executeOnPooledThread(update);
        assertSame(fatal, update.get());
    }

    public void testFatalCredentialStorageErrorPropagatesAfterModalTaskCleanup() throws Exception {
        IntegrationCredentialService service = IntegrationCredentialService.getInstance(getProject());
        AssertionError fatal = new AssertionError("synthetic fatal storage failure");
        java.util.concurrent.atomic.AtomicReference<AssertionError> observed = new java.util.concurrent.atomic.AtomicReference<>();

        EdtTestUtil.runInEdtAndWait(() -> {
            try {
                service.runCredentialUpdate("synthetic", () -> {
                    throw fatal;
                });
                fail("Fatal errors must propagate to the caller");
            } catch (AssertionError actual) {
                if (actual != fatal) {
                    throw actual;
                }
                observed.set(actual);
            } catch (com.intellij.openapi.options.ConfigurationException exception) {
                throw new AssertionError("Fatal errors must not be converted to a settings error", exception);
            }
        });

        assertSame(fatal, observed.get());
    }

    public void testSerializedSettingsKeepOrdinaryFieldsAndOmitCredentials() {
        ShowDocSettings showDoc = new ShowDocSettings();
        showDoc.setUrl("https://showdoc.example.test");
        Element showDocXml = XmlSerializer.serialize(showDoc.getState());
        assertChildPresent(showDocXml, "url");
        assertChildAbsent(showDocXml, "apiKey");
        assertChildAbsent(showDocXml, "apiToken");

        YApiSettings yapi = new YApiSettings();
        yapi.setUrl("https://yapi.example.test");
        yapi.setProjectId(42L);
        Element yapiXml = XmlSerializer.serialize(yapi.getState());
        assertChildPresent(yapiXml, "url");
        assertChildPresent(yapiXml, "projectId");
        assertChildAbsent(yapiXml, "token");

        YuQueSettings yuque = new YuQueSettings();
        yuque.setLogin("owner");
        yuque.setRepos("docs");
        Element yuqueXml = XmlSerializer.serialize(yuque.getState());
        assertChildPresent(yuqueXml, "url");
        assertChildPresent(yuqueXml, "apiUrl");
        assertChildPresent(yuqueXml, "login");
        assertChildPresent(yuqueXml, "repos");
        assertChildAbsent(yuqueXml, "token");
    }

    public void testAllIntegrationSecretInputsArePasswordFields() throws Exception {
        assertPasswordField(ShowDocSettingForm.class, "apiKeyTextField");
        assertPasswordField(ShowDocSettingForm.class, "apiTokenTextField");
        assertPasswordField(YApiSettingForm.class, "tokenTextField");
        assertPasswordField(YuQueSettingForm.class, "tokenTextField");
    }

    public void testConfigurableUiResourcesCanBeDisposedAndRecreated() throws Exception {
        EdtTestUtil.runInEdtAndWait(() -> {
            List<Configurable> configurables = List.of(
                    new ShowDocSettingsConfigurable(getProject()),
                    new YApiSettingsConfigurable(getProject()),
                    new YuQueSettingsConfigurable(getProject()));
            for (Configurable configurable : configurables) {
                assertNotNull(configurable.createComponent());
                configurable.disposeUIResources();
                configurable.disposeUIResources();
                assertNotNull(configurable.createComponent());
                configurable.disposeUIResources();
            }
        });
    }

    public void testResetAndDisposeDiscardSecretDrafts() throws Exception {
        EdtTestUtil.runInEdtAndWait(() -> {
            ShowDocSettingForm showDoc = new ShowDocSettingForm(getProject());
            passwordField(showDoc, "apiKeyTextField").setText("synthetic-showdoc-key");
            showDoc.reset();
            assertPasswordEmpty(passwordField(showDoc, "apiKeyTextField"));
            passwordField(showDoc, "apiTokenTextField").setText("synthetic-showdoc-token");
            showDoc.dispose();
            assertPasswordEmpty(passwordField(showDoc, "apiTokenTextField"));

            YApiSettingForm yapi = new YApiSettingForm(getProject());
            passwordField(yapi, "tokenTextField").setText("synthetic-yapi-token");
            yapi.reset();
            assertPasswordEmpty(passwordField(yapi, "tokenTextField"));
            passwordField(yapi, "tokenTextField").setText("synthetic-yapi-draft");
            yapi.dispose();
            assertPasswordEmpty(passwordField(yapi, "tokenTextField"));

            YuQueSettingForm yuque = new YuQueSettingForm(getProject());
            passwordField(yuque, "tokenTextField").setText("synthetic-yuque-token");
            yuque.reset();
            assertPasswordEmpty(passwordField(yuque, "tokenTextField"));
            passwordField(yuque, "tokenTextField").setText("synthetic-yuque-draft");
            yuque.dispose();
            assertPasswordEmpty(passwordField(yuque, "tokenTextField"));
        });
    }

    public void testIntegrationPagesUseGuideIconsAndClarifyYuQueFields() throws Exception {
        EdtTestUtil.runInEdtAndWait(() -> {
            ShowDocSettingForm showDoc = new ShowDocSettingForm(getProject());
            YApiSettingForm yapi = new YApiSettingForm(getProject());
            YuQueSettingForm yuque = new YuQueSettingForm(getProject());
            try {
                for (Object form : List.of(showDoc, yapi, yuque)) {
                    JPanel helpContainer = field(form, "integrationHelpContainer", JPanel.class);
                    assertEquals("Each integration form should show one compact help icon",
                            1, helpContainer.getComponentCount());
                    Component guide = helpContainer.getComponent(0);
                    assertTrue("Integration guide should use IDEA's native help icon",
                            guide instanceof ContextHelpLabel);
                    assertEquals(DocViewBundle.message("integration.help.open"),
                            guide.getAccessibleContext().getAccessibleName());
                    assertEquals(DocViewBundle.message("integration.help.description"),
                            guide.getAccessibleContext().getAccessibleDescription());
                }

                assertTrue(containsLabel(yuque.getRootPanel(), DocViewBundle.message("yuque.url")));
                assertTrue(containsLabel(yuque.getRootPanel(), DocViewBundle.message("yuque.api.url")));
                assertTrue(containsLabel(yuque.getRootPanel(), DocViewBundle.message("yuque.username")));
                assertTrue(containsLabel(yuque.getRootPanel(), DocViewBundle.message("yuque.repos")));
                assertTrue(field(yuque, "apiUrlTextField", com.intellij.ui.components.JBTextField.class)
                        .getToolTipText().contains("example.test/api/v2"));
            } finally {
                showDoc.dispose();
                yapi.dispose();
                yuque.dispose();
            }
        });
    }

    private static void assertPasswordField(Class<?> formClass, String fieldName) throws Exception {
        Field field = formClass.getDeclaredField(fieldName);
        assertTrue(fieldName + " should use a password control",
                JBPasswordField.class.isAssignableFrom(field.getType()));
    }

    private static JBPasswordField passwordField(Object form, String fieldName) {
        try {
            Field field = form.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            return (JBPasswordField) field.get(form);
        } catch (ReflectiveOperationException exception) {
            throw new AssertionError("Could not access password field " + fieldName, exception);
        }
    }

    private static <T> T field(Object instance, String fieldName, Class<T> fieldType) {
        try {
            Field field = instance.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            return fieldType.cast(field.get(instance));
        } catch (ReflectiveOperationException exception) {
            throw new AssertionError("Could not access form field " + fieldName, exception);
        }
    }

    private static boolean containsLabel(Container root, String text) {
        for (Component child : root.getComponents()) {
            if (child instanceof JLabel label && text.equals(label.getText())) {
                return true;
            }
            if (child instanceof Container container && containsLabel(container, text)) {
                return true;
            }
        }
        return false;
    }

    private void assertPasswordEmpty(JBPasswordField field) {
        char[] value = field.getPassword();
        try {
            assertEquals(0, value.length);
        } finally {
            java.util.Arrays.fill(value, '\0');
        }
    }

    private void assertChildPresent(Element element, String name) {
        assertTrue("Expected serialized field " + name, hasChild(element, name));
    }

    private void assertChildAbsent(Element element, String name) {
        assertFalse("Unexpected serialized field " + name, hasChild(element, name));
    }

    private static boolean hasChild(Element element, String name) {
        return element.getChildren().stream().anyMatch(child -> name.equals(child.getName())
                || name.equals(child.getAttributeValue("name")));
    }
}
