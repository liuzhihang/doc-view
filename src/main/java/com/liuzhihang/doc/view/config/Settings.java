package com.liuzhihang.doc.view.config;

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.project.Project;
import com.intellij.util.xmlb.XmlSerializerUtil;
import com.liuzhihang.doc.view.constant.AnnotationConstant;
import com.liuzhihang.doc.view.constant.ValidationConstant;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.Set;

/**
 * 配置持久保存
 * <p>
 * <p>
 * See <a href="http://www.jetbrains.org/intellij/sdk/docs/basics/persisting_state_of_components.html">IntelliJ Platform SDK DevGuide</a>
 *
 * @author liuzhihang
 * @date 2020/2/27 19:02
 */
@State(name = "DocViewSettingsComponent", storages = {@Storage("DocViewSettings.xml")})
public class Settings implements PersistentStateComponent<Settings> {


    /**
     * 包含类注解名称
     */
    private Set<String> containClassAnnotationName = new HashSet<String>() {{
        add(AnnotationConstant.CONTROLLER);
        add(AnnotationConstant.REST_CONTROLLER);
    }};

    /**
     * 包含方法注解名称
     */
    private Set<String> containMethodAnnotationName = new HashSet<String>() {{
        add(AnnotationConstant.GET_MAPPING);
        add(AnnotationConstant.POST_MAPPING);
        add(AnnotationConstant.PUT_MAPPING);
        add(AnnotationConstant.DELETE_MAPPING);
        add(AnnotationConstant.PATCH_MAPPING);
        add(AnnotationConstant.REQUEST_MAPPING);
    }};


    /**
     * 排除参数
     */
    private Set<String> excludeParamTypes = new HashSet<String>() {{
        add("HttpServletRequest");
        add("ServletRequest");
        add("HttpServletResponse");
        add("ServletResponse");
    }};

    /**
     * 字段必填注解
     */
    private Set<String> fieldRequiredAnnotationName = new HashSet<String>() {{
        add(ValidationConstant.NOT_BLANK);
        add(ValidationConstant.NOT_EMPTY);
        add(ValidationConstant.NOT_NULL);
    }};

    /**
     * 排除字段
     */
    private Set<String> excludeFieldNames = new HashSet<String>() {{
        add("serialVersionUID");
    }};

    public static Settings getInstance(@NotNull Project project) {
        return ServiceManager.getService(project, Settings.class);
    }


    @Nullable
    @Override
    public Settings getState() {
        return this;
    }

    @Override
    public void loadState(@NotNull Settings state) {
        XmlSerializerUtil.copyBean(state, this);
    }

    public Set<String> getContainClassAnnotationName() {
        return containClassAnnotationName;
    }

    public void setContainClassAnnotationName(Set<String> containClassAnnotationName) {
        this.containClassAnnotationName = containClassAnnotationName;
    }

    public Set<String> getContainMethodAnnotationName() {
        return containMethodAnnotationName;
    }

    public void setContainMethodAnnotationName(Set<String> containMethodAnnotationName) {
        this.containMethodAnnotationName = containMethodAnnotationName;
    }

    public Set<String> getExcludeParamTypes() {
        return excludeParamTypes;
    }

    public void setExcludeParamTypes(Set<String> excludeParamTypes) {
        this.excludeParamTypes = excludeParamTypes;
    }

    public Set<String> getFieldRequiredAnnotationName() {
        return fieldRequiredAnnotationName;
    }

    public void setFieldRequiredAnnotationName(Set<String> fieldRequiredAnnotationName) {
        this.fieldRequiredAnnotationName = fieldRequiredAnnotationName;
    }

    public Set<String> getExcludeFieldNames() {
        return excludeFieldNames;
    }

    public void setExcludeFieldNames(Set<String> excludeFieldNames) {
        this.excludeFieldNames = excludeFieldNames;
    }
}
