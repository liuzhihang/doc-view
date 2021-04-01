package com.liuzhihang.doc.view.dto;

import com.intellij.psi.PsiMethod;

import java.util.List;

/**
 * 文档参数
 *
 * @author liuzhihang
 * @date 2020/2/28 10:32
 */
public class DocView {

    /**
     * 当前接口的方法
     */
    private PsiMethod psiMethod;

    private String fullClassName;

    private String className;

    /**
     * 文档名称
     */
    private String name;

    /**
     * 文档描述
     */
    private String desc;

    /**
     * 环境地址
     */
    private List<String> domain;

    /**
     * 接口地址
     */
    private String path;

    /**
     * 请求方式 GET POST PUT DELETE HEAD OPTIONS PATCH
     */
    private String method;

    /**
     * 变动说明
     */
    private String changeLog;


    /**
     * headers
     */
    private List<Header> headerList;


    /**
     * 请求参数
     */
    private List<Body> reqBodyList;

    /**
     * 返回参数
     */
    private List<Body> respBodyList;


    /**
     * 请求参数
     */
    private List<Param> reqParamList;

    /**
     * 参数时Json时举例
     */
    private String reqExample;

    /**
     * reqExampleType 类型
     */
    private String reqExampleType;

    /**
     * 返回参数
     */
    private String respExample;

    /**
     * 备注
     */
    private String remark;

    private String type;

    public DocView(String name) {
        this.name = name;
    }

    public DocView() {
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDesc() {
        return desc;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }

    public List<String> getDomain() {
        return domain;
    }

    public void setDomain(List<String> domain) {
        this.domain = domain;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public String getChangeLog() {
        return changeLog;
    }

    public void setChangeLog(String changeLog) {
        this.changeLog = changeLog;
    }

    public List<Header> getHeaderList() {
        return headerList;
    }

    public void setHeaderList(List<Header> headerList) {
        this.headerList = headerList;
    }

    public String getReqExampleType() {
        return reqExampleType;
    }

    public void setReqExampleType(String reqExampleType) {
        this.reqExampleType = reqExampleType;
    }

    public List<Body> getReqBodyList() {
        return reqBodyList;
    }

    public void setReqBodyList(List<Body> reqBodyList) {
        this.reqBodyList = reqBodyList;
    }

    public List<Body> getRespBodyList() {
        return respBodyList;
    }

    public void setRespBodyList(List<Body> respBodyList) {
        this.respBodyList = respBodyList;
    }

    public String getReqExample() {
        return reqExample;
    }

    public void setReqExample(String reqExample) {
        this.reqExample = reqExample;
    }

    public String getRespExample() {
        return respExample;
    }

    public void setRespExample(String respExample) {
        this.respExample = respExample;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public List<Param> getReqParamList() {
        return reqParamList;
    }

    public void setReqParamList(List<Param> reqParamList) {
        this.reqParamList = reqParamList;
    }

    public String getFullClassName() {
        return fullClassName;
    }

    public void setFullClassName(String fullClassName) {
        this.fullClassName = fullClassName;
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public PsiMethod getPsiMethod() {
        return psiMethod;
    }

    public void setPsiMethod(PsiMethod psiMethod) {
        this.psiMethod = psiMethod;
    }
}
