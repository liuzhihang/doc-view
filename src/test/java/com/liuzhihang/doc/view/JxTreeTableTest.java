package com.liuzhihang.doc.view;

import com.liuzhihang.doc.view.dto.DocViewParamData;
import com.intellij.ui.dualView.TreeTableView;
import com.intellij.ui.treeStructure.treetable.ListTreeTableModelOnColumns;
import com.liuzhihang.doc.view.ui.treeview.ParamTreeTableView;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;

/**
 * @author liuzhihang
 * @date 2021/8/3 17:02
 */
public class JxTreeTableTest {

    public static void main(String[] args) {

        DocViewParamData rootData = getDocViewParamData("root", "String", true, "this is root");
        DefaultMutableTreeNode rootNode = new DefaultMutableTreeNode(rootData);

        DocViewParamData child1Data = getDocViewParamData("child1", "String", true, "this is child1");
        DefaultMutableTreeNode child1 = new DefaultMutableTreeNode(child1Data);

        DocViewParamData child2Data = getDocViewParamData("child2", "String", true, "this is child2");
        DefaultMutableTreeNode child2 = new DefaultMutableTreeNode(child2Data);

        DocViewParamData child3Data = getDocViewParamData("child3", "String", true, "this is child3");
        DefaultMutableTreeNode child3 = new DefaultMutableTreeNode(child3Data);

        DocViewParamData child3Child1Data = getDocViewParamData("child3---A", "String", true, "this is child3---A");
        DefaultMutableTreeNode child3Child1 = new DefaultMutableTreeNode(child3Child1Data);
        DocViewParamData child3Child2Data = getDocViewParamData("child3---B", "String", true, "this is child3---B");
        DefaultMutableTreeNode child3Child2 = new DefaultMutableTreeNode(child3Child2Data);
        DocViewParamData child3Child3Data = getDocViewParamData("child3---C", "String", true, "this is child3---C");
        DefaultMutableTreeNode child3Child3 = new DefaultMutableTreeNode(child3Child3Data);
        DocViewParamData child3Child4Data = getDocViewParamData("child3---D", "String", true, "this is child3---D");
        DefaultMutableTreeNode child3Child4 = new DefaultMutableTreeNode(child3Child4Data);


        rootNode.add(child1);
        rootNode.add(child2);
        rootNode.add(child3);

        child3.add(child3Child1);
        child3.add(child3Child2);
        child3.add(child3Child3);
        child3.add(child3Child4);

        ListTreeTableModelOnColumns model = new ListTreeTableModelOnColumns(rootNode, ParamTreeTableView.COLUMN_INFOS);
        TreeTableView treeTable = new ParamTreeTableView(model);
        treeTable.setRootVisible(true);
        treeTable.setRowHeight(30);
        treeTable.getColumnModel().getColumn(0).setPreferredWidth(150);

        JFrame frame = new JFrame();
        frame.setBounds(400, 400, 800, 500);
        frame.getContentPane().add(new JScrollPane(treeTable));
        frame.setVisible(true);

        System.out.println(treeTable.getEditingRow());

    }

    private static DocViewParamData getDocViewParamData(String name, String type, boolean required, String desc) {
        DocViewParamData rootData = new DocViewParamData();
        rootData.setName(name);
        rootData.setType(type);
        rootData.setRequired(required);
        rootData.setDesc(desc);
        return rootData;
    }

}
