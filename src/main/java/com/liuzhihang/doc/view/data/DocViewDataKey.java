package com.liuzhihang.doc.view.data;

import com.intellij.openapi.actionSystem.DataKey;
import com.intellij.ui.treeStructure.Tree;

/**
 * @author liuzhihang
 * @date 2021/10/23 21:04
 */
public class DocViewDataKey {

    /**
     * 目录树
     */
    public static final DataKey<Tree> WINDOW_CATALOG_TREE = DataKey.create("DocViewWindowTree");

}
