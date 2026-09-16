package com.liuzhihang.doc.view.contract;

import com.liuzhihang.doc.view.dto.DocViewData;
import com.liuzhihang.doc.view.service.impl.YApiServiceImpl;
import junit.framework.TestCase;

import java.io.InputStream;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.List;

public final class CollectionDependencyContractTest extends TestCase {

    public void testRenderingAndYApiDoNotLinkCommonsCollections() throws Exception {
        for (Class<?> type : List.of(DocViewData.class, YApiServiceImpl.class)) {
            try (InputStream stream = type.getResourceAsStream(type.getSimpleName() + ".class")) {
                assertNotNull(stream);
                String bytecode = new String(stream.readAllBytes(), StandardCharsets.ISO_8859_1);
                assertFalse(type.getName() + " still links Commons Collections",
                        bytecode.contains("org/apache/commons/collections/"));
            }
        }
    }

    public void testNullAndEmptyMarkdownParametersRemainEmpty() throws Exception {
        for (String name : List.of("paramMarkdown", "separateParamMarkdown", "separateSubParamMarkdown")) {
            Method method = DocViewData.class.getDeclaredMethod(name, List.class);
            method.setAccessible(true);
            assertEquals("", method.invoke(null, new Object[]{null}));
            assertEquals("", method.invoke(null, List.of()));
        }
    }

    public void testNullAndEmptyYApiParametersRemainEmpty() throws Exception {
        YApiServiceImpl service = new YApiServiceImpl();
        for (String name : List.of("buildReqQuery", "buildReqHeaders")) {
            Method method = YApiServiceImpl.class.getDeclaredMethod(name, List.class);
            method.setAccessible(true);
            assertEquals(List.of(), method.invoke(service, new Object[]{null}));
            assertEquals(List.of(), method.invoke(service, List.of()));
        }
    }
}
