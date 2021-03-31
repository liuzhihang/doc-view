package com.liuzhihang.doc.view.ui;

import com.intellij.psi.PsiField;
import com.liuzhihang.doc.view.dto.Body;
import org.apache.commons.collections.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.table.DefaultTableModel;
import java.util.*;

/**
 * @author liuzhihang
 * @date 2021/3/29 20:16
 */
public class ParamTableModel extends DefaultTableModel {

    private static final Logger log = LoggerFactory.getLogger(ParamTableModel.class);

    public static final Vector<String> titleList = new Vector<>(Arrays.asList("参数名", "类型", "必选", "描述"));

    private List<Body> bodyList;
    private Map<PsiField, Body> modifyBodyMap = new HashMap<>();


    public ParamTableModel(List<Body> bodyList) {

        this.bodyList = bodyList;

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

    /**
     * 单元格的值发生变更
     *
     * @param value  值
     * @param row    行
     * @param column 列
     */
    @Override
    public void setValueAt(Object value, int row, int column) {

        // 值没有发生改变
        if (getValueAt(row, column).equals(value)) {
            super.setValueAt(value, row, column);
        }

        Body body = bodyList.get(row);

        // 值发生了改变, 是否必选发生改变
        if (column == 2) {
            Boolean required = (Boolean) value;
            if (!body.getRequired().equals(required)) {
                body.setRequired(required);
                modifyBodyMap.put(body.getParamPsiField(), body);
            }

        }
        // 注释发生改变
        if (column == 3) {
            String desc = (String) value;
            if (!body.getDesc().equals(desc)) {
                body.setDesc(desc);
                modifyBodyMap.put(body.getParamPsiField(), body);
            }
        }

        // 有可能改了之后, 又改回去, 先不进行处理, 直接覆盖.
        super.setValueAt(value, row, column);
    }

    public Map<PsiField, Body> getModifyBodyMap() {
        return modifyBodyMap;
    }
}
