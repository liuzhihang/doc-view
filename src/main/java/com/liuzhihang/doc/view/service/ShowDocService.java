package com.liuzhihang.doc.view.service;

import com.intellij.openapi.project.Project;
import com.liuzhihang.doc.view.dto.DocView;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * 上传到 showDoc
 *
 * @author liuzhihang
 * @date 2021/7/27 12:00
 */
public interface ShowDocService {


    /**
     * 上传到 showDoc
     *
     * @param project
     * @param docViewList
     */
    void upload(@NotNull Project project, @NotNull List<DocView> docViewList);

    /**
     * 上传到 showDoc
     *
     * @param project
     * @param docView
     */
    void upload(@NotNull Project project, @NotNull DocView docView);
}
