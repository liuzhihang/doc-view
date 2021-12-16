package com.liuzhihang.doc.view.utils;

import com.intellij.openapi.project.Project;
import com.intellij.project.ProjectKt;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;

/**
 * @author liaozan
 * @since 2021/12/16
 */
public class StorageUtils {

    public static Path getConfigDir(Project project) {
        Path configDir = ProjectKt.getStateStore(project).getDirectoryStorePath();
        return Paths.get(Objects.requireNonNull(configDir).toString(), "doc-view", "temp");
    }

}
