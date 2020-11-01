package com.liuzhihang.doc.view.utils;

import com.intellij.psi.PsiElement;
import com.intellij.psi.javadoc.PsiDocComment;
import com.intellij.psi.javadoc.PsiDocTag;
import com.intellij.psi.javadoc.PsiDocTagValue;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

/**
 * @author liuzhihang
 * @date 2020/3/18 17:00
 */
public class CustomPsiDocCommentUtils {


    @NotNull
    public static String getComment(PsiDocComment docComment) {

        if (docComment == null) {
            return "";
        }


        StringBuilder desc = new StringBuilder();

        PsiDocTag[] tags = docComment.getTags();
        for (PsiDocTag tag : tags) {

            if ("description".equalsIgnoreCase(tag.getName())) {

                PsiElement[] dataElements = tag.getDataElements();

                for (PsiElement psiElement : dataElements) {
                    if (StringUtils.isNotBlank(psiElement.getText())) {
                        desc.append(psiElement.getText()).append("\n");
                    }
                }

                return desc.toString();
            }
        }

        PsiElement[] descriptionElements = docComment.getDescriptionElements();

        for (PsiElement descriptionElement : descriptionElements) {
            if (StringUtils.isNotBlank(descriptionElement.getText())) {
                desc.append(descriptionElement.getText()).append("\n");
            }
        }


        return desc.toString();
    }


    @NotNull
    public static String getName(PsiDocComment docComment) {

        if (docComment == null) {
            return "";
        }

        PsiDocTag nameTag = docComment.findTagByName("name");

        if (nameTag == null) {
            return "";
        }

        PsiElement[] dataElements = nameTag.getDataElements();

        if (dataElements.length > 0) {

            StringBuilder name = new StringBuilder();

            for (PsiElement dataElement : dataElements) {
                if (StringUtils.isNotBlank(dataElement.getText())) {
                    name.append(dataElement.getText()).append("\n");
                }
            }

            return name.toString();
        }

        return "";
    }

    /**
     * 移除注释文本中的 / * 空格 换行符
     *
     * @param commentText
     * @return
     */
    public static String removeSymbol(String commentText) {
        return commentText.replace("*", "")
                .replace("/", "")
                .replace(" ", "")
                .replace("\n", "")
                .replace("\t", "");
    }

}
