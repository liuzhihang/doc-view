package com.liuzhihang.doc.view.ui;

import com.intellij.psi.PsiElement;
import com.liuzhihang.doc.view.dto.ParamData;
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

    public static final Vector<String> titleList = new Vector<>(Arrays.asList("参数名", "类型", "必选", "描述"));
    private static final Logger log = LoggerFactory.getLogger(ParamTableModel.class);

    private List<ParamData> paramList;
    private Map<PsiElement, ParamData> modifyBodyMap = new HashMap<>();

    /**
     * 构造参数
     * <p>
     * Body 里面可能会嵌套
     *
     * @param bodyList
     */
    public ParamTableModel(List<ParamData> bodyList) {

        this.paramList = bodyList;

        Vector<Vector<Object>> vector = new Vector<>();

        if (CollectionUtils.isNotEmpty(bodyList)) {
            // "参数名", "类型", "必选", "描述"
            for (ParamData data : bodyList) {
                Vector<Object> sv = new Vector<>();
                sv.add(data.getPrefix() + data.getName());
                sv.add(data.getType());
                sv.add(data.getRequired());
                sv.add(data.getDesc());
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

        ParamData data = paramList.get(row);

        // 值发生了改变, 是否必选发生改变
        if (column == 2) {
            Boolean required = (Boolean) value;
            if (!data.getRequired().equals(required)) {
                data.setRequired(required);
                modifyBodyMap.put(data.getPsiElement(), data);
            }

        }
        // 注释发生改变
        if (column == 3) {
            String desc = (String) value;
            if (!data.getDesc().equals(desc)) {
                data.setDesc(desc);
                modifyBodyMap.put(data.getPsiElement(), data);
            }
        }

        // 有可能改了之后, 又改回去, 先不进行处理, 直接覆盖.
        super.setValueAt(value, row, column);
    }

    public Map<PsiElement, ParamData> getModifyBodyMap() {
        return modifyBodyMap;
    }
}
