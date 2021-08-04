package com.liuzhihang.doc.view.ui.treetable;

import com.intellij.ui.Gray;
import com.intellij.ui.JBColor;
import com.intellij.util.ui.UIUtil;
import org.jdesktop.swingx.JXTreeTable;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableColumn;
import java.awt.*;

import static javax.swing.ListSelectionModel.SINGLE_SELECTION;

/**
 * @author liuzhihang
 * @date 2021/3/30 10:16
 */
public class ParamTreeTableUI {

    public static final DefaultTableCellRenderer RENDERER = new DefaultTableCellRenderer() {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {

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

        treeTable.getColumnModel().getColumn(0).setPreferredWidth(150);
        treeTable.setRowHeight(30);
        treeTable.setLeafIcon(null);
        treeTable.setOpenIcon(null);
        treeTable.setClosedIcon(null);

        final DefaultListSelectionModel defaultListSelectionModel = new DefaultListSelectionModel();
        treeTable.setSelectionModel(defaultListSelectionModel);

        defaultListSelectionModel.setSelectionMode(SINGLE_SELECTION);
        defaultListSelectionModel.addListSelectionListener(e -> defaultListSelectionModel.clearSelection());


        for (int i = 0; i < treeTable.getColumnCount(); i++) {
            treeTable.getColumn(treeTable.getColumnName(i)).setCellRenderer(RENDERER);
        }


        for (int i = 0; i < treeTable.getColumnCount(); i++) {

            // 根据 列名 获取 表格列
            TableColumn tableColumn = treeTable.getColumn("必选");
            // 设置 表格列 的 单元格编辑器
            tableColumn.setCellEditor(new DefaultCellEditor(new JComboBox<>(new Boolean[]{true, false})));

        }

    }

}
