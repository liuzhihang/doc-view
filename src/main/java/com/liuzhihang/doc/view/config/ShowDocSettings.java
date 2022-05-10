package com.liuzhihang.doc.view.config;

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.project.Project;
import com.intellij.util.xmlb.XmlSerializerUtil;
import lombok.Data;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * ShowDoc 的相关设置
 *
 * @author liuzhihang
 * @date 2020/11/22 13:51
 */
@Data
@State(name = "DocViewShowDocSettingsComment", storages = {@Storage("DocViewShowDocSettings.xml")})
public class ShowDocSettings implements PersistentStateComponent<ShowDocSettings> {

    private String url;

    private String apiKey;

    private String apiToken;


    public static ShowDocSettings getInstance(@NotNull Project project) {
        return project.getService(ShowDocSettings.class);
    }

    @Override
    public @Nullable
    ShowDocSettings getState() {
        return this;
    }

    @Override
    public void loadState(@NotNull ShowDocSettings state) {

        XmlSerializerUtil.copyBean(state, this);
    }

}
