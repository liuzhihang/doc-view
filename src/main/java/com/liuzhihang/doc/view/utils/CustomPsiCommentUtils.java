package com.liuzhihang.doc.view.utils;

import com.google.common.collect.Lists;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.util.Computable;
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
     * 获取注释中 tagName 对应的注释, 如果指定 tagName 则直接从 tagName 里面获取
     *
     * @param docComment 注释 PsiDocComment
     * @param tagName    注释中的 @xxx 标签
     * @return 注释
     */
    @NotNull
    public static String tagDocComment(PsiDocComment docComment, String tagName) {

        String comment = "";

        if (docComment == null) {
            return comment;
        }

        for (PsiElement element : docComment.getChildren()) {

            // 不是 tagName 则继续查找
            if (!("PsiDocTag:@" + tagName).equalsIgnoreCase(element.toString())) {
                continue;
            }

            return element.getText()
                    .replace(("@" + tagName), StringUtils.EMPTY)
                    .trim();

        }

        return "";
    }

    /**
     * 获取方法中字段的注释, 一般会用 @param 标注出来
     *
     * @param docComment 方法的注释 Psi
     * @param parameter  参数
     * @return 字段注释
     */
    @NotNull
    public static String paramDocComment(PsiDocComment docComment, @NotNull PsiParameter parameter) {

        String comment = "";

        if (docComment == null) {
            return comment;
        }

        for (PsiElement element : docComment.getChildren()) {

            // 不是当前字段则继续循环
            if (!("PsiDocTag:@param").equalsIgnoreCase(element.toString())) {
                continue;
            }

            // 在注释中定位到该参数
            if (element.getText().startsWith("@param " + parameter.getName())) {

                String paramWithComment = element.getText();

                if (paramWithComment.contains("\n")) {
                    // 该字段后面还有注释
                    comment = paramWithComment.substring(("@param " + parameter.getName()).length(), element.getText().indexOf("\n"));
                } else {
                    // 该字段后面没有其他注释, 只有 */
                    comment = paramWithComment.substring(("@param " + parameter.getName()).length());
                }
            }
        }
        // 移除前后的空格
        return comment.trim();
    }

    /**
     * 获取注释, 没有 tag 的注释, 一般是卸载注释开头的位置
     *
     * @param docComment 注释 PSI
     * @return 注释
     */
    @NotNull
    public static String tagDocComment(PsiDocComment docComment) {

        return ApplicationManager.getApplication().runReadAction((Computable<String>) () -> {

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
        });

    }

    /**
     * 获取注释, 没有 tag 的注释
     *
     * @param docComment 注释 PSI
     * @param oneLine    只获取一行
     * @return 注释
     */
    @NotNull
    public static String tagDocComment(PsiDocComment docComment, Boolean oneLine) {

        return ApplicationManager.getApplication().runReadAction((Computable<String>) () -> {
            if (!oneLine) {
                return tagDocComment(docComment);
            }
            if (docComment != null) {
                for (PsiElement element : docComment.getChildren()) {

                    if ("PsiDocToken:DOC_COMMENT_DATA".equalsIgnoreCase(element.toString())) {
                        // 只获取第一行注释
                        return element.getText().replaceAll("[* \n]+", StringUtils.EMPTY);
                    }
                }
            }
            return "";
        });

    }

    /**
     * 获取字段的注释
     *
     * 举例 // xxx
     *
     * @param psiComment 字段的注释 PSI
     * @return 注释
     */
    @NotNull
    public static String fieldComment(PsiComment psiComment) {

        if (psiComment != null && StringUtils.isNotBlank(psiComment.getText())) {
            // 原注释中的换行符移除
            return psiComment.getText().replace("/", StringUtils.EMPTY).trim();

        }
        return "";
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
