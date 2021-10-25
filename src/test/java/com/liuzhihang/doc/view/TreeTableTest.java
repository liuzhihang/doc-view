package com.liuzhihang.doc.view;

import com.intellij.ui.ColoredTreeCellRenderer;
import com.intellij.ui.dualView.TreeTableView;
import com.intellij.ui.treeStructure.treetable.ListTreeTableModelOnColumns;
import com.intellij.ui.treeStructure.treetable.TreeColumnInfo;
import com.intellij.util.ui.ColumnInfo;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeCellRenderer;

/**
 * @author liuzhihang
 * @date 2020/10/27 16:47
 */
public class TreeTableTest {


    public static void main(String[] args) {


        //初始化为空
        CustomNode root = new CustomNode("Root", "");

        root.add(new CustomNode("a", "a"));
        root.add(new CustomNode("b", "b"));
        root.add(new CustomNode("c", "c"));


        CustomNode d = new CustomNode("d", "d");
        root.add(d);

        d.add(new CustomNode("d1", "d1"));
        d.add(new CustomNode("d3", "d3"));
        d.add(new CustomNode("d4", "d4"));
        d.add(new CustomNode("d5", "d5"));

        ColumnInfo[] columnInfo = new ColumnInfo[]{
                new TreeColumnInfo("Name") {
                    @Override
                    public int getWidth(JTable table) {
                        return 200;
                    }

                },   // <-- This is important!
                new ColumnInfo("Value") {
                    @Nullable
                    @Override
                    public Object valueOf(Object o) {
                        if (o instanceof CustomNode) {
                            return ((CustomNode) o).getValue();
                        } else return o;
                    }
                }
        };

        ListTreeTableModelOnColumns model = new ListTreeTableModelOnColumns(root, columnInfo);
        TreeTableView table = new TreeTableView(model) {
            @Override
            public void setTreeCellRenderer(TreeCellRenderer renderer) {
                super.setTreeCellRenderer(new ColoredTreeCellRenderer() {

                    @Override
                    public void customizeCellRenderer(@NotNull JTree tree, Object value, boolean selected, boolean expanded, boolean leaf, int row, boolean hasFocus) {
                        CustomNode node = (CustomNode) value;
                        append(node.getKey());
                    }
                });
            }

            @Override
            public TableCellRenderer getCellRenderer(int row, int column) {
                return super.getCellRenderer(row, column);
            }


            @Override
            public TableCellEditor getCellEditor(int row, int column) {
                return super.getCellEditor(row, column);
            }

            @Override
            public Object getValueAt(int row, int column) {
                ListTreeTableModelOnColumns myModel = (ListTreeTableModelOnColumns) getTableModel();
                CustomNode node = (CustomNode) myModel.getRowValue(row);
                if (column == 0) {
                    return node.getKey();
                } else {
                    return node.getValue();
                }
            }

            @Override
            public boolean isCellEditable(int row, int column) {
                return column == 1;
            }

        };
        table.setRootVisible(true);
        table.setVisible(true);


        JFrame frame = new JFrame();
        frame.setBounds(400, 400, 800, 500);
        frame.getContentPane().add(table);
        frame.setVisible(true);

    }

    private static class CustomNode extends DefaultMutableTreeNode {
        private String key;
        private Object value;

        public CustomNode() {
        }

        public CustomNode(String key, Object value) {
            this.key = key;
            this.value = value;
        }

        public String getKey() {
            return key;
        }

        public void setKey(String key) {
            this.key = key;
        }

        public Object getValue() {
            return value;
        }

        public void setValue(Object value) {
            this.value = value;
        }
    }




}
