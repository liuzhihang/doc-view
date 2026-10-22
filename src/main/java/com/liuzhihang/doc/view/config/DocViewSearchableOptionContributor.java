package com.liuzhihang.doc.view.config;

import com.intellij.ide.ui.search.SearchableOptionContributor;
import com.intellij.ide.ui.search.SearchableOptionProcessor;
import com.liuzhihang.doc.view.DocViewBundle;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/** Adds Chinese and English keywords to the corresponding project settings pages. */
public final class DocViewSearchableOptionContributor extends SearchableOptionContributor {

    private static final String ROOT_ID = "liuzhihang.api.doc.SettingsConfigurable";
    private static final String RULES_ID = "liuzhihang.api.doc.GenerationRulesConfigurable";
    private static final String TEMPLATE_ID = "liuzhihang.api.doc.TemplateConfigurable";
    private static final String INTEGRATIONS_ID = "liuzhihang.api.doc.IntegrationSettingsConfigurable";
    @Override
    public void processOptions(@NotNull SearchableOptionProcessor processor) {
        add(processor, ROOT_ID, DocViewBundle.message("settings.page.root"), DocViewBundle.message("settings.page.root"),
                "Doc View", "常规行为", "行标记", "编辑器行标记", "line marker", "普通接口", "普通接口识别", "ordinary interface");
        add(processor, RULES_ID, DocViewBundle.message("settings.page.rules"), DocViewBundle.message("settings.page.rules"),
                "生成规则", "接口标题", "接口名称", "接口描述", "必填", "字段名", "JsonProperty",
                "generation rules", "title", "operation summary", "method name", "required", "field name");
        String templatePageName = DocViewBundle.message("settings.page.template");
        add(processor, TEMPLATE_ID, templatePageName, templatePageName,
                "模板", "变量", "模板变量", "Velocity", "恢复默认模板", "预览", "Markdown", "导出", "独立实体", "合并导出",
                "前缀符号", "嵌套层级前缀", "template", "template variables", "restore defaults", "preview",
                "markdown", "merge export", "separate entities", "nested prefix");
        add(processor, INTEGRATIONS_ID, DocViewBundle.message("settings.page.integrations"), DocViewBundle.message("settings.page.integrations"),
                "平台集成", "YApi", "ShowDoc", "语雀", "集成指南", "platform integrations", "integration guide");
        add(processor, TEMPLATE_ID, templatePageName,
                DocViewBundle.message("template.variables.title"),
                "DocView.name", "DocView.desc", "DocView.path", "DocView.method", "DocView.requestHeader",
                "DocView.requestParam", "DocView.requestBody", "DocView.requestExample", "DocView.responseParam",
                "DocView.responseExample", "DocView.type", "DocView.responseParamDataList");
    }

    private static void add(SearchableOptionProcessor processor, String configurableId,
                            String displayName, String hit, String... terms) {
        for (String term : terms) {
            processor.addOptions(term, displayName, hit, configurableId, displayName, false);
        }
    }
}
