package com.liuzhihang.doc.view.ui;

import com.intellij.openapi.project.Project;
import com.liuzhihang.doc.view.dto.Body;
import com.liuzhihang.doc.view.dto.DocView;
import com.liuzhihang.doc.view.dto.DocViewData;
import com.liuzhihang.doc.view.dto.Header;
import com.liuzhihang.doc.view.dto.Param;
import com.liuzhihang.doc.view.enums.FrameworkEnum;
import com.liuzhihang.doc.view.utils.VelocityUtils;

import java.util.ArrayList;
import java.util.List;

/** Renders templates against fixed fictional API data without scanning project PSI. */
public final class TemplatePreviewRenderer {

    private TemplatePreviewRenderer() {
    }

    public static String render(Project project, String template, FrameworkEnum framework) {
        DocView sample = sample(framework);
        return VelocityUtils.render(template, new DocViewData(project, sample));
    }

    private static DocView sample(FrameworkEnum framework) {
        DocView docView = new DocView("查询示例用户");
        docView.setDesc("根据示例用户编号查询虚构的用户资料。");
        docView.setPath(framework == FrameworkEnum.DUBBO
                ? "com.example.demo.DemoUserService.findUser"
                : "/api/demo/users/{userId}");
        docView.setMethod(framework == FrameworkEnum.DUBBO ? "Dubbo" : "GET");
        docView.setType(framework);

        Header header = new Header();
        header.setName("X-Request-Id");
        header.setValue("demo-request-001");
        header.setRequired(true);
        header.setDesc("虚构的请求追踪编号");
        docView.setHeaderList(List.of(header));

        Param userId = new Param();
        userId.setName("userId");
        userId.setType("Long");
        userId.setRequired(true);
        userId.setDesc("示例用户编号");
        userId.setExample("42");
        docView.setReqParamList(List.of(userId));

        Body requestUser = body("user", "DemoUser", true, "示例用户资料");
        requestUser.getChildList().add(body("displayName", "String", true, "显示名称"));
        requestUser.getChildList().add(body("active", "Boolean", false, "是否启用"));
        docView.getReqBody().setChildList(List.of(requestUser));

        Body responseUser = body("user", "DemoUser", true, "查询到的示例用户");
        responseUser.getChildList().add(body("userId", "Long", true, "示例用户编号"));
        responseUser.getChildList().add(body("displayName", "String", true, "示例用户名称"));
        docView.getRespBody().setChildList(List.of(responseUser));

        docView.setReqBodyExample("{\"userId\":42,\"filter\":\"active\"}");
        docView.setRespExample("{\"success\":true,\"data\":{\"userId\":42,\"displayName\":\"示例用户\"}}");
        return docView;
    }

    private static Body body(String name, String type, boolean required, String description) {
        Body body = new Body();
        body.setName(name);
        body.setType(type);
        body.setRequired(required);
        body.setDesc(description);
        return body;
    }
}
