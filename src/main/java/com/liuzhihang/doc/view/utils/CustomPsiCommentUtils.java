package com.liuzhihang.doc.view.utils;

import com.google.common.collect.Lists;
import com.intellij.psi.PsiComment;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiParameter;
import com.intellij.psi.javadoc.PsiDocComment;
import com.intellij.psi.javadoc.PsiDocTag;
import com.intellij.psi.javadoc.PsiDocTagValue;
import com.liuzhihang.doc.view.constant.FieldTypeConstant;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Iterator;
import java.util.List;

/**
 * 从注释中解析注解的工具类
 *
 * @author liuzhihang
 * @date 2020/3/18 17:00
 */
public class CustomPsiCommentUtils {


    /**
     * 获取注释, 如果指定 tagName 则直接从 tagName 里面获取
     *
     * @param docComment
     * @param tagName
     * @return
     */
    @NotNull
    public static String getDocComment(PsiDocComment docComment, String tagName) {

        if (docComment != null) {
            for (PsiElement element : docComment.getChildren()) {

                if (!("PsiDocTag:@" + tagName).equalsIgnoreCase(element.toString())) {
                    continue;
                }

                return element.getText()
                        .replace(("@" + tagName), StringUtils.EMPTY)
                        .trim();

            }
        }

        return "";
    }

    /**
     * 获取方法字段的注释
     *
     * @param docComment
     * @param parameter
     * @return
     */
    @NotNull
    public static String getMethodParam(PsiDocComment docComment, @NotNull PsiParameter parameter) {

        if (docComment != null) {
            for (PsiElement element : docComment.getChildren()) {

                if (!("PsiDocTag:@param").equalsIgnoreCase(element.toString())) {
                    continue;
                }

                if (element.getText().startsWith("@param " + parameter.getName())) {
                    return element.getText().substring(("@param " + parameter.getName()).length(), element.getText().indexOf("\n"));
                }

            }
        }

        return "";
    }


    /**
     * 获取注释, 没有 tag 的注释
     *
     * @param docComment
     * @return
     */
    @NotNull
    public static String getDocComment(PsiDocComment docComment) {

        StringBuilder sb = new StringBuilder();

        if (docComment != null) {
            for (PsiElement element : docComment.getChildren()) {

                if (!"PsiDocToken:DOC_COMMENT_DATA".equalsIgnoreCase(element.toString())) {
                    continue;
                }
                // 原注释中的换行符移除
                sb.append(element.getText().replaceAll("[* \n]+", StringUtils.EMPTY));

            }
        }
        return sb.toString();
    }

    @NotNull
    public static String getComment(PsiComment psiComment) {


        if (psiComment != null && StringUtils.isNotBlank(psiComment.getText())) {
            // 原注释中的换行符移除
            return psiComment.getText().replace("/", StringUtils.EMPTY).trim();

        }
        return "";
    }


    /**
     * 保留换行
     *
     * @param docComment
     * @return
     */
    @NotNull
    public static String getMethodComment(PsiDocComment docComment) {
        StringBuilder sb = new StringBuilder();

        if (docComment != null) {
            for (PsiElement element : docComment.getChildren()) {

                if (!"PsiDocToken:DOC_COMMENT_DATA".equalsIgnoreCase(element.toString())) {
                    continue;
                }
                if (sb.length() > 0) {
                    // 保留换行符
                    sb.append(element.getText().replaceAll("[/* ]+", StringUtils.EMPTY)).append("\n");
                } else {
                    sb.append(element.getText().replaceAll("[/* ]+", StringUtils.EMPTY));
                }

            }
        }
        return sb.toString();
    }

    /**
     * 构建参数
     *
     * @param elements      元素
     * @param paramNameList 参数名称数组
     * @return {@link java.util.List<java.lang.String>}
     */
    @NotNull
    public static List<String> buildParams(@NotNull List<PsiElement> elements, List<String> paramNameList) {

        List<String> paramDocList = Lists.newArrayList();

        for (Iterator<PsiElement> iterator = elements.iterator(); iterator.hasNext(); ) {
            PsiElement element = iterator.next();
            if (!"PsiDocTag:@param".equalsIgnoreCase(element.toString())) {
                continue;
            }
            String paramName = null;
            String paramData = null;
            for (PsiElement child : element.getChildren()) {
                if (StringUtils.isBlank(paramName) && "PsiElement(DOC_PARAMETER_REF)".equals(child.toString())) {
                    paramName = StringUtils.trim(child.getText());
                } else if (StringUtils.isBlank(paramData) && "PsiDocToken:DOC_COMMENT_DATA".equals(child.toString())) {
                    paramData = StringUtils.trim(child.getText());
                }
            }
            if (StringUtils.isBlank(paramName) || StringUtils.isBlank(paramData)) {
                iterator.remove();
                continue;
            }
            if (!paramNameList.contains(paramName)) {
                iterator.remove();
                continue;
            }
            paramNameList.remove(paramName);
        }
        for (String paramName : paramNameList) {
            paramDocList.add("@param " + paramName);
        }
        return paramDocList;
    }


    /**
     * 构建返回
     *
     * @param elements   元素
     * @param returnName 返回名称
     * @return {@link java.lang.String}
     */
    @Nullable
    public static String buildReturn(@NotNull List<PsiElement> elements, String returnName) {
        boolean isInsert = true;
        for (Iterator<PsiElement> iterator = elements.iterator(); iterator.hasNext(); ) {
            PsiElement element = iterator.next();
            if (!"PsiDocTag:@return".equalsIgnoreCase(element.toString())) {
                continue;
            }
            PsiDocTagValue value = ((PsiDocTag) element).getValueElement();
            if (value == null || StringUtils.isBlank(value.getText())) {
                iterator.remove();
            } else if (returnName.length() <= 0 || "void".equals(returnName)) {
                iterator.remove();
            } else {
                isInsert = false;
            }
        }
        if (isInsert && returnName.length() > 0 && !"void".equals(returnName)) {
            if (FieldTypeConstant.BASE_TYPE_SET.contains(returnName)) {
                return "@return " + returnName;
            } else {
                return "@return {@link " + returnName + "}";
            }
        }
        return null;
    }

    /**
     * 构建异常
     *
     * @param elements          元素
     * @param exceptionNameList 异常名称数组
     * @return {@link java.util.List<java.lang.String>}
     */
    @NotNull
    public static List<String> buildException(@NotNull List<PsiElement> elements, List<String> exceptionNameList) {
        List<String> paramDocList = Lists.newArrayList();
        for (Iterator<PsiElement> iterator = elements.iterator(); iterator.hasNext(); ) {
            PsiElement element = iterator.next();
            if (!"PsiDocTag:@throws".equalsIgnoreCase(element.toString())
                    && !"PsiDocTag:@exception".equalsIgnoreCase(element.toString())) {
                continue;
            }
            String exceptionName = null;
            String exceptionData = null;
            for (PsiElement child : element.getChildren()) {
                if (StringUtils.isBlank(exceptionName) && "PsiElement(DOC_TAG_VALUE_ELEMENT)".equals(child.toString())) {
                    exceptionName = StringUtils.trim(child.getText());
                } else if (StringUtils.isBlank(exceptionData) && "PsiDocToken:DOC_COMMENT_DATA".equals(child.toString())) {
                    exceptionData = StringUtils.trim(child.getText());
                }
            }
            if (StringUtils.isBlank(exceptionName) || StringUtils.isBlank(exceptionData)) {
                iterator.remove();
                continue;
            }
            if (!exceptionNameList.contains(exceptionName)) {
                iterator.remove();
                continue;
            }
            exceptionNameList.remove(exceptionName);
        }
        for (String exceptionName : exceptionNameList) {
            paramDocList.add("@throws " + exceptionName);
        }
        return paramDocList;
    }

}
