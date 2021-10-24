package com.liuzhihang.doc.view.ui.window;

import com.intellij.ide.util.treeView.NodeRenderer;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.ActionToolbar;
import com.intellij.openapi.actionSystem.DataProvider;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.SimpleToolWindowPanel;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiMethod;
import com.intellij.ui.treeStructure.SimpleTree;
import com.intellij.util.SlowOperations;
import com.liuzhihang.doc.view.DocViewBundle;
import com.liuzhihang.doc.view.data.DocViewDataKeys;
import com.liuzhihang.doc.view.dto.DocView;
import com.liuzhihang.doc.view.dto.DocViewData;
import com.liuzhihang.doc.view.notification.DocViewNotification;
import com.liuzhihang.doc.view.service.impl.SpringDocViewServiceImpl;
import com.liuzhihang.doc.view.utils.DocViewUtils;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import java.io.File;

/**
 * @author liuzhihang
 * @date 2021/10/22 16:25
 */
@Slf4j
public class DocViewWindowPanel extends SimpleToolWindowPanel implements DataProvider {

    private final SimpleTree catalogTree = new SimpleTree();
    private final Project project;

    public DocViewWindowPanel(@NotNull Project project) {
        super(Boolean.TRUE, Boolean.TRUE);

        this.project = project;
        initToolbar();
        initCatalogTree();
        setContent(catalogTree);
    }


    private void initToolbar() {

        final ActionManager actionManager = ActionManager.getInstance();
        ActionToolbar actionToolbar = actionManager.createActionToolbar("liuzhihang.doc.tool.window.toolbar",
                (DefaultActionGroup) actionManager.getAction("liuzhihang.doc.tool.window.toolbar.action"),
                true);
        actionToolbar.setTargetComponent(getToolbar());
        setToolbar(actionToolbar.getComponent());

    }

    /**
     * 初始化目录树
     */
    private void initCatalogTree() {

        catalogTree.setRootVisible(false);

        catalogTree.setCellRenderer(new NodeRenderer() {
            @Override
            public void customizeCellRenderer(@NotNull JTree tree, Object value, boolean selected, boolean expanded, boolean leaf, int row, boolean hasFocus) {

                DefaultMutableTreeNode node = (DefaultMutableTreeNode) value;
                Object userObject = node.getUserObject();
                if (userObject instanceof PsiClass) {
                    PsiClass psiClass = (PsiClass) userObject;
                    append(DocViewUtils.getTitle(psiClass));
                }

                if (userObject instanceof PsiMethod) {
                    PsiMethod psiMethod = (PsiMethod) userObject;
                    append(DocViewUtils.getName(psiMethod));
                }

            }
        });

        catalogTree.addTreeSelectionListener(e -> {
            SimpleTree simpleTree = (SimpleTree) e.getSource();
            if (simpleTree.getLastSelectedPathComponent() instanceof DefaultMutableTreeNode) {
                DefaultMutableTreeNode node = (DefaultMutableTreeNode) simpleTree.getLastSelectedPathComponent();
                Object userObject = node.getUserObject();
                if (userObject instanceof PsiMethod) {
                    PsiMethod psiMethod = (PsiMethod) userObject;

                    SpringDocViewServiceImpl service = ServiceManager.getService(SpringDocViewServiceImpl.class);
                    DocView docView = service.buildClassMethodDoc(project, psiMethod.getContainingClass(), psiMethod);

                    SlowOperations.allowSlowOperations(SlowOperations.GENERIC);
                    String markdownText = DocViewData.markdownText(project, docView);
                    String basePath = project.getBasePath();
                    File file = new File(basePath + "/.idea/doc-view/temp/" + docView.getName() + ".md");


                    try {
                        FileUtil.writeToFile(file, markdownText);
                    } catch (Exception ex) {
                        DocViewNotification.notifyError(project, DocViewBundle.message("notify.spring.error.method"));
                    }

                    ApplicationManager.getApplication().invokeAndWait(() -> {
                        VirtualFile vf = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(file);
                        OpenFileDescriptor descriptor = new OpenFileDescriptor(project, vf);
                        FileEditorManager.getInstance(project).openTextEditor(descriptor, false);
                    });
                }

            }
        });


    }

    @Override
    public @Nullable Object getData(@NotNull @NonNls String dataId) {

        if (DocViewDataKeys.WINDOW_CATALOG_TREE.is(dataId)) {
            return catalogTree;
        }


        return super.getData(dataId);
    }
}
