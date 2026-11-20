package com.liuzhihang.doc.view.testsupport;

import com.intellij.testFramework.fixtures.JavaCodeInsightTestFixture;

public final class AnnotationStubs {

    private AnnotationStubs() {
    }

    public static void installSpringWeb(JavaCodeInsightTestFixture fixture) {
        fixture.addClass("""
                package org.springframework.stereotype;

                public @interface Controller {
                }
                """);
        fixture.addClass("""
                package org.springframework.web.bind.annotation;

                public @interface RestController {
                }
                """);
        fixture.addClass("""
                package org.springframework.web.bind.annotation;

                public @interface RequestMapping {
                    String[] value() default {};
                    String method() default "";
                }
                """);
        fixture.addClass("""
                package org.springframework.web.bind.annotation;

                public @interface GetMapping {
                    String[] value() default {};
                }
                """);
        fixture.addClass("""
                package org.springframework.web.bind.annotation;

                public @interface RequestParam {
                    String value() default "";
                    String name() default "";

                    String defaultValue() default "\\n\\t\\t\\n\\t\\t\\n\\uE000\\uE001\\uE002\\n\\t\\t\\t\\t\\n";
                    boolean required() default true;
                }
                """);
        fixture.addClass("""
                package org.springframework.web.bind.annotation;

                public @interface RequestHeader {
                    String value() default "";
                    String name() default "";
                    String defaultValue() default "\\n\\t\\t\\n\\t\\t\\n\\uE000\\uE001\\uE002\\n\\t\\t\\t\\t\\n";
                    boolean required() default true;
                }
                """);
    }

    public static void installFeign(JavaCodeInsightTestFixture fixture) {
        fixture.addClass("""
                package org.springframework.cloud.openfeign;

                public @interface FeignClient {
                }
                """);
    }

    public static void installDubbo(JavaCodeInsightTestFixture fixture) {
        fixture.addClass("""
                package org.apache.dubbo.config.annotation;

                public @interface DubboService {
                }
                """);
        fixture.addClass("""
                package org.apache.dubbo.config.annotation;

                public @interface Service {
                }
                """);
        fixture.addClass("""
                package com.alibaba.dubbo.config.annotation;

                public @interface Service {
                }
                """);
    }

    public static void installOpenApi3(JavaCodeInsightTestFixture fixture) {
        fixture.addClass("""
                package io.swagger.v3.oas.annotations;

                public @interface Operation {
                    String summary() default "";

                    String description() default "";
                }
                """);
    }

    public static void installSwagger2(JavaCodeInsightTestFixture fixture) {
        fixture.addClass("""
                package io.swagger.annotations;

                public @interface ApiOperation {
                    String value() default "";

                    String notes() default "";
                }
                """);
    }
}
