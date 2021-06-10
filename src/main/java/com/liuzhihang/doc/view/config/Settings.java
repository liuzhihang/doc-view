package com.liuzhihang.doc.view.config;

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.project.Project;
import com.intellij.util.xmlb.XmlSerializerUtil;
import com.liuzhihang.doc.view.constant.SpringConstant;
import com.liuzhihang.doc.view.constant.ValidationConstant;
import lombok.Data;
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
@Data
@State(name = "DocViewSettingsComponent", storages = {@Storage("DocViewSettings.xml")})
public class Settings implements PersistentStateComponent<Settings> {


    private String name = "docName";
    private String required = "required";

    /**
     * 包含类注解名称
     */
    private Set<String> containClassAnnotationName = new HashSet<>() {{
        add(SpringConstant.CONTROLLER);
        add(SpringConstant.REST_CONTROLLER);
    }};

    /**
     * 包含方法注解名称
     */
    private Set<String> containMethodAnnotationName = new HashSet<>() {{
        add(SpringConstant.GET_MAPPING);
        add(SpringConstant.POST_MAPPING);
        add(SpringConstant.PUT_MAPPING);
        add(SpringConstant.DELETE_MAPPING);
        add(SpringConstant.PATCH_MAPPING);
        add(SpringConstant.REQUEST_MAPPING);
    }};


    /**
     * 排除参数
     */
    private Set<String> excludeParamTypes = new HashSet<>() {{
        add("HttpServletRequest");
        add("ServletRequest");
        add("HttpServletResponse");
        add("ServletResponse");
    }};

    /**
     * 字段必填注解
     */
    private Set<String> fieldRequiredAnnotationName = new HashSet<>() {{
        add(ValidationConstant.NOT_BLANK);
        add(ValidationConstant.NOT_EMPTY);
        add(ValidationConstant.NOT_NULL);
    }};

    /**
     * 排除字段
     */
    private Set<String> excludeFieldNames = new HashSet<>() {{
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


}
