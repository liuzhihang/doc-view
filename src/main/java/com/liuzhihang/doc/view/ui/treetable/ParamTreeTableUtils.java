package com.liuzhihang.doc.view.ui.treetable;

import com.intellij.ui.Gray;
import com.intellij.ui.JBColor;
import com.intellij.util.ui.UIUtil;
import com.liuzhihang.doc.view.dto.DocViewParamData;
import org.jdesktop.swingx.JXTreeTable;
import org.jdesktop.swingx.decorator.ColorHighlighter;
import org.jdesktop.swingx.treetable.DefaultMutableTreeTableNode;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableColumn;
import java.awt.*;
import java.util.List;

import static javax.swing.ListSelectionModel.SINGLE_SELECTION;

/**
 * @author liuzhihang
 * @date 2021/3/30 10:16
 */
public class ParamTreeTableUtils {

    public static final DefaultTableCellRenderer RENDERER = new DefaultTableCellRenderer() {

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            setOpaque(true);
            /**
             * 隔行变色
             */
            if (UIUtil.isUnderDarcula()) {
                if (row % 2 == 0) {
                    setBackground(JBColor.WHITE);
                } else {
                    setBackground(Gray._45);
                }
            } else {
                if (row % 2 == 0) {
                    setBackground(JBColor.WHITE);
                } else {
                    setBackground(Gray._245);
                }
            }

            return super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
        }
    };

    /**
     * 对 treeTable 进行渲染
     *
     * @param treeTable
     */
    public static void render(JXTreeTable treeTable) {

        final DefaultListSelectionModel defaultListSelectionModel = new DefaultListSelectionModel();
        treeTable.setSelectionModel(defaultListSelectionModel);

        defaultListSelectionModel.setSelectionMode(SINGLE_SELECTION);
        defaultListSelectionModel.addListSelectionListener(e -> defaultListSelectionModel.clearSelection());


        for (int i = 0; i < treeTable.getColumnCount(); i++) {

            TableColumn column = treeTable.getColumn(i);

            if (column.getIdentifier().equals("必选")) {
                // 设置 表格列 的 单元格编辑器
                column.setCellEditor(new DefaultCellEditor(new JComboBox<>(new Boolean[]{true, false})));
            }

        }

        treeTable.setRowHeight(30);
        treeTable.setLeafIcon(null);
        treeTable.setOpenIcon(null);
        treeTable.setClosedIcon(null);

        if (UIUtil.isUnderDarcula()) {
            treeTable.addHighlighter(new ColorHighlighter((renderer, adapter) -> adapter.row % 2 == 1, Gray._45, null));
        } else {
            treeTable.addHighlighter(new ColorHighlighter((renderer, adapter) -> adapter.row % 2 == 1, Gray._245, null));
        }

        treeTable.setSelectionForeground(JBColor.WHITE);
    }


    public static void createTreeData(DefaultMutableTreeTableNode rootNode, @NotNull List<DocViewParamData> dataList) {

        for (DocViewParamData data : dataList) {
            DefaultMutableTreeTableNode node = new DefaultMutableTreeTableNode(data);
            rootNode.add(node);

            if (data.getChildList() != null && data.getChildList().size() > 0) {
                createTreeData(node, data.getChildList());
            }
        }

    }

}
