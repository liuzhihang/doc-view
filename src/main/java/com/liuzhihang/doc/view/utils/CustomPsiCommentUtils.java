package com.liuzhihang.doc.view.utils;

import com.intellij.psi.PsiElement;
import com.intellij.psi.javadoc.PsiDocComment;
import com.intellij.psi.javadoc.PsiDocTag;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

/**
 * 从注释中解析注解的工具类
 *
 * @author liuzhihang
 * @date 2020/3/18 17:00
 */
public class CustomPsiCommentUtils {


    /**
     * 获取所有注释
     *
     * @param docComment
     * @return
     */
    public static String getComment(PsiDocComment docComment) {

        if (docComment == null) {
            return "";
        }


        StringBuilder desc = new StringBuilder();

        PsiElement[] descriptionElements = docComment.getDescriptionElements();

        for (PsiElement descriptionElement : descriptionElements) {
            if (StringUtils.isNotBlank(descriptionElement.getText())) {
                desc.append(descriptionElement.getText()).append("\n");
            }
        }


        return desc.toString();

    }


    /**
     * 获取注释, 如果指定 tagName 则直接从 tagName 里面获取
     * <p>
     * 如果从 tagName 里面获取不到, 则直接获取全部注释
     *
     * @param docComment
     * @param tagName
     * @return
     */
    @NotNull
    public static String getComment(PsiDocComment docComment, String tagName) {

        return getComment(docComment, tagName, true);
    }

    /**
     * 获取注释, 如果指定 tagName 则直接从 tagName 里面获取, tag
     *
     * @param docComment
     * @param tagName
     * @param takeAll    tag 不存在时是否获取全部注释
     * @return
     */
    @NotNull
    public static String getComment(PsiDocComment docComment, String tagName, boolean takeAll) {

        if (docComment == null) {
            return "";
        }


        if (StringUtils.isBlank(tagName)) {
            return "";
        }

        PsiDocTag psiDocTag = docComment.findTagByName(tagName);

        // tag 不存在
        if (psiDocTag == null) {

            if (takeAll) {
                return getComment(docComment);
            } else {
                return "";
            }
        }

        StringBuilder desc = new StringBuilder();

        PsiDocTag[] tags = docComment.getTags();
        for (PsiDocTag tag : tags) {

            if (tagName.equalsIgnoreCase(tag.getName())) {

                PsiElement[] dataElements = tag.getDataElements();

                for (PsiElement psiElement : dataElements) {
                    desc.append(psiElement.getText());
                }

                return desc.toString();
            }
        }

        return desc.toString();
    }

}
