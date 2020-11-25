package com.liuzhihang.doc.view.action.toolbar;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiUtil;
import com.liuzhihang.doc.view.dto.DocView;
import com.liuzhihang.doc.view.service.DocViewService;
import com.liuzhihang.doc.view.service.impl.ToolWindowServiceImpl;
import com.liuzhihang.doc.view.tool.DocViewToolWindow;
import com.liuzhihang.doc.view.tool.DocViewToolWindowFactory;
import com.liuzhihang.doc.view.utils.ExportUtils;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;
import java.util.HashMap;
import java.util.Map;

/**
 * 工具栏 - 导出
 *
 * @author lvgorice@gmail.com
 * @version 1.0
 * @date 2020-11-20
 */
public class ExportAction extends AnAction {

    @Override
    public void actionPerformed(@NotNull AnActionEvent actionEvent) {
        Project project = actionEvent.getProject();
        assert project != null;
        JTree tree = DocViewToolWindowFactory.getDataContext(project).getData(DocViewToolWindow.DOC_VIEW_TREE);
        assert tree != null;
        TreePath[] selectionPaths = tree.getSelectionPaths();
        assert selectionPaths != null;
        Map<String, Map<String, DocView>> generatedDocView = new HashMap<>(32);
        // 逐个文件处理
        for (TreePath selectionPath : selectionPaths) {
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) selectionPath.getLastPathComponent();
            if (node.getUserObject().toString().endsWith(".java")) {
                String key = node.getParent().toString() + node.getUserObject();
                VirtualFile virtualFile = ToolWindowServiceImpl.virtualFileCache.get(key);
                PsiFile psiFile = PsiUtil.getPsiFile(project, virtualFile);
                DocViewService docViewService = DocViewService.getInstance(project, psiFile);
                assert docViewService != null;
                // 如果文件生成成功
                Map<String, DocView> docViewMap = docViewService.generatorDocView(project, psiFile);
                if (docViewMap.size() > 0) {
                    generatedDocView.put(psiFile.getName(), docViewMap);
                }
            } else {
                // TODO: 如果选择的是目录，则导出该目录下全部内容
//                for (int i = 0; i < node.getChildCount(); i++) {}
            }
        }
        // 导出
        if (generatedDocView.size() > 0) {
            ExportUtils.bathExportMarkdown(project, generatedDocView);
        }
    }
}
