package com.liuzhihang.doc.view.action.toolbar.preview;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.JBPopup;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.ui.popup.PopupStep;
import com.intellij.openapi.ui.popup.util.BaseListPopupStep;
import com.liuzhihang.doc.view.action.toolbar.AbstractUploadAction;
import com.liuzhihang.doc.view.data.DocViewDataKeys;
import com.liuzhihang.doc.view.dto.DocView;
import com.liuzhihang.doc.view.service.DocViewUploadService;
import com.liuzhihang.doc.view.service.impl.ShowDocServiceImpl;
import com.liuzhihang.doc.view.service.impl.YApiServiceImpl;
import com.liuzhihang.doc.view.ui.PreviewForm;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.util.List;

/**
 * Preview 上传全部
 *
 * @author liuzhihang
 * @date 2021/10/23 22:31
 */
public class MenuUploadAllAction extends AbstractUploadAction {
    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {

        // 获取当前project对象
        Project project = e.getData(PlatformDataKeys.PROJECT);
        JBPopup popup = e.getData(DocViewDataKeys.PREVIEW_POPUP);
        List<DocView> docViewList = e.getData(DocViewDataKeys.PREVIEW_DOC_VIEW_LIST);
        JPanel previewToolbarPanel = e.getData(DocViewDataKeys.PREVIEW_TOOLBAR_PANEL);


        if (popup == null || project == null || docViewList == null || previewToolbarPanel == null) {
            return;
        }

        Point location = previewToolbarPanel.getLocationOnScreen();
        location.x = MouseInfo.getPointerInfo().getLocation().x;
        location.y += previewToolbarPanel.getHeight();

        PreviewForm.myIsPinned.set(true);

        JBPopupFactory.getInstance()
                .createListPopup(new BaseListPopupStep<>(null, "YApi", "ShowDoc") {
                    @Override
                    public @NotNull String getTextFor(String value) {
                        return "Upload to " + value;
                    }

                    @Override
                    public @Nullable PopupStep<?> onChosen(String selectedValue, boolean finalChoice) {

                        if (selectedValue.equals("YApi")) {
                            // 上传到 yapi
                            checkYApiSettings(project, popup);
                            upload(project, docViewList, YApiServiceImpl.class);

                        } else if (selectedValue.equals("ShowDoc")) {
                            // 上传到 ShowDoc
                            checkShowDocSettings(project, popup);
                            upload(project, docViewList, ShowDocServiceImpl.class);
                        }


                        return FINAL_CHOICE;
                    }
                }).showInScreenCoordinates(previewToolbarPanel, location);

    }

    private void upload(@NotNull Project project, @NotNull List<DocView> docViewList,
                        @NotNull Class<? extends DocViewUploadService> uploadService) {

        DocViewUploadService service = ServiceManager.getService(uploadService);

        ProgressManager.getInstance().run(new Task.Backgroundable(project, "Doc View upload", true) {
            @Override
            public void run(@NotNull ProgressIndicator progressIndicator) {
                service.upload(project, docViewList);
            }
        });
    }


}
