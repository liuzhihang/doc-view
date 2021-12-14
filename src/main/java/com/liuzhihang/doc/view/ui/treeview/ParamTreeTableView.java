package com.liuzhihang.doc.view.ui.treeview;

import com.intellij.ide.util.treeView.NodeRenderer;
import com.intellij.openapi.util.NlsContexts;
import com.intellij.psi.PsiElement;
import com.intellij.ui.dualView.TreeTableView;
import com.intellij.ui.treeStructure.treetable.ListTreeTableModelOnColumns;
import com.intellij.ui.treeStructure.treetable.TreeColumnInfo;
import com.intellij.util.ui.ColumnInfo;
import com.liuzhihang.doc.view.dto.DocViewParamData;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.table.TableCellEditor;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeCellRenderer;
import java.util.HashMap;
import java.util.Map;

/**
 * @author liuzhihang
 * @date 2021/10/20 19:51
 */
public class ParamTreeTableView extends TreeTableView {

    // {"参数名", "类型", "必选", "描述"}
    public static final ColumnInfo[] COLUMN_INFOS = new ColumnInfo[]{
            new TreeColumnInfo("参数名 *") {

                @NlsContexts.Tooltip
                @Nullable
                @Override
                public String getTooltipText() {
                    return "不可修改";
                }
            },
            new ColumnInfo("类型 *") {
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

                @NlsContexts.Tooltip
                @Nullable
                @Override
                public String getTooltipText() {
                    return "不可修改";
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

                @Override
                public int getWidth(JTable table) {
                    return 80;
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

    private static final NodeRenderer NODE_RENDERER = new NodeRenderer() {

        @Override
        public void customizeCellRenderer(@NotNull JTree tree, Object value, boolean selected, boolean expanded, boolean leaf, int row, boolean hasFocus) {
            if (value instanceof DefaultMutableTreeNode) {
                DefaultMutableTreeNode node = (DefaultMutableTreeNode) value;
                if (node.getUserObject() instanceof DocViewParamData) {
                    DocViewParamData paramData = (DocViewParamData) node.getUserObject();
                    append(paramData.getName() == null ? "" : paramData.getName());
                }
            }
        }

    };

    /**
     * 修改集合
     */
    private Map<PsiElement, DocViewParamData> modifiedMap = new HashMap<>();

    public ParamTreeTableView(ListTreeTableModelOnColumns treeTableModel) {
        super(treeTableModel);
    }

    @Override
    public void setTreeCellRenderer(TreeCellRenderer renderer) {
        super.setTreeCellRenderer(NODE_RENDERER);
    }

    @Override
    public TableCellEditor getCellEditor(int row, int column) {

        if (column == 2) {
            return new DefaultCellEditor(new JComboBox<>(new Boolean[]{true, false}));
        }
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
    public void setValueAt(Object value, int row, int column) {

        ListTreeTableModelOnColumns tableModel = (ListTreeTableModelOnColumns) getTableModel();
        DefaultMutableTreeNode node = (DefaultMutableTreeNode) tableModel.getRowValue(row);
        DocViewParamData paramData = (DocViewParamData) node.getUserObject();

        // "参数名", "类型", "必选", "描述"
        if (column == 2) {
            paramData.setRequired(String.valueOf(value).equalsIgnoreCase("true"));
            modifiedMap.put(paramData.getPsiElement(), paramData);
        } else if (column == 3) {
            paramData.setDesc(String.valueOf(value));
            modifiedMap.put(paramData.getPsiElement(), paramData);
        }

        super.setValueAt(value, row, column);

    }

    @Override
    public boolean isCellEditable(int row, int column) {

        if (column == 2) {
            return true;
        }
        if (column == 3) {
            return true;
        }

        return false;
    }

    @Override
    public int getRowHeight() {
        return 24;
    }


    public Map<PsiElement, DocViewParamData> getModifiedMap() {

        return modifiedMap;
    }
}
