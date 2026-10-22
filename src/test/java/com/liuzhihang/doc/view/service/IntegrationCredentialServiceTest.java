package com.liuzhihang.doc.view.service;

import com.intellij.credentialStore.CredentialAttributes;
import com.intellij.credentialStore.keePass.InMemoryCredentialStore;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.options.ConfigurationException;
import com.liuzhihang.doc.view.DocViewBundle;
import junit.framework.TestCase;

import javax.swing.SwingUtilities;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

public final class IntegrationCredentialServiceTest extends TestCase {

    public void testCredentialsAreIsolatedByProjectAndProvider() {
        FakeSecretStore store = new FakeSecretStore();
        IntegrationCredentialService projectA = service("project-a", store);
        IntegrationCredentialService projectB = service("project-b", store);

        projectA.updateToken(IntegrationCredentialService.Provider.YAPI,
                IntegrationCredentialService.CredentialMutation.replace("yapi-secret"));

        assertEquals("yapi-secret", projectA.readToken(IntegrationCredentialService.Provider.YAPI));
        assertNull(projectA.readToken(IntegrationCredentialService.Provider.YUQUE));
        assertNull(projectB.readToken(IntegrationCredentialService.Provider.YAPI));
    }

    public void testKeePassEntriesAreIsolatedByProjectAndProvider() {
        InMemoryCredentialStore store = new InMemoryCredentialStore();
        IntegrationCredentialService projectA = service("project-a", new KeePassSecretStore(store));
        IntegrationCredentialService projectB = service("project-b", new KeePassSecretStore(store));

        projectA.updateShowDoc(
                IntegrationCredentialService.CredentialMutation.replace("project-a-showdoc-key"),
                IntegrationCredentialService.CredentialMutation.replace("project-a-showdoc-token"));
        projectA.updateToken(IntegrationCredentialService.Provider.YAPI,
                IntegrationCredentialService.CredentialMutation.replace("project-a-yapi-token"));

        projectB.updateShowDoc(
                IntegrationCredentialService.CredentialMutation.replace("project-b-showdoc-key"),
                IntegrationCredentialService.CredentialMutation.replace("project-b-showdoc-token"));
        projectB.updateToken(IntegrationCredentialService.Provider.YAPI,
                IntegrationCredentialService.CredentialMutation.replace("project-b-yapi-token"));

        assertEquals(new IntegrationCredentialService.ShowDocCredentials(
                        "project-a-showdoc-key", "project-a-showdoc-token"),
                projectA.readShowDocCredentials());
        assertEquals("project-a-yapi-token", projectA.readToken(IntegrationCredentialService.Provider.YAPI));
        assertEquals(new IntegrationCredentialService.ShowDocCredentials(
                        "project-b-showdoc-key", "project-b-showdoc-token"),
                projectB.readShowDocCredentials());
        assertEquals("project-b-yapi-token", projectB.readToken(IntegrationCredentialService.Provider.YAPI));

        projectA.updateShowDoc(
                IntegrationCredentialService.CredentialMutation.clear(),
                IntegrationCredentialService.CredentialMutation.clear());
        projectA.updateToken(IntegrationCredentialService.Provider.YAPI,
                IntegrationCredentialService.CredentialMutation.clear());

        assertEquals(new IntegrationCredentialService.ShowDocCredentials(null, null),
                projectA.readShowDocCredentials());
        assertNull(projectA.readToken(IntegrationCredentialService.Provider.YAPI));
        assertEquals(new IntegrationCredentialService.ShowDocCredentials(
                        "project-b-showdoc-key", "project-b-showdoc-token"),
                projectB.readShowDocCredentials());
        assertEquals("project-b-yapi-token", projectB.readToken(IntegrationCredentialService.Provider.YAPI));
    }

    public void testShowDocBundleCanReplaceKeepAndClearOneValueAtATime() {
        FakeSecretStore store = new FakeSecretStore();
        IntegrationCredentialService service = service("project-a", store);

        service.updateShowDoc(
                IntegrationCredentialService.CredentialMutation.replace("showdoc-key"),
                IntegrationCredentialService.CredentialMutation.replace("showdoc-token"));
        assertEquals(new IntegrationCredentialService.ShowDocCredentials("showdoc-key", "showdoc-token"),
                service.readShowDocCredentials());

        service.updateShowDoc(
                IntegrationCredentialService.CredentialMutation.replace("new-key"),
                IntegrationCredentialService.CredentialMutation.keep());
        assertEquals(new IntegrationCredentialService.ShowDocCredentials("new-key", "showdoc-token"),
                service.readShowDocCredentials());

        service.updateShowDoc(
                IntegrationCredentialService.CredentialMutation.clear(),
                IntegrationCredentialService.CredentialMutation.keep());
        assertEquals(new IntegrationCredentialService.ShowDocCredentials(null, "showdoc-token"),
                service.readShowDocCredentials());

        service.updateShowDoc(
                IntegrationCredentialService.CredentialMutation.keep(),
                IntegrationCredentialService.CredentialMutation.clear());
        assertEquals(new IntegrationCredentialService.ShowDocCredentials(null, null),
                service.readShowDocCredentials());
    }

    public void testCredentialStatusReportsEachShowDocValueAndSingleTokenProviders() {
        FakeSecretStore store = new FakeSecretStore();
        IntegrationCredentialService service = service("project-a", store);

        service.updateShowDoc(
                IntegrationCredentialService.CredentialMutation.replace("showdoc-key"),
                IntegrationCredentialService.CredentialMutation.keep());
        assertEquals(new IntegrationCredentialService.CredentialStatus(true, false),
                service.readStatus(IntegrationCredentialService.Provider.SHOWDOC));

        service.updateToken(IntegrationCredentialService.Provider.YUQUE,
                IntegrationCredentialService.CredentialMutation.replace("yuque-token"));
        assertEquals(new IntegrationCredentialService.CredentialStatus(false, true),
                service.readStatus(IntegrationCredentialService.Provider.YUQUE));
    }

    public void testKeepDoesNotWriteAndClearRemovesOnlySelectedProvider() {
        FakeSecretStore store = new FakeSecretStore();
        IntegrationCredentialService service = service("project-a", store);

        service.updateToken(IntegrationCredentialService.Provider.YAPI,
                IntegrationCredentialService.CredentialMutation.replace("yapi-secret"));
        int writesAfterReplace = store.writeCount;
        service.updateToken(IntegrationCredentialService.Provider.YAPI,
                IntegrationCredentialService.CredentialMutation.keep());
        assertEquals(writesAfterReplace, store.writeCount);

        service.updateToken(IntegrationCredentialService.Provider.YUQUE,
                IntegrationCredentialService.CredentialMutation.replace("yuque-secret"));
        service.updateToken(IntegrationCredentialService.Provider.YAPI,
                IntegrationCredentialService.CredentialMutation.clear());

        assertNull(service.readToken(IntegrationCredentialService.Provider.YAPI));
        assertEquals("yuque-secret", service.readToken(IntegrationCredentialService.Provider.YUQUE));
    }

    public void testPasswordSafeAccessIsRejectedOnEdt() throws Exception {
        IntegrationCredentialService service = service("project-a", new FakeSecretStore());
        AtomicReference<Throwable> failure = new AtomicReference<>();

        SwingUtilities.invokeAndWait(() -> {
            try {
                service.readToken(IntegrationCredentialService.Provider.YAPI);
            } catch (Throwable throwable) {
                failure.set(throwable);
            }
        });

        assertTrue("EDT access should fail before touching the secret store",
                failure.get() instanceof IllegalStateException);
    }

    public void testCredentialUpdateErrorDoesNotExposeExceptionDetails() {
        IntegrationCredentialService service = service("project-a", new FakeSecretStore());
        try {
            service.runCredentialUpdate("test", () -> {
                throw new IllegalStateException("token=synthetic-secret at https://private.example.test");
            });
            fail("Expected a generic credential storage error");
        } catch (ConfigurationException exception) {
            assertEquals(DocViewBundle.message("notify.credentials.storage.update.failed"), exception.getMessage());
            assertFalse(exception.getMessage().contains("synthetic-secret"));
            assertFalse(exception.getMessage().contains("private.example.test"));
        }
    }

    private static IntegrationCredentialService service(String projectHash,
                                                         IntegrationCredentialService.SecretStore store) {
        Project project = (Project) Proxy.newProxyInstance(
                Project.class.getClassLoader(),
                new Class<?>[]{Project.class},
                (proxy, method, args) -> {
                    if (method.getName().equals("getLocationHash")) {
                        return projectHash;
                    }
                    if (method.getName().equals("toString")) {
                        return "Project[" + projectHash + "]";
                    }
                    if (method.getName().equals("hashCode")) {
                        return projectHash.hashCode();
                    }
                    if (method.getName().equals("equals")) {
                        return proxy == args[0];
                    }
                    throw new UnsupportedOperationException("Unexpected Project API: " + method.getName());
                });
        return new IntegrationCredentialService(project, store);
    }

    private static final class KeePassSecretStore implements IntegrationCredentialService.SecretStore {
        private final InMemoryCredentialStore store;

        private KeePassSecretStore(InMemoryCredentialStore store) {
            this.store = store;
        }

        @Override
        public String get(CredentialAttributes attributes) {
            return store.getPassword(attributes);
        }

        @Override
        public void set(CredentialAttributes attributes, String value) {
            store.setPassword(attributes, value);
        }
    }

    private static final class FakeSecretStore implements IntegrationCredentialService.SecretStore {
        private final Map<String, String> values = new HashMap<>();
        private int writeCount;

        @Override
        public String get(CredentialAttributes attributes) {
            return values.get(key(attributes));
        }

        @Override
        public void set(CredentialAttributes attributes, String value) {
            writeCount++;
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
