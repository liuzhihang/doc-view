package com.liuzhihang.doc.view.ui;

import com.liuzhihang.doc.view.dto.Body;
import org.apache.commons.collections.CollectionUtils;

import javax.swing.table.DefaultTableModel;
import java.util.Arrays;
import java.util.List;
import java.util.Vector;

/**
 * @author liuzhihang
 * @date 2021/3/29 20:16
 */
public class ParamTableModel extends DefaultTableModel {

    public static final Vector<String> titleList = new Vector<>(Arrays.asList("参数名", "类型", "必选", "描述"));

    public ParamTableModel(List<Body> bodyList) {

        Vector<Vector<Object>> vector = new Vector<>();

        if (CollectionUtils.isNotEmpty(bodyList)) {
            // "参数名", "类型", "必选", "描述"
            for (Body body : bodyList) {
                Vector<Object> sv = new Vector<>();
                sv.add(body.getName());
                sv.add(body.getType());
                sv.add(body.getRequired());
                sv.add(body.getDesc());
                vector.add(sv);
            }
        }

        setDataVector(vector, titleList);
    }


    @Override
    public boolean isCellEditable(int row, int column) {

        if (column == 0 || column == 1) {
            return false;
        }

        return super.isCellEditable(row, column);
    }


}
