package com.liuzhihang.doc.view.data;

import com.intellij.openapi.actionSystem.DataKey;
import com.intellij.openapi.ui.popup.JBPopup;
import com.intellij.ui.treeStructure.SimpleTree;
import com.liuzhihang.doc.view.dto.DocView;
import com.liuzhihang.doc.view.ui.PreviewForm;
import com.liuzhihang.doc.view.ui.window.DocViewWindowPanel;
import com.liuzhihang.doc.view.ui.window.RootNode;

import javax.swing.*;
import java.util.List;

/**
 * @author liuzhihang
 * @date 2021/10/23 21:04
 */
public class DocViewDataKeys {

    /**
     * 目录树
     */
    public static final DataKey<DocViewWindowPanel> WINDOW_PANE = DataKey.create("DocViewWindowPane");
    public static final DataKey<RootNode> WINDOW_ROOT_NODE = DataKey.create("DocViewWindowRootNode");
    public static final DataKey<SimpleTree> WINDOW_CATALOG_TREE = DataKey.create("DocViewWindowTree");
    public static final DataKey<JComponent> WINDOW_TOOLBAR = DataKey.create("DocViewWindowToolbar");


    /**
     * Preview 界面
     */
    public static final DataKey<PreviewForm> PREVIEW_FORM = DataKey.create("PreviewForm");
    public static final DataKey<JBPopup> PREVIEW_POPUP = DataKey.create("PreviewPop");
    public static final DataKey<DocView> PREVIEW_CURRENT_DOC_VIEW = DataKey.create("PreviewCurrentDocView");
    public static final DataKey<String> PREVIEW_MARKDOWN_TEXT = DataKey.create("PreviewMarkdownText");
    public static final DataKey<List<DocView>> PREVIEW_DOC_VIEW_LIST = DataKey.create("PreviewDocViewList");
    public static final DataKey<JPanel> PREVIEW_TOOLBAR_PANEL = DataKey.create("PreviewToolbarPanel");
}
