package com.liuzhihang.doc.view.service;

import com.intellij.openapi.components.Service;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;
import com.intellij.psi.util.PsiModificationTracker;
import com.liuzhihang.doc.view.config.Settings;
import com.liuzhihang.doc.view.config.TemplateSettings;
import com.liuzhihang.doc.view.dto.Body;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@Service(Service.Level.PROJECT)
public final class DtoSchemaCacheService {

    private final Project project;
    private final Object cacheLock = new Object();
    private final Map<DtoSchemaCacheKey, Body> bodyCache = new HashMap<>();
    private CacheEpoch cacheEpoch;

    public DtoSchemaCacheService(@NotNull Project project) {
        this.project = project;
    }

    public static DtoSchemaCacheService getInstance(@NotNull Project project) {
        return project.getService(DtoSchemaCacheService.class);
    }

    @Nullable
    public Body getBody(@NotNull DtoSchemaCacheKey key) {
        Body cached;
        synchronized (cacheLock) {
            CacheEpoch currentEpoch = observeEpochLocked();
            if (!key.matches(currentEpoch)) {
                return null;
            }
            cached = bodyCache.get(key);
        }
        if (cached == null) {
            return null;
        }

        Body copy = copyBody(cached, null, false);
        synchronized (cacheLock) {
            CacheEpoch currentEpoch = observeEpochLocked();
            return key.matches(currentEpoch) && bodyCache.get(key) == cached ? copy : null;
        }
    }

    public void putBody(@NotNull DtoSchemaCacheKey key, @NotNull Body body) {
        synchronized (cacheLock) {
            if (!key.matches(observeEpochLocked())) {
                return;
            }
        }

        Body copy = copyBody(body, null, false);
        synchronized (cacheLock) {
            CacheEpoch currentEpoch = observeEpochLocked();
            if (key.matches(currentEpoch)) {
                bodyCache.put(key, copy);
            }
        }
    }

    public void invalidate(@NotNull DtoSchemaCacheKey key) {
        synchronized (cacheLock) {
            CacheEpoch currentEpoch = observeEpochLocked();
            if (key.matches(currentEpoch)) {
                bodyCache.remove(key);
            }
        }
    }

    public void clear() {
        synchronized (cacheLock) {
            bodyCache.clear();
        }
    }

    public DtoSchemaCacheKey key(@NotNull String role, @NotNull String typeIdentity) {
        synchronized (cacheLock) {
            CacheEpoch currentEpoch = observeEpochLocked();
            return new DtoSchemaCacheKey(role, typeIdentity, currentEpoch.modificationCount(),
                    currentEpoch.settingsHash(), currentEpoch.templateHash());
        }
    }

    @NotNull
    public static Body copyBody(@NotNull Body source, Body parent, boolean keepPsiElement) {
        ProgressManager.checkCanceled();
        Body copy = new Body();
        if (keepPsiElement) {
            copy.setPsiElement(source.getPsiElement());
        }
        copy.setRequired(source.getRequired());
        copy.setName(source.getName());
        copy.setExample(source.getExample());
        copy.setDesc(source.getDesc());
        copy.setType(source.getType());
        copy.setSince(source.getSince());
        copy.setVersion(source.getVersion());
        copy.setParent(parent);
        copy.setQualifiedNameForClassType(source.getQualifiedNameForClassType());
        copy.setCollection(source.isCollection());
        copy.setMap(source.isMap());
        for (Body child : source.getChildList()) {
            ProgressManager.checkCanceled();
            copy.getChildList().add(copyBody(child, copy, keepPsiElement));
        }
        return copy;
    }

    private CacheEpoch observeEpochLocked() {
        CacheEpoch currentEpoch = new CacheEpoch(
                PsiModificationTracker.getInstance(project).getModificationCount(),
                settingsHash(),
                templateHash()
        );
        if (!currentEpoch.equals(cacheEpoch)) {
            bodyCache.clear();
            cacheEpoch = currentEpoch;
        }
        return currentEpoch;
    }

    private int settingsHash() {
        Settings settings = Settings.getInstance(project);
        return Objects.hash(
                settings.getRequired(),
                settings.getRequiredUseCommentTag(),
                settings.getFieldNameJsonProperty(),
                settings.getContainClassAnnotationName(),
                settings.getContainMethodAnnotationName(),
                settings.getRequiredFieldAnnotation(),
                settings.getFieldNameAnnotation(),
                settings.getExcludeFieldNames(),
                settings.getExcludeParameterType(),
                settings.getExcludeFieldAnnotation(),
                settings.getExcludeClassPackage(),
                settings.getPrefixSymbol1(),
                settings.getPrefixSymbol2(),
                settings.getSeparateParam()
        );
    }

    private int templateHash() {
        TemplateSettings templateSettings = TemplateSettings.getInstance(project);
        return Objects.hash(templateSettings.getSpringTemplate(), templateSettings.getDubboTemplate());
    }

    public record DtoSchemaCacheKey(String role,
                                    String typeIdentity,
                                    long modificationCount,
                                    int settingsHash,
                                    int templateHash) {

        private boolean matches(CacheEpoch epoch) {
            return modificationCount == epoch.modificationCount()
                    && settingsHash == epoch.settingsHash()
                    && templateHash == epoch.templateHash();
        }
    }

    private record CacheEpoch(long modificationCount, int settingsHash, int templateHash) {
    }
}
