package com.liuzhihang.doc.view.ui;

import com.intellij.ui.Gray;
import com.intellij.ui.JBColor;
import com.intellij.util.ui.UIUtil;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableColumn;
import java.awt.*;

/**
 * @author liuzhihang
 * @date 2021/3/30 10:16
 */
public class ParamTableUI {


    private static final DefaultTableCellRenderer RENDERER = new DefaultTableCellRenderer() {
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
     * 行设置
     * <p>
     * 1. 设置每行的颜色
     *
     * @param table
     */
    public static void rowSetting(JTable table) {
        for (int i = 0; i < table.getColumnCount(); i++) {
            table.getColumn(table.getColumnName(i)).setCellRenderer(RENDERER);
        }
    }

    /**
     * 列设置
     * <p>
     * 1. 设置单元格格式
     *
     * @param table
     */
    public static void columnSetting(JTable table) {

        for (int i = 0; i < table.getColumnCount(); i++) {

            // 根据 列名 获取 表格列
            TableColumn tableColumn = table.getColumn("必选");
            // 设置 表格列 的 单元格编辑器
            tableColumn.setCellEditor(new DefaultCellEditor(new JComboBox<>(new Boolean[]{true, false})));

        }

    }
}
