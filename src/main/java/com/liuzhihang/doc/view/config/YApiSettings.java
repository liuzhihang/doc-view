package com.liuzhihang.doc.view.config;

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.project.Project;
import com.intellij.util.xmlb.XmlSerializerUtil;
import com.liuzhihang.doc.view.constant.Constant;
import lombok.Data;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * YApi 的相关设置
 *
 * @author liuzhihang
 * @date 2020/11/22 13:51
 */
@Data
@State(name = "DocViewYApiSettingsComment", storages = {@Storage(Constant.DOC_VIEW + "/YApiSettings.xml")})
public class YApiSettings implements PersistentStateComponent<YApiSettings> {

    private String url;

    private Long projectId;

    private String token;


    public static YApiSettings getInstance(@NotNull Project project) {
        return ServiceManager.getService(project, YApiSettings.class);
    }

    @Override
    public @Nullable
    YApiSettings getState() {
        return this;
    }

    @Override
    public void loadState(@NotNull YApiSettings state) {

        XmlSerializerUtil.copyBean(state, this);
    }

}
