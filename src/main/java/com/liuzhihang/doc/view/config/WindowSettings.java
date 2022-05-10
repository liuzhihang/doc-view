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
 * 配置持久保存
 * <p>
 * <p>
 * See <a href="http://www.jetbrains.org/intellij/sdk/docs/basics/persisting_state_of_components.html">IntelliJ Platform SDK DevGuide</a>
 *
 * @author liuzhihang
 * @date 2020/2/27 19:02
 */
@Data
@State(name = "DocViewWindowSettingsComponent", storages = {@Storage("DocViewWindowSettings.xml")})
public class WindowSettings implements PersistentStateComponent<WindowSettings> {

    /**
     * 范围
     */
    private String scope;

    /**
     * 包含接口
     */
    private boolean includeInterface = false;


    public static WindowSettings getInstance(@NotNull Project project) {
        return project.getService(WindowSettings.class);
    }


    @Nullable
    @Override
    public WindowSettings getState() {
        return this;
    }

    @Override
    public void loadState(@NotNull WindowSettings state) {
        XmlSerializerUtil.copyBean(state, this);
    }


}
