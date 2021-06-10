package com.liuzhihang.doc.view.service;

import com.intellij.openapi.project.Project;
import com.liuzhihang.doc.view.dto.DocView;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

/**
 * @author liuzhihang
 * @date 2021/6/8 23:56
 */
public interface YApiService {


    /**
     * 上传到 yapi
     *
     * @param project
     * @param docViewMap
     */
    void upload(@NotNull Project project, @NotNull Map<String, DocView> docViewMap);

    /**
     * 上传到 yapi
     *
     * @param project
     * @param docView
     */
    void upload(@NotNull Project project, @NotNull DocView docView);
}
