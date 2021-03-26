package com.liuzhihang.doc.view.ui;

import javax.swing.table.DefaultTableModel;
import java.util.Arrays;
import java.util.Vector;

/**
 * @author liuzhihang
 * @date 2021/3/26 16:21
 */
public class ParamTable extends DefaultTableModel {

    private Vector<Vector<Object>> rows;
    private Vector<String> columnNames = (Vector<String>) Arrays.asList("参数名", "类型", "必选", "描述");

    public ParamTable(Vector<Vector<Object>> data) {
        this.rows = rows;
        setDataVector(rows, columnNames);
    }
}
