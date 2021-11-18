package com.liuzhihang.doc.view;

import com.liuzhihang.doc.view.dto.DocViewParamData;
import org.jdesktop.swingx.JXTreeTable;
import org.jdesktop.swingx.treetable.DefaultMutableTreeTableNode;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

import static javax.swing.ListSelectionModel.SINGLE_SELECTION;

/**
 * @author liuzhihang
 * @date 2021/8/3 17:02
 */
public class JxTreeTableTest {

    public static void main(String[] args) {

        DocViewParamData rootData = getDocViewParamData("root", "String", true, "this is root");
        DefaultMutableTreeTableNode rootNode = new DefaultMutableTreeTableNode(rootData);

        DocViewParamData child1Data = getDocViewParamData("child1", "String", true, "this is child1");
        DefaultMutableTreeTableNode child1 = new DefaultMutableTreeTableNode(child1Data);

        DocViewParamData child2Data = getDocViewParamData("child2", "String", true, "this is child2");
        DefaultMutableTreeTableNode child2 = new DefaultMutableTreeTableNode(child2Data);

        DocViewParamData child3Data = getDocViewParamData("child3", "String", true, "this is child3");
        DefaultMutableTreeTableNode child3 = new DefaultMutableTreeTableNode(child3Data);

        DocViewParamData child3Child1Data = getDocViewParamData("child3---A", "String", true, "this is child3---A");
        DefaultMutableTreeTableNode child3Child1 = new DefaultMutableTreeTableNode(child3Child1Data);
        DocViewParamData child3Child2Data = getDocViewParamData("child3---B", "String", true, "this is child3---B");
        DefaultMutableTreeTableNode child3Child2 = new DefaultMutableTreeTableNode(child3Child2Data);
        DocViewParamData child3Child3Data = getDocViewParamData("child3---C", "String", true, "this is child3---C");
        DefaultMutableTreeTableNode child3Child3 = new DefaultMutableTreeTableNode(child3Child3Data);
        DocViewParamData child3Child4Data = getDocViewParamData("child3---D", "String", true, "this is child3---D");
        DefaultMutableTreeTableNode child3Child4 = new DefaultMutableTreeTableNode(child3Child4Data);


        rootNode.add(child1);
        rootNode.add(child2);
        rootNode.add(child3);

        child3.add(child3Child1);
        child3.add(child3Child2);
        child3.add(child3Child3);
        child3.add(child3Child4);

        JXTreeTable treeTable = new JXTreeTable();

        treeTable.getColumnModel().getColumn(0).setPreferredWidth(150);
        // treeTable.expandAll();
        // treeTable.setCellSelectionEnabled(false);
        treeTable.setRowHeight(30);
        treeTable.setLeafIcon(null);
        // treeTable.setCollapsedIcon(null);
        // treeTable.setExpandedIcon(null);
        treeTable.setOpenIcon(null);
        treeTable.setClosedIcon(null);

        final DefaultListSelectionModel defaultListSelectionModel = new DefaultListSelectionModel();
        treeTable.setSelectionModel(defaultListSelectionModel);

        defaultListSelectionModel.setSelectionMode(SINGLE_SELECTION);
        defaultListSelectionModel.addListSelectionListener(e -> defaultListSelectionModel.clearSelection());

        JScrollPane jScrollPane = new JScrollPane();
        jScrollPane.setViewportView(treeTable);

        JFrame frame = new JFrame();
        frame.setBounds(400, 400, 800, 500);
        frame.getContentPane().add(jScrollPane);
        frame.setVisible(true);

        System.out.println(treeTable.getEditingRow());

    }

    @NotNull
    private static DocViewParamData getDocViewParamData(String name, String type, boolean required, String desc) {
        DocViewParamData rootData = new DocViewParamData();
        rootData.setName(name);
        rootData.setType(type);
        rootData.setRequired(required);
        rootData.setDesc(desc);
        return rootData;
    }

}
