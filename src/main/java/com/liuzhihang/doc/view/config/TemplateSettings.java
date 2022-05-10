package com.liuzhihang.doc.view.config;

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.project.Project;
import com.intellij.util.xmlb.XmlSerializerUtil;
import com.liuzhihang.doc.view.DocViewBundle;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * DocView 模版设置
 *
 * @author liuzhihang
 * @date 2020/11/22 13:51
 */
@State(name = "TemplateSettingsComponent", storages = {@Storage("DocViewTemplateSettings.xml")})
public class TemplateSettings implements PersistentStateComponent<TemplateSettings> {

    /**
     * spring 的模版
     */
    private String springTemplate = DocViewBundle.message("template.spring.init");
    private String dubboTemplate = DocViewBundle.message("template.dubbo.init");

    public static TemplateSettings getInstance(@NotNull Project project) {
        return project.getService(TemplateSettings.class);
    }


    @Nullable
    @Override
    public TemplateSettings getState() {
        return this;
    }

    @Override
    public void loadState(@NotNull TemplateSettings state) {
        XmlSerializerUtil.copyBean(state, this);
    }


    public String getSpringTemplate() {
        return springTemplate;
    }

    public void setSpringTemplate(String springTemplate) {
        this.springTemplate = springTemplate;
    }

    public String getDubboTemplate() {
        return dubboTemplate;
    }

    public void setDubboTemplate(String dubboTemplate) {
        this.dubboTemplate = dubboTemplate;
    }
}
