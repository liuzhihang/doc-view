package com.liuzhihang.doc.view;

import com.intellij.ui.treeStructure.treetable.ListTreeTableModel;
import com.intellij.ui.treeStructure.treetable.TreeTable;
import com.intellij.ui.treeStructure.treetable.TreeTableCellRenderer;
import com.intellij.ui.treeStructure.treetable.TreeTableModel;
import com.intellij.util.ui.ColumnInfo;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;

/**
 * @author liuzhihang
 * @date 2020/10/27 16:47
 */
public class TreeTableTest {

    static ColumnInfo name = new ColumnInfo("Name") {
        @Nullable
        @Override
        public Object valueOf(Object o) {
            if (o instanceof CustomNode) {
                return ((CustomNode) o).getName();
            } else return o;
        }
    };

    static ColumnInfo value = new ColumnInfo("Value") {
        @Nullable
        @Override
        public Object valueOf(Object o) {
            if (o instanceof CustomNode) {
                return ((CustomNode) o).getValue();
            } else return o;
        }
    };
    static ColumnInfo[] columns = {name, value};

    public static void main(String[] args) {

        DefaultMutableTreeNode root = new DefaultMutableTreeNode("I'm the root");


        DefaultMutableTreeNode first = new CustomNode("I am first name", "I am first value");
        DefaultMutableTreeNode second = new CustomNode("I am second name", "I am second value");
        root.add(first);
        root.add(second);

        DefaultMutableTreeNode child1 = new CustomNode("I am child name1", "I am child value");
        DefaultMutableTreeNode child2 = new CustomNode("I am child name2", "I am child value");
        DefaultMutableTreeNode child3 = new CustomNode("I am child name3", "I am child value");


        second.add(child1);
        second.add(child2);
        second.add(child3);

        ListTreeTableModel model = new ListTreeTableModel(root, columns);
        TreeTable treeTable = new TreeTable(model) {
            @Override
            public TreeTableCellRenderer createTableRenderer(TreeTableModel treeTableModel) {
                TreeTableCellRenderer tableRenderer = super.createTableRenderer(treeTableModel);
                // UIUtil.setLineStyleAngled(this.getTree());
                tableRenderer.setRootVisible(true);
                tableRenderer.setShowsRootHandles(true);


                return tableRenderer;
            }

            @Override
            public TableCellRenderer getCellRenderer(int row, int column) {
                TreePath treePath = getTree().getPathForRow(row);
                if (treePath == null) return super.getCellRenderer(row, column);

                Object node = treePath.getLastPathComponent();

                @SuppressWarnings("unchecked")
                TableCellRenderer renderer = columns[column].getRenderer(node);
                return renderer == null ? super.getCellRenderer(row, column) : renderer;
            }

            @Override
            public TableCellEditor getCellEditor(int row, int column) {
                TreePath treePath = getTree().getPathForRow(row);
                if (treePath == null) return super.getCellEditor(row, column);

                Object node = treePath.getLastPathComponent();
                @SuppressWarnings("unchecked")
                TableCellEditor editor = columns[column].getEditor(node);
                return editor == null ? super.getCellEditor(row, column) : editor;
            }
        };
        treeTable.setRootVisible(true);
        treeTable.setRowHeight(18);

        JFrame frame = new JFrame();
        frame.setBounds(400, 400, 800, 500);
        frame.getContentPane().add(treeTable);
        frame.setVisible(true);

    }


    private static class CustomNode extends DefaultMutableTreeNode {
        String name;
        Object value;

        CustomNode(String name, Object value) {
            this.name = name;
            this.value = value;
        }

        public Object getValue() {
            return value;
        }

        public String getName() {
            return name;
        }
    }

}
