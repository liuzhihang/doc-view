package com.liuzhihang.doc.view.config;

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.util.xmlb.XmlSerializerUtil;
import lombok.Data;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * IDE 应用级的首页与更新提示配置
 * <p>
 * <p>
 * See <a href="http://www.jetbrains.org/intellij/sdk/docs/basics/persisting_state_of_components.html">IntelliJ Platform SDK DevGuide</a>
 *
 * @author liuzhihang
 * @date 2020/2/27 19:02
 */
@Data
@State(name = "DocViewApplicationSettingsComponent", storages = {@Storage("DocViewApplicationSettings.xml")})
public class ApplicationSettings implements PersistentStateComponent<ApplicationSettings> {

    private String pluginVersion = "";
    private boolean showUpdateNotification = true;


    public static ApplicationSettings getInstance() {
        return ApplicationManager.getApplication().getService(ApplicationSettings.class);
    }


    @Nullable
    @Override
    public ApplicationSettings getState() {
        return this;
    }

    @Override
    public void loadState(@NotNull ApplicationSettings state) {
        XmlSerializerUtil.copyBean(state, this);
    }


}
