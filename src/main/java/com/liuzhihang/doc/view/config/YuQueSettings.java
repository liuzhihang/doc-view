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
@State(name = "DocViewYuQueSettingsComment", storages = {@Storage("DocViewYuQueSettings.xml")})
public class YuQueSettings implements PersistentStateComponent<YuQueSettings> {

    private String url = "https://www.yuque.com";
    private String apiUrl = "https://www.yuque.com/api/v2";

    private String token;

    private String login;

    private String repos;


    public static YuQueSettings getInstance(@NotNull Project project) {
        return project.getService(YuQueSettings.class);
    }

    @Override
    public @Nullable
    YuQueSettings getState() {
        return this;
    }

    @Override
    public void loadState(@NotNull YuQueSettings state) {

        XmlSerializerUtil.copyBean(state, this);
    }

    public String getNamespace() {
        return login + "/" + repos;
    }

}
