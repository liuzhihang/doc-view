package com.liuzhihang.doc.view.config;

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.project.Project;
import com.intellij.util.xmlb.XmlSerializerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * DocView 的标签设置
 *
 * @author liuzhihang
 * @date 2020/11/22 13:51
 */
@State(name = "TagsSettingsComment", storages = {@Storage("DocViewTagsSettings.xml")})
public class TagsSettings implements PersistentStateComponent<TagsSettings> {


    private String name = "docName";
    private String required = "required";


    public static TagsSettings getInstance(@NotNull Project project) {
        return ServiceManager.getService(project, TagsSettings.class);
    }


    @Nullable
    @Override
    public TagsSettings getState() {
        return this;
    }

    @Override
    public void loadState(@NotNull TagsSettings state) {
        XmlSerializerUtil.copyBean(state, this);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getRequired() {
        return required;
    }

    public void setRequired(String required) {
        this.required = required;
    }
}
