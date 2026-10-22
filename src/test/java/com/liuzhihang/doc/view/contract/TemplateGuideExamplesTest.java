package com.liuzhihang.doc.view.contract;

import com.intellij.testFramework.fixtures.LightJavaCodeInsightFixtureTestCase;
import com.liuzhihang.doc.view.DocViewBundle;
import com.liuzhihang.doc.view.dto.Body;
import com.liuzhihang.doc.view.dto.DocView;
import com.liuzhihang.doc.view.dto.DocViewData;
import com.liuzhihang.doc.view.dto.Param;
import com.liuzhihang.doc.view.enums.FrameworkEnum;
import com.liuzhihang.doc.view.utils.VelocityUtils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/** Executes the copyable guide text, rather than a second copy of its templates. */
public final class TemplateGuideExamplesTest extends LightJavaCodeInsightFixtureTestCase {
    private String guide;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        guide = Files.readString(Path.of("docs/user-guide/templates.md"));
    }

    public void testDocumentedDefaultsMatchBundledTemplates() {
        assertEquals(DocViewBundle.message("template.spring.init").stripTrailing(),
                snippetAfter("**Spring / Feign**"));
        assertEquals(DocViewBundle.message("template.dubbo.init").stripTrailing(),
                snippetAfter("**Dubbo**"));
    }

    public void testTitleAndParamExamplesUseTheDocumentedInput() {
        DocView doc = sample();
        DocViewData data = new DocViewData(getProject(), doc);
        assertEquals("# 查询用户", VelocityUtils.render(snippetAfter("## 示例一："), data).strip());
        String table = VelocityUtils.render(snippetAfter("## 示例二："), data);
        assertTrue(table, table.contains("|id|String|用户编号|"));
        assertFalse(table, table.contains("${"));
    }

    public void testRecursiveMacroIncludesChildrenAndStopsAtEmptyLists() {
        DocView doc = sample();
        Body profile = body("profile", "Profile", true, "用户资料");
        Body address = body("address", "Address", false, "地址");
        address.getChildList().add(body("city", "String", true, "城市"));
        profile.getChildList().add(address);
        profile.getChildList().add(body("enabled", "Boolean", false, "启用状态"));
        doc.getReqBody().setChildList(List.of(profile));
        String table = VelocityUtils.render(snippetAfter("下面是可选的递归宏示例"),
                new DocViewData(getProject(), doc));
        assertTrue(table, table.contains("profile|Profile|是|"));
        assertTrue(table, table.contains("address|Address| 否|"));
        assertTrue(table, table.contains("city|String|是|"));
        assertTrue(table, table.contains("enabled|Boolean| 否|"));
        assertEquals(table, 1, occurrences(table, "city|String|"));
        assertFalse(table, table.contains("$"));

        doc.getReqBody().setChildList(List.of());
        String empty = VelocityUtils.render(snippetAfter("下面是可选的递归宏示例"),
                new DocViewData(getProject(), doc));
        assertFalse(empty, empty.contains("Profile"));
        assertTrue(empty, empty.contains("|参数名|类型|必填|示例|描述|"));
    }

    private String snippetAfter(String heading) {
        int start = guide.indexOf(heading);
        assertTrue("Missing guide section: " + heading, start >= 0);
        StringBuilder code = new StringBuilder();
        boolean found = false;
        for (String line : guide.substring(start).split("\n", -1)) {
            if (line.startsWith("    ")) {
                found = true;
                code.append(line.substring(4)).append('\n');
            } else if (found && line.isBlank()) {
                code.append('\n');
            } else if (found) {
                break;
            }
        }
        assertTrue("Missing copyable snippet: " + heading, found);
        return code.toString().stripTrailing();
    }

    private static DocView sample() {
        DocView doc = new DocView("查询用户");
        doc.setType(FrameworkEnum.SPRING);
        Param param = new Param();
        param.setName("id");
        param.setType("String");
        param.setRequired(true);
        param.setDesc("用户编号");
        doc.setReqParamList(List.of(param));
        return doc;
    }

    private static Body body(String name, String type, boolean required, String description) {
        Body body = new Body();
        body.setName(name);
        body.setType(type);
        body.setRequired(required);
        body.setDesc(description);
        return body;
    }

    private static int occurrences(String text, String needle) {
        return (text.length() - text.replace(needle, "").length()) / needle.length();
    }
}
