package com.liuzhihang.doc.view.ui.treeview;

import com.intellij.ui.ColoredTreeCellRenderer;
import com.intellij.ui.dualView.TreeTableView;
import com.intellij.ui.treeStructure.treetable.ListTreeTableModelOnColumns;
import com.intellij.ui.treeStructure.treetable.TreeColumnInfo;
import com.intellij.util.ui.ColumnInfo;
import com.liuzhihang.doc.view.dto.DocViewParamData;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeCellRenderer;

/**
 * @author liuzhihang
 * @date 2021/10/20 19:51
 */
public class ParamTreeTableView extends TreeTableView {

    // {"参数名", "类型", "必选", "描述"}
    public static final ColumnInfo[] COLUMN_INFOS = new ColumnInfo[]{
            new TreeColumnInfo("参数名") {
            },
            new ColumnInfo("类型") {
                @Nullable
                @Override
                public Object valueOf(Object o) {
                    if (o instanceof DefaultMutableTreeNode) {
                        DefaultMutableTreeNode node = (DefaultMutableTreeNode) o;
                        if (node.getUserObject() instanceof DocViewParamData) {
                            DocViewParamData paramData = (DocViewParamData) node.getUserObject();
                            return paramData.getType();
                        }

                    }
                    return o;
                }
            },
            new ColumnInfo("必选") {
                @Nullable
                @Override
                public Object valueOf(Object o) {
                    if (o instanceof DefaultMutableTreeNode) {
                        DefaultMutableTreeNode node = (DefaultMutableTreeNode) o;
                        if (node.getUserObject() instanceof DocViewParamData) {
                            DocViewParamData paramData = (DocViewParamData) node.getUserObject();
                            return paramData.getRequired();
                        }

                    }
                    return o;
                }
            },
            new ColumnInfo("描述") {
                @Nullable
                @Override
                public Object valueOf(Object o) {
                    if (o instanceof DefaultMutableTreeNode) {
                        DefaultMutableTreeNode node = (DefaultMutableTreeNode) o;
                        if (node.getUserObject() instanceof DocViewParamData) {
                            DocViewParamData paramData = (DocViewParamData) node.getUserObject();
                            return paramData.getDesc();
                        }

                    }
                    return o;
                }
            }
    };

    public ParamTreeTableView(ListTreeTableModelOnColumns treeTableModel) {
        super(treeTableModel);
    }


    @Override
    public void setTreeCellRenderer(TreeCellRenderer renderer) {
        super.setTreeCellRenderer(new ColoredTreeCellRenderer() {

            @Override
            public void customizeCellRenderer(@NotNull JTree tree, Object value, boolean selected, boolean expanded, boolean leaf, int row, boolean hasFocus) {
                DefaultMutableTreeNode node = (DefaultMutableTreeNode) value;
                DocViewParamData paramData = (DocViewParamData) node.getUserObject();
                append(paramData.getName());
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
        ListTreeTableModelOnColumns tableModel = (ListTreeTableModelOnColumns) getTableModel();
        DefaultMutableTreeNode node = (DefaultMutableTreeNode) tableModel.getRowValue(row);
        DocViewParamData paramData = (DocViewParamData) node.getUserObject();
        switch (column) {
            case 0:
                return paramData.getName();
            case 1:
                return paramData.getType();
            case 2:
                return paramData.getRequired();
            case 3:
                return paramData.getDesc();
        }
        return "";
    }

    @Override
    public boolean isCellEditable(int row, int column) {
        return false;
    }

}
