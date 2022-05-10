package com.liuzhihang.doc.view.config;

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.project.Project;
import com.intellij.util.xmlb.XmlSerializerUtil;
import com.liuzhihang.doc.view.constant.LombokConstant;
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

    /**
     * DocView 的 注释 tag 统一使用 DocView 当做前缀, 防止和用户自身的注释 tag 冲突
     */
    private String titleTag = "DocView.Title";
    private Boolean titleUseCommentTag = true;
    private Boolean titleClassComment = true;
    private Boolean titleUseSimpleClassName = true;
    private Boolean titleUseFullClassName = true;

    /**
     * 接口名字
     */
    private String nameTag = "DocView.Name";
    private Boolean nameUseSwagger3 = true;
    private Boolean nameUseSwagger = true;
    private Boolean nameUseCommentTag = true;
    private Boolean nameMethodComment = true;

    /**
     * 文档描述
     */
    private Boolean descUseSwagger3 = true;
    private Boolean descUseSwagger = true;

    /**
     * 字段是否必填
     */
    private String required = "DocView.Required";
    private Boolean requiredUseCommentTag = true;

    /**
     * 是否合并导出
     */
    private Boolean mergeExport = true;

    /**
     * 隐藏左侧目录
     */
    private Boolean hideLeft = false;

    /**
     * 是否显示边栏标记
     */
    private Boolean lineMarker = true;
    private Boolean includeNormalInterface = false;

    /**
     * 包含类注解名称
     */
    private Set<String> containClassAnnotationName = new HashSet<>() {{
        add(SpringConstant.CONTROLLER);
        add(SpringConstant.REST_CONTROLLER);
        add(SpringConstant.FEIGN_CLIENT);
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
    private Set<String> requiredFieldAnnotation = new HashSet<>() {{
        add(ValidationConstant.NOT_BLANK);
        add(ValidationConstant.NOT_EMPTY);
        add(ValidationConstant.NOT_NULL);
        add(LombokConstant.NON_NULL);
    }};

    /**
     * 排除字段
     */
    private Set<String> excludeFieldNames = new HashSet<>() {{
        add("serialVersionUID");
    }};

    /**
     * 排除字段
     */
    private Set<String> excludeParameterType = new HashSet<>() {{
        add("org.springframework.core.io.InputStreamSource");
        add("javax.servlet.ServletResponse");
        add("javax.servlet.ServletRequest");
    }};

    /**
     * 被注解的字段需要过滤掉
     */
    private Set<String> excludeFieldAnnotation = new HashSet<>() {{
        add("javax.annotation.Resource");
        add("org.springframework.beans.factory.annotation.Autowired");
        add("org.apache.dubbo.config.annotation.Reference");
        add("org.apache.dubbo.config.annotation.DubboReference");
    }};

    /**
     * 被注解的字段需要过滤掉
     */
    private Set<String> excludeClassPackage = new HashSet<>() {{
        add("com.baomidou.mybatisplus.extension.activerecord.Model");
    }};

    /**
     * 子属性缩进符
     */
    private String prefixSymbol1 = "";
    private String prefixSymbol2 = "-->";


    public static Settings getInstance(@NotNull Project project) {
        return project.getService(Settings.class);
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
