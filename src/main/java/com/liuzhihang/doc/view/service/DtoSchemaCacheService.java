package com.liuzhihang.doc.view.service;

import com.intellij.openapi.components.Service;
import com.intellij.openapi.project.Project;
import com.intellij.psi.util.PsiModificationTracker;
import com.liuzhihang.doc.view.config.Settings;
import com.liuzhihang.doc.view.config.TemplateSettings;
import com.liuzhihang.doc.view.dto.Body;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Service(Service.Level.PROJECT)
public final class DtoSchemaCacheService {

    private final Project project;
    private final ConcurrentMap<DtoSchemaCacheKey, Body> bodyCache = new ConcurrentHashMap<>();

    public DtoSchemaCacheService(@NotNull Project project) {
        this.project = project;
    }

    public static DtoSchemaCacheService getInstance(@NotNull Project project) {
        return project.getService(DtoSchemaCacheService.class);
    }

    @Nullable
    public Body getBody(@NotNull DtoSchemaCacheKey key) {
        Body cached = bodyCache.get(key);
        return cached == null ? null : copyBody(cached, null, false);
    }

    public void putBody(@NotNull DtoSchemaCacheKey key, @NotNull Body body) {
        bodyCache.put(key, copyBody(body, null, false));
    }

    public void invalidate(@NotNull DtoSchemaCacheKey key) {
        bodyCache.remove(key);
    }

    public void clear() {
        bodyCache.clear();
    }

    public DtoSchemaCacheKey key(@NotNull String role, @NotNull String typeIdentity) {
        return new DtoSchemaCacheKey(
                role,
                typeIdentity,
                PsiModificationTracker.getInstance(project).getModificationCount(),
                settingsHash(),
                templateHash()
        );
    }

    @NotNull
    public static Body copyBody(@NotNull Body source, Body parent, boolean keepPsiElement) {
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
            copy.getChildList().add(copyBody(child, copy, keepPsiElement));
        }
        return copy;
    }

    private int settingsHash() {
        Settings settings = Settings.getInstance(project);
        return Objects.hash(
                settings.getRequired(),
                settings.getRequiredUseCommentTag(),
                settings.getFieldNameJsonProperty(),
                settings.getContainClassAnnotationName(),
                settings.getContainMethodAnnotationName(),
                settings.getExcludeParamTypes(),
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
    }
}
