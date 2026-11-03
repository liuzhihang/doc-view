package com.liuzhihang.doc.view.contract;

import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiJavaFile;
import com.intellij.testFramework.fixtures.LightJavaCodeInsightFixtureTestCase;
import com.intellij.testFramework.LightProjectDescriptor;
import com.liuzhihang.doc.view.service.impl.SpringDocViewServiceImpl;
import com.liuzhihang.doc.view.utils.SpringPsiUtils;

public final class SpringRouteSelectionTest extends LightJavaCodeInsightFixtureTestCase {
    @Override
    protected LightProjectDescriptor getProjectDescriptor() {
        return JAVA_21;
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        for (String name : new String[]{"RequestMapping", "GetMapping"}) {
            myFixture.addClass("package org.springframework.web.bind.annotation; public @interface "
                    + name + " { String[] value() default {}; String[] path() default {}; }");
        }
    }

    public void testSelectsFirstClassAndMethodArrayValues() {
        PsiClass controller = controller("@RequestMapping({\"/aa/aa\", \"/bb/bb\"})",
                "@GetMapping({\"/first\", \"/second\"})");
        assertEquals("/aa/aa/first", new SpringDocViewServiceImpl()
                .buildClassMethodDoc(controller, controller.findMethodsByName("find", false)[0]).getPath());
    }

    public void testPathAliasAndConstantExpressions() {
        PsiClass controller = controller("@RequestMapping(path={\"api/\", \"backup\"})",
                "@GetMapping(path={Routes.PREFIX + \"users/\", \"ignored\"})");
        assertEquals("/api/v1/users", SpringPsiUtils.path(controller, controller.getMethods()[0]));
    }

    public void testScalarValueAndEmptyPath() {
        PsiClass controller = controller("@RequestMapping(\"/api/\")", "@GetMapping(value={})");
        assertEquals("/api", SpringPsiUtils.path(controller, controller.getMethods()[0]));
    }

    public void testEmptyValueArrayFallsBackToPathAlias() {
        PsiClass controller = controller("@RequestMapping(value={}, path=\"/api\")", "@GetMapping(path=\"/users\")");
        assertEquals("/api/users", SpringPsiUtils.path(controller, controller.getMethods()[0]));
    }

    public void testUnresolvedPathDoesNotBecomeSourceExpression() {
        PsiClass controller = controller("@RequestMapping(Unknown.PATH)", "@GetMapping(\"/users\")");
        assertEquals("/users", SpringPsiUtils.path(controller, controller.getMethods()[0]));
    }

    private PsiClass controller(String classMapping, String methodMapping) {
        return ((PsiJavaFile) myFixture.configureByText("RouteController.java", """
                import org.springframework.web.bind.annotation.*;
                %s
                class RouteController {
                    %s public void find() {}
                }
                class Routes { static final String PREFIX = "v1/"; }
                """.formatted(classMapping, methodMapping))).getClasses()[0];
    }
}
