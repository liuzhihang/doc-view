package com.liuzhihang.doc.view.service.impl;

import com.google.gson.Gson;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.options.ShowSettingsUtil;
import com.intellij.openapi.project.Project;
import com.intellij.psi.CommonClassNames;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiPrimitiveType;
import com.intellij.psi.PsiType;
import com.intellij.psi.util.InheritanceUtil;
import com.liuzhihang.doc.view.DocViewBundle;
import com.liuzhihang.doc.view.config.YApiSettings;
import com.liuzhihang.doc.view.config.YApiSettingsConfigurable;
import com.liuzhihang.doc.view.constant.FieldTypeConstant;
import com.liuzhihang.doc.view.dto.Body;
import com.liuzhihang.doc.view.dto.DocView;
import com.liuzhihang.doc.view.dto.Header;
import com.liuzhihang.doc.view.dto.Param;
import com.liuzhihang.doc.view.enums.ContentTypeEnum;
import com.liuzhihang.doc.view.integration.YApiFacadeService;
import com.liuzhihang.doc.view.integration.dto.YApiCat;
import com.liuzhihang.doc.view.integration.dto.YApiHeader;
import com.liuzhihang.doc.view.integration.dto.YApiQuery;
import com.liuzhihang.doc.view.integration.dto.YapiSave;
import com.liuzhihang.doc.view.integration.impl.YApiFacadeServiceImpl;
import com.liuzhihang.doc.view.notification.DocViewNotification;
import com.liuzhihang.doc.view.service.DocViewUploadService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 上传到 yapi
 * <p>
 * 将 docViewMap 转换为 YApi 对象并调用 YApiFacadeService
 *
 * @author liuzhihang
 * @date 2021/6/8 23:57
 */
@Slf4j
@Service
public class YApiServiceImpl implements DocViewUploadService {


    @Override
    public boolean checkSettings(@NotNull Project project) {

        YApiSettings apiSettings = YApiSettings.getInstance(project);

        if (StringUtils.isBlank(apiSettings.getUrl())
                || apiSettings.getProjectId() == null
                || StringUtils.isBlank(apiSettings.getToken())) {
            // 说明没有配置 YApi 上传地址, 跳转到配置页面
            DocViewNotification.notifyError(project, DocViewBundle.message("notify.yapi.info.settings"));
            ShowSettingsUtil.getInstance().showSettingsDialog(project, YApiSettingsConfigurable.class);
            return false;
        }
        return true;
    }

    @Override
    public void doUpload(@NotNull Project project, @NotNull DocView docView) {

        try {
            YApiSettings settings = YApiSettings.getInstance(project);

            YApiFacadeService facadeService = ApplicationManager.getApplication().getService(YApiFacadeServiceImpl.class);

            YApiCat cat = getOrAddCat(settings, docView.getDocTitle());

            YapiSave save = new YapiSave();
            save.setYapiUrl(settings.getUrl());
            save.setToken(settings.getToken());
            save.setProjectId(settings.getProjectId());
            save.setCatId(cat.getId());

            if ("Dubbo".equals(docView.getMethod())) {
                // dubbo 接口处理
                save.setPath("/Dubbo/" + docView.getPsiMethod().getName());
                save.setMethod("POST");
            } else {
                save.setMethod(docView.getMethod());
                save.setPath(docView.getPath());
            }
            // 枚举: raw,form,json
            save.setReqBodyType(docView.getContentType().toString().toLowerCase());
            save.setReqBodyForm(new ArrayList<>());
            save.setReqParams(new ArrayList<>());
            save.setReqHeaders(buildReqHeaders(docView.getHeaderList()));
            save.setReqQuery(buildReqQuery(docView.getReqParamList()));
            save.setResBodyType("json");
            save.setResBody(buildJsonSchema(docView.getRespBody().getChildList()));
            save.setMarkdown(buildDesc(docView));
            save.setTitle(docView.getName());

            if (docView.getContentType().equals(ContentTypeEnum.JSON)) {
                save.setReqBodyIsJsonSchema(true);
                save.setReqBodyOther(buildJsonSchema(docView.getReqBody().getChildList()));
            }

            facadeService.save(save);

            String yapiInterfaceUrl = settings.getUrl() + "/project/" + settings.getProjectId() + "/interface/api/cat_" + cat.getId();

            DocViewNotification.uploadSuccess(project, "YApi", yapiInterfaceUrl);
        } catch (Exception e) {
            DocViewNotification.notifyError(project, DocViewBundle.message("notify.yapi.upload.error", e.getMessage()));
            log.error("上传单个文档失败:{}", docView, e);
        }

    }

    /**
     * 构造描述信息
     */
    @NotNull
    private String buildDesc(DocView docView) {


        return "**接口名称:**\n\n"
                + docView.getName() + "\n\n"
                + "**接口描述:**\n\n"
                + docView.getDesc() + "\n\n"
                + "**请求示例:**\n\n"
                + "```" + docView.getContentType() + "\n" +
                (docView.getReqBodyExample() == null ? "" : docView.getReqBodyExample()) + "\n" +
                "```" + "\n\n"
                + "**返回示例:**\n\n"
                + "```json\n" +
                (docView.getRespExample() == null ? "" : docView.getRespExample()) + "\n" +
                "```\n\n";
    }

    /**
     * JsonSchema 信息如下
     * <p>
     * type: 数据类型 object array
     * required: 必填字段列表
     * title:标题
     * description:描述
     * properties: 字段列表
     * <p>
     * items: 数组类型时内部元素
     */
    private String buildJsonSchema(List<Body> bodyList) {

        List<String> requiredList = new LinkedList<>();

        Map<String, Object> properties = new LinkedHashMap<>();

        buildProperties(requiredList, properties, bodyList);

        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "object");
        schema.put("required", requiredList);
        schema.put("title", " ");
        schema.put("description", " ");
        schema.put("properties", properties);

        return new Gson().toJson(schema);
    }

    /**
     * {
     * "type": "String",
     * "mock": {
     * "mock": "@string"
     * }
     * }
     */
    private void buildProperties(List<String> requiredList, Map<String, Object> properties, List<Body> bodyList) {

        for (Body body : bodyList) {

            Map<String, Object> innerProperties = new LinkedHashMap<>();
            // mock 数据先不填充

            // 设置 body
            if (CollectionUtils.isNotEmpty(body.getChildList()) && body.getPsiElement() instanceof PsiField) {

                PsiField field = (PsiField) body.getPsiElement();
                PsiType type = field.getType();

                if (type instanceof PsiPrimitiveType || FieldTypeConstant.FIELD_TYPE.containsKey(type.getPresentableText())) {
                    // 基础类型
                    innerProperties.put("type", body.getType());
                    innerProperties.put("description", body.getDesc());
                } else if (InheritanceUtil.isInheritor(type, CommonClassNames.JAVA_UTIL_COLLECTION)) {
                    // 集合 还要被 items 包裹
                    List<String> itermRequiredList = new LinkedList<>();
                    Map<String, Object> iterm = new LinkedHashMap<>();
                    Map<String, Object> itermProperties = new LinkedHashMap<>();
                    buildProperties(itermRequiredList, itermProperties, body.getChildList());
                    iterm.put("type", "object");
                    iterm.put("required", itermRequiredList);
                    iterm.put("description", body.getType());
                    iterm.put("properties", itermProperties);

                    innerProperties.put("type", "array");
                    innerProperties.put("description", body.getType());
                    innerProperties.put("items", iterm);

                } else {
                    // InheritanceUtil.isInheritor(type, CommonClassNames.JAVA_UTIL_MAP)
                    // 对象 和 Map
                    List<String> objectRequiredList = new LinkedList<>();
                    Map<String, Object> objectProperties = new LinkedHashMap<>();

                    buildProperties(objectRequiredList, objectProperties, body.getChildList());
                    innerProperties.put("type", "object");
                    innerProperties.put("required", objectRequiredList);
                    innerProperties.put("description", body.getType());
                    innerProperties.put("properties", objectProperties);
                }

            } else if (body.getPsiElement() instanceof PsiClass) {

                if (InheritanceUtil.isInheritor((PsiClass) body.getPsiElement(), CommonClassNames.JAVA_UTIL_COLLECTION)) {
                    // 参数是 List<User>
                    List<String> itermRequiredList = new LinkedList<>();
                    Map<String, Object> iterm = new LinkedHashMap<>();
                    Map<String, Object> itermProperties = new LinkedHashMap<>();
                    buildProperties(itermRequiredList, itermProperties, body.getChildList());
                    iterm.put("type", "object");
                    iterm.put("required", itermRequiredList);
                    iterm.put("description", body.getType());
                    iterm.put("properties", itermProperties);

                    innerProperties.put("type", "array");
                    innerProperties.put("description", body.getType());
                    innerProperties.put("items", iterm);
                } else {
                    // InheritanceUtil.isInheritor(type, CommonClassNames.JAVA_UTIL_MAP)
                    List<String> objectRequiredList = new LinkedList<>();
                    Map<String, Object> objectProperties = new LinkedHashMap<>();

                    buildProperties(objectRequiredList, objectProperties, body.getChildList());
                    innerProperties.put("type", "object");
                    innerProperties.put("required", objectRequiredList);
                    innerProperties.put("description", body.getType());
                    innerProperties.put("properties", objectProperties);

                }

            } else {
                // 基础类型
                innerProperties.put("type", body.getType());
                innerProperties.put("description", body.getDesc());
            }
            // 是否必填
            if (body.getRequired()) {
                requiredList.add(body.getName());
            }
            properties.put(body.getName(), innerProperties);

        }

    }


    private List<YApiQuery> buildReqQuery(List<Param> paramList) {

        if (CollectionUtils.isEmpty(paramList)) {
            return new ArrayList<>();
        }

        return paramList.stream().map(param -> {
            YApiQuery apiQuery = new YApiQuery();
            apiQuery.setName(param.getName());
            apiQuery.setType(param.getType());
            apiQuery.setExample(param.getExample());
            apiQuery.setDesc(param.getDesc());
            apiQuery.setRequired(param.getRequired() ? "1" : "0");
            return apiQuery;
        }).collect(Collectors.toList());

    }

    private List<YApiHeader> buildReqHeaders(List<Header> headerList) {

        if (CollectionUtils.isEmpty(headerList)) {
            return new ArrayList<>();
        }

        return headerList.stream().map(header -> {
            YApiHeader apiHeader = new YApiHeader();
            apiHeader.setName(header.getName());
            apiHeader.setDesc(header.getDesc());
            apiHeader.setValue(header.getValue());
            apiHeader.setRequired(header.getRequired() ? "1" : "0");
            return apiHeader;
        }).collect(Collectors.toList());


    }

    @NotNull
    private YApiCat getOrAddCat(@NotNull YApiSettings settings, @NotNull String name) throws Exception {

        YApiFacadeService facadeService = ApplicationManager.getApplication().getService(YApiFacadeServiceImpl.class);

        // 检查 catId (菜单是否存在)
        List<YApiCat> catMenu = facadeService.getCatMenu(settings.getUrl(), settings.getProjectId(), settings.getToken());

        Optional<YApiCat> catOptional = catMenu.stream()
                .filter(yApiCat -> yApiCat.getName().equals(name))
                .findAny();

        if (catOptional.isPresent()) {
            return catOptional.get();
        }
        YApiCat cat = new YApiCat();
        cat.setYapiUrl(settings.getUrl());
        cat.setProjectId(settings.getProjectId());
        cat.setName(name);
        cat.setToken(settings.getToken());

        return facadeService.addCat(cat);

    }

}
