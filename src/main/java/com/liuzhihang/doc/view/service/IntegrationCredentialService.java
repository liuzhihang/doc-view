package com.liuzhihang.doc.view.service;

import com.google.gson.Gson;
import com.intellij.credentialStore.CredentialAttributes;
import com.intellij.credentialStore.CredentialAttributesKt;
import com.intellij.ide.passwordSafe.PasswordSafe;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.liuzhihang.doc.view.DocViewBundle;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.EventQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Stores integration credentials outside project settings.
 */
@Service(Service.Level.PROJECT)
public final class IntegrationCredentialService {

    private static final Gson GSON = new Gson();
    private static final String SERVICE_NAME = "Doc View integration credentials";

    private final Project project;
    private final SecretStore secretStore;

    public IntegrationCredentialService(@NotNull Project project) {
        this(project, new PasswordSafeSecretStore());
    }

    IntegrationCredentialService(@NotNull Project project, @NotNull SecretStore secretStore) {
        this.project = project;
        this.secretStore = secretStore;
    }

    public static @NotNull IntegrationCredentialService getInstance(@NotNull Project project) {
        return project.getService(IntegrationCredentialService.class);
    }

    public @NotNull CredentialStatus readStatus(@NotNull Provider provider) {
        assertBackgroundThread();
        if (provider == Provider.SHOWDOC) {
            ShowDocCredentials credentials = readShowDocCredentialsInternal();
            return new CredentialStatus(isConfigured(credentials.apiKey()), isConfigured(credentials.apiToken()));
        }
        return new CredentialStatus(false, isConfigured(readTokenInternal(provider)));
    }

    public @Nullable String readToken(@NotNull Provider provider) {
        assertBackgroundThread();
        return readTokenInternal(provider);
    }

    public @NotNull ShowDocCredentials readShowDocCredentials() {
        assertBackgroundThread();
        return readShowDocCredentialsInternal();
    }

    public void updateToken(@NotNull Provider provider, @NotNull CredentialMutation mutation) {
        assertBackgroundThread();
        if (provider == Provider.SHOWDOC) {
            throw new IllegalArgumentException("ShowDoc credentials must be updated as a bundle");
        }
        validateMutation(mutation);
        switch (mutation.type()) {
            case KEEP -> {
                return;
            }
            case REPLACE -> secretStore.set(attributesFor(provider), mutation.value());
            case CLEAR -> secretStore.set(attributesFor(provider), null);
        }
    }

    public void updateShowDoc(@NotNull CredentialMutation apiKeyMutation,
                              @NotNull CredentialMutation apiTokenMutation) {
        assertBackgroundThread();
        validateMutation(apiKeyMutation);
        validateMutation(apiTokenMutation);
        if (apiKeyMutation.type() == MutationType.KEEP && apiTokenMutation.type() == MutationType.KEEP) {
            return;
        }

        ShowDocCredentials current;
        if (apiKeyMutation.type() == MutationType.KEEP || apiTokenMutation.type() == MutationType.KEEP) {
            current = readShowDocCredentialsInternal();
        } else {
            current = new ShowDocCredentials(null, null);
        }

        String apiKey = applyMutation(current.apiKey(), apiKeyMutation);
        String apiToken = applyMutation(current.apiToken(), apiTokenMutation);
        if (apiKey == null && apiToken == null) {
            secretStore.set(attributesFor(Provider.SHOWDOC), null);
        } else {
            secretStore.set(attributesFor(Provider.SHOWDOC), GSON.toJson(new ShowDocCredentials(apiKey, apiToken)));
        }
    }

    /**
     * Performs the PasswordSafe operation in a non-cancellable modal task when called from the EDT.
     * The task runs on a background thread; ordinary runtime failures are replaced with a generic,
     * credential-free settings error. JVM errors propagate after the modal task finishes cleanup.
     */
    public void runCredentialUpdate(@NotNull String title, @NotNull Runnable update) throws ConfigurationException {
        if (!EventQueue.isDispatchThread()) {
            try {
                update.run();
            } catch (RuntimeException ignored) {
                throw storageUpdateException();
            }
            return;
        }

        AtomicReference<RuntimeException> failure = new AtomicReference<>();
        AtomicReference<Error> fatalFailure = new AtomicReference<>();
        AtomicBoolean completed = new AtomicBoolean();
        try {
            ProgressManager.getInstance().run(new Task.Modal(project, title, false) {
                @Override
                public void run(@NotNull ProgressIndicator indicator) {
                    try {
                        update.run();
                    } catch (RuntimeException throwable) {
                        failure.set(throwable);
                    } catch (Error error) {
                        fatalFailure.set(error);
                    } finally {
                        completed.set(true);
                    }
                }
            });
        } catch (Error error) {
            throw error;
        } catch (RuntimeException ignored) {
            throw storageUpdateException();
        }
        Error fatal = fatalFailure.get();
        if (fatal != null) {
            throw fatal;
        }
        if (!completed.get() || failure.get() != null) {
            throw storageUpdateException();
        }
    }

    private static ConfigurationException storageUpdateException() {
        return new ConfigurationException(DocViewBundle.message("notify.credentials.storage.update.failed"));
    }

    private @Nullable String readTokenInternal(@NotNull Provider provider) {
        if (provider == Provider.SHOWDOC) {
            throw new IllegalArgumentException("ShowDoc credentials must be read as a bundle");
        }
        return secretStore.get(attributesFor(provider));
    }

    private @NotNull ShowDocCredentials readShowDocCredentialsInternal() {
        String value = secretStore.get(attributesFor(Provider.SHOWDOC));
        if (value == null || value.isEmpty()) {
            return new ShowDocCredentials(null, null);
        }
        try {
            ShowDocCredentials credentials = GSON.fromJson(value, ShowDocCredentials.class);
            return credentials == null ? new ShowDocCredentials(null, null) : credentials;
        } catch (RuntimeException ignored) {
            // Do not propagate parser messages: some Gson errors include the credential payload.
            throw new IllegalStateException("Stored ShowDoc credentials could not be read");
        }
    }

    private static @Nullable String applyMutation(@Nullable String current, @NotNull CredentialMutation mutation) {
        return switch (mutation.type()) {
            case KEEP -> current;
            case REPLACE -> mutation.value();
            case CLEAR -> null;
        };
    }

    private static void validateMutation(@NotNull CredentialMutation mutation) {
        if (mutation.type() == MutationType.REPLACE && (mutation.value() == null || mutation.value().isEmpty())) {
            throw new IllegalArgumentException("Replacement credentials must not be empty");
        }
    }

    private static boolean isConfigured(@Nullable String value) {
        return value != null && !value.isBlank();
    }

    @NotNull
    CredentialAttributes attributesFor(@NotNull Provider provider) {
        String projectHash = project.getLocationHash();
        String serviceKey = provider.displayName + " " + projectHash;
        String serviceName = CredentialAttributesKt.generateServiceName(SERVICE_NAME, serviceKey);
        return new CredentialAttributes(serviceName, projectHash);
    }

    private static void assertBackgroundThread() {
        if (EventQueue.isDispatchThread()) {
            throw new IllegalStateException("PasswordSafe access must run on a background thread");
        }
    }

    public enum Provider {
        SHOWDOC("ShowDoc"),
        YAPI("YApi"),
        YUQUE("YuQue");

        private final String displayName;

        Provider(String displayName) {
            this.displayName = displayName;
        }
    }

    public enum MutationType {
        KEEP,
        REPLACE,
        CLEAR
    }

    public record CredentialMutation(@NotNull MutationType type, @Nullable String value) {
        public static CredentialMutation keep() {
            return new CredentialMutation(MutationType.KEEP, null);
        }

        public static CredentialMutation replace(@NotNull String value) {
            return new CredentialMutation(MutationType.REPLACE, value);
        }

        public static CredentialMutation clear() {
            return new CredentialMutation(MutationType.CLEAR, null);
        }
    }

    public record CredentialStatus(boolean apiKeyConfigured, boolean apiTokenConfigured) {
    }

    public record ShowDocCredentials(@Nullable String apiKey, @Nullable String apiToken) {
    }

    interface SecretStore {
        @Nullable String get(@NotNull CredentialAttributes attributes);

        void set(@NotNull CredentialAttributes attributes, @Nullable String value);
    }

    private static final class PasswordSafeSecretStore implements SecretStore {
        @Override
        public @Nullable String get(@NotNull CredentialAttributes attributes) {
            return PasswordSafe.getInstance().getPassword(attributes);
        }

        @Override
        public void set(@NotNull CredentialAttributes attributes, @Nullable String value) {
            if (value == null) {
                PasswordSafe.getInstance().set(attributes, null);
            } else {
                PasswordSafe.getInstance().setPassword(attributes, value);
            }
        }
    }
}
