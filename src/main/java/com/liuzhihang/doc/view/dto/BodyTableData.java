package com.liuzhihang.doc.view.dto;

import com.intellij.openapi.progress.ProgressManager;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;

/** A display-only projection; parsing, editor and custom-template trees keep their original nodes. */
final class BodyTableData {

    private BodyTableData() {
    }

    static List<DocViewParamData> project(List<DocViewParamData> rows) {
        return project(rows, "", "", true);
    }

    private static List<DocViewParamData> project(List<DocViewParamData> rows,
                                                  String sourceParentPrefix,
                                                  String visibleParentPrefix,
                                                  boolean topLevel) {
        List<DocViewParamData> visible = new ArrayList<>();
        if (rows == null) {
            return visible;
        }
        for (DocViewParamData row : rows) {
            ProgressManager.checkCanceled();
            String sourcePrefix = StringUtils.defaultString(row.getPrefixSymbol2());
            // These flags identify parser-created element/key/value nodes, not business field names.
            if (row.isCollection() || row.isMap()) {
                visible.addAll(project(row.getChildList(), sourcePrefix, visibleParentPrefix, topLevel));
                continue;
            }
            String levelPrefix = sourcePrefix.startsWith(sourceParentPrefix)
                    ? sourcePrefix.substring(sourceParentPrefix.length()) : sourcePrefix;
            String visiblePrefix = topLevel ? "" : visibleParentPrefix + levelPrefix;

            DocViewParamData copy = new DocViewParamData();
            copy.setPsiElement(row.getPsiElement());
            copy.setName(row.getName());
            copy.setType(row.getType());
            copy.setRequired(row.getRequired());
            copy.setExample(row.getExample());
            copy.setDesc(row.getDesc());
            copy.setSince(row.getSince());
            copy.setVersion(row.getVersion());
            copy.setPrefixSymbol1(topLevel ? "" : row.getPrefixSymbol1());
            copy.setPrefixSymbol2(visiblePrefix);
            copy.setChildList(project(row.getChildList(), sourcePrefix, visiblePrefix, false));
            visible.add(copy);
        }
        return visible;
    }
}
