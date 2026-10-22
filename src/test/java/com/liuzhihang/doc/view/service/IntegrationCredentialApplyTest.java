package com.liuzhihang.doc.view.service;

import com.intellij.credentialStore.CredentialAttributes;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.project.Project;
import com.intellij.testFramework.EdtTestUtil;
import com.intellij.testFramework.fixtures.LightJavaCodeInsightFixtureTestCase;
import com.intellij.ui.components.JBPasswordField;
import com.intellij.ui.components.JBTextField;
import com.liuzhihang.doc.view.config.ShowDocSettings;
import com.liuzhihang.doc.view.config.ShowDocSettingsConfigurable;
import com.liuzhihang.doc.view.ui.ShowDocSettingForm;

import javax.swing.JCheckBox;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public final class IntegrationCredentialApplyTest extends LightJavaCodeInsightFixtureTestCase {

    public void testFailedApplyKeepsDraftAndStateThenRetryKeepCancelAndClearAreExplicit() throws Exception {
        FakeSecretStore store = new FakeSecretStore();
        Project project = projectWithCredentials(store);
        IntegrationCredentialService credentials = IntegrationCredentialService.getInstance(project);
        ShowDocSettings settings = ShowDocSettings.getInstance(project);
        String originalUrl = settings.getUrl();
        settings.setUrl("https://saved.example.test");
        runInBackground(() -> {
            credentials.updateShowDoc(
                    IntegrationCredentialService.CredentialMutation.replace("saved-key"),
                    IntegrationCredentialService.CredentialMutation.replace("saved-token"));
            return null;
        });

        AtomicReference<ShowDocSettingsConfigurable> configurableRef = new AtomicReference<>();
        AtomicReference<ShowDocSettingForm> formRef = new AtomicReference<>();
        AtomicReference<ShowDocSettingForm> clearFormRef = new AtomicReference<>();
        try {
            AtomicReference<ConfigurationException> applyFailure = new AtomicReference<>();
            EdtTestUtil.runInEdtAndWait(() -> {
                ShowDocSettingsConfigurable configurable = new ShowDocSettingsConfigurable(project);
                configurable.createComponent();
                configurable.reset();
                ShowDocSettingForm form = field(configurable, "showDocSettingForm", ShowDocSettingForm.class);
                configurableRef.set(configurable);
                formRef.set(form);

                textField(form, "urlTextField").setText(" https://draft.example.test/// ");
                passwordField(form, "apiKeyTextField").setText("replacement-key");
                store.failNextWrite = "token=synthetic-secret at https://private.example.test";
                try {
                    configurable.apply();
                } catch (ConfigurationException exception) {
                    applyFailure.set(exception);
                }
            });

            ConfigurationException failure = applyFailure.get();
            assertNotNull("The simulated PasswordSafe failure should reject Apply", failure);
            assertFalse(failure.getMessage().contains("synthetic-secret"));
            assertFalse(failure.getMessage().contains("private.example.test"));
            assertEquals("https://saved.example.test", settings.getUrl());
            assertEquals(new IntegrationCredentialService.ShowDocCredentials("saved-key", "saved-token"),
                    runInBackground(credentials::readShowDocCredentials));

            EdtTestUtil.runInEdtAndWait(() -> {
                ShowDocSettingsConfigurable configurable = configurableRef.get();
                ShowDocSettingForm form = formRef.get();
                assertEquals(" https://draft.example.test/// ", textField(form, "urlTextField").getText());
                assertPasswordValue("replacement-key", passwordField(form, "apiKeyTextField"));
                assertTrue(configurable.isModified());

                try {
                    configurable.apply();
                } catch (ConfigurationException exception) {
                    throw new AssertionError("Retry should succeed with the fake store available", exception);
                }
                assertEquals("https://draft.example.test", settings.getUrl());
                assertPasswordValue("", passwordField(form, "apiKeyTextField"));
                assertFalse(configurable.isModified());
            });
            assertEquals(new IntegrationCredentialService.ShowDocCredentials("replacement-key", "saved-token"),
                    runInBackground(credentials::readShowDocCredentials));

            AtomicInteger writesBeforeCancel = new AtomicInteger();
            EdtTestUtil.runInEdtAndWait(() -> {
                ShowDocSettingsConfigurable configurable = configurableRef.get();
                ShowDocSettingForm form = formRef.get();
                writesBeforeCancel.set(store.writes.get());
                textField(form, "urlTextField").setText("https://cancel-draft.example.test");
                passwordField(form, "apiKeyTextField").setText("cancel-draft-key");
                field(form, "clearApiTokenCheckBox", JCheckBox.class).doClick();
                configurable.disposeUIResources();
            });
            assertEquals("Cancel/close without Apply must not commit the draft URL",
                    "https://draft.example.test", settings.getUrl());
            assertEquals("Cancel/close without Apply must not replace or clear credentials",
                    new IntegrationCredentialService.ShowDocCredentials("replacement-key", "saved-token"),
                    runInBackground(credentials::readShowDocCredentials));
            assertEquals(writesBeforeCancel.get(), store.writes.get());

            EdtTestUtil.runInEdtAndWait(() -> {
                ShowDocSettingForm clearForm = new ShowDocSettingForm(project);
                clearFormRef.set(clearForm);
                clearForm.reset();
                field(clearForm, "clearApiTokenCheckBox", JCheckBox.class).doClick();
                try {
                    clearForm.apply();
                } catch (ConfigurationException exception) {
                    throw new AssertionError("Explicitly clearing a saved token should succeed", exception);
                }
                assertFalse(clearForm.isModified());
                clearForm.dispose();
            });
            assertEquals(new IntegrationCredentialService.ShowDocCredentials("replacement-key", null),
                    runInBackground(credentials::readShowDocCredentials));
        } finally {
            EdtTestUtil.runInEdtAndWait(() -> {
                ShowDocSettingsConfigurable configurable = configurableRef.get();
                if (configurable != null) {
                    configurable.disposeUIResources();
                }
                ShowDocSettingForm clearForm = clearFormRef.get();
                if (clearForm != null) {
                    clearForm.dispose();
                }
            });
            settings.setUrl(originalUrl);
        }
    }

    private Project projectWithCredentials(FakeSecretStore store) {
        AtomicReference<IntegrationCredentialService> serviceRef = new AtomicReference<>();
        Project delegate = getProject();
        InvocationHandler handler = (proxy, method, args) -> {
            if (method.getName().equals("getService") && args != null && args.length == 1
                    && args[0] == IntegrationCredentialService.class) {
                return serviceRef.get();
            }
            if (method.getDeclaringClass() == Object.class) {
                return switch (method.getName()) {
                    case "toString" -> "TestProject[" + delegate.getLocationHash() + "]";
                    case "hashCode" -> delegate.hashCode();
                    case "equals" -> proxy == args[0];
                    default -> throw new UnsupportedOperationException(method.getName());
                };
            }
            try {
                return method.invoke(delegate, args);
            } catch (InvocationTargetException exception) {
                throw exception.getCause();
            }
        };
        Project project = (Project) Proxy.newProxyInstance(
                Project.class.getClassLoader(), new Class<?>[]{Project.class}, handler);
        serviceRef.set(new IntegrationCredentialService(project, store));
        return project;
    }

    private static <T> T field(Object instance, String name, Class<T> type) {
        try {
            Field field = instance.getClass().getDeclaredField(name);
            field.setAccessible(true);
            return type.cast(field.get(instance));
        } catch (ReflectiveOperationException exception) {
            throw new AssertionError("Could not access " + name, exception);
        }
    }

    private static JBTextField textField(Object form, String name) {
        return field(form, name, JBTextField.class);
    }

    private static JBPasswordField passwordField(Object form, String name) {
        return field(form, name, JBPasswordField.class);
    }

    private static void assertPasswordValue(String expected, JBPasswordField field) {
        char[] actual = field.getPassword();
        try {
            assertEquals(expected, new String(actual));
        } finally {
            java.util.Arrays.fill(actual, '\0');
        }
    }

    private static <T> T runInBackground(Callable<T> operation) throws Exception {
        FutureTask<T> task = new FutureTask<>(operation);
        Thread worker = new Thread(task, "integration-credential-apply-test");
        worker.start();
        try {
            return task.get();
        } catch (ExecutionException exception) {
            Throwable cause = exception.getCause();
            if (cause instanceof Exception checked) {
                throw checked;
            }
            if (cause instanceof Error error) {
                throw error;
            }
            throw new AssertionError("Background credential operation failed", cause);
        }
    }

    private static final class FakeSecretStore implements IntegrationCredentialService.SecretStore {
        private final Map<String, String> values = new HashMap<>();
        private final AtomicInteger writes = new AtomicInteger();
        private volatile String failNextWrite;

        @Override
        public synchronized String get(CredentialAttributes attributes) {
            return values.get(key(attributes));
        }

        @Override
        public synchronized void set(CredentialAttributes attributes, String value) {
            writes.incrementAndGet();
            String failure = failNextWrite;
            if (failure != null) {
                failNextWrite = null;
                throw new IllegalStateException(failure);
            }
            String key = key(attributes);
            if (value == null) {
                values.remove(key);
            } else {
                values.put(key, value);
            }
        }

        private static String key(CredentialAttributes attributes) {
            return attributes.getServiceName() + "\u0000" + attributes.getUserName();
        }
    }
}
