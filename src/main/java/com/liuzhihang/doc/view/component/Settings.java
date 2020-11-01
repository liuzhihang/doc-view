package com.liuzhihang.doc.view.component;

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.util.xmlb.XmlSerializerUtil;
import com.liuzhihang.doc.view.config.AnnotationConfig;
import com.liuzhihang.doc.view.config.ValidationConfig;
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
@State(name = "SettingsComponent", storages = {@Storage("DocViewSettings.xml")})
public class Settings implements PersistentStateComponent<Settings> {


    /**
     * 包含类注解名称
     */
    private Set<String> containClassAnnotationName = new HashSet<String>() {{
        add(AnnotationConfig.CONTROLLER);
        add(AnnotationConfig.REST_CONTROLLER);
    }};

    /**
     * 包含方法注解名称
     */
    private Set<String> containMethodAnnotationName = new HashSet<String>() {{
        add(AnnotationConfig.GET_MAPPING);
        add(AnnotationConfig.POST_MAPPING);
        add(AnnotationConfig.PUT_MAPPING);
        add(AnnotationConfig.DELETE_MAPPING);
        add(AnnotationConfig.PATCH_MAPPING);
        add(AnnotationConfig.REQUEST_MAPPING);
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
        add(ValidationConfig.NOT_BLANK);
        add(ValidationConfig.NOT_EMPTY);
        add(ValidationConfig.NOT_NULL);
    }};

    /**
     * 排除字段
     */
    private Set<String> excludeFieldNames = new HashSet<String>() {{
        add("serialVersionUID");
    }};


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
