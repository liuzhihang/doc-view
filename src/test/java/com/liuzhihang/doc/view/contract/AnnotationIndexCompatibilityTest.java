package com.liuzhihang.doc.view.contract;

import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.module.Module;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.testFramework.IndexingTestUtil;
import com.intellij.testFramework.fixtures.LightJavaCodeInsightFixtureTestCase;
import com.liuzhihang.doc.view.testsupport.AnnotationStubs;
import com.liuzhihang.doc.view.utils.DubboPsiUtils;
import com.liuzhihang.doc.view.utils.FeignPsiUtil;
import com.liuzhihang.doc.view.utils.SpringPsiUtils;

import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

public final class AnnotationIndexCompatibilityTest extends LightJavaCodeInsightFixtureTestCase {

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        AnnotationStubs.installSpringWeb(myFixture);
        AnnotationStubs.installFeign(myFixture);
        AnnotationStubs.installDubbo(myFixture);
    }

    public void testQualifiedAnnotationAliasesAreIndexedAndShortNameNoiseIsRejected() {
        addQualifiedCandidates();
        addShortNameNoise();
        waitForIndexes();

        assertEquals(Set.of(
                        "contract.index.SpringController",
                        "contract.index.SpringRestController"),
                scan(getModule(), SpringPsiUtils::findDocViewFromModule));
        assertEquals(Set.of("contract.index.OrderClient"),
                scan(getModule(), FeignPsiUtil::findDocViewFromModule));
        assertEquals(Set.of(
                        "contract.index.AlibabaLegacyService",
                        "contract.index.ApacheLegacyService",
                        "contract.index.ModernDubboService"),
                scan(getModule(), DubboPsiUtils::findDocViewFromModule));
    }

    public void testDubboServiceIsFoundWhenLegacyServiceIndexIsEmpty() {
        myFixture.addClass("""
                package contract.index;

                @org.apache.dubbo.config.annotation.DubboService
                public interface ModernOnlyService {
                }
                """);
        waitForIndexes();

        assertEquals(Set.of("contract.index.ModernOnlyService"),
                scan(getModule(), DubboPsiUtils::findDocViewFromModule));
    }

    private void addQualifiedCandidates() {
        myFixture.addClass("""
                package contract.index;

                @org.springframework.stereotype.Controller
                public class SpringController {
                }
                """);
        myFixture.addClass("""
                package contract.index;

                @org.springframework.web.bind.annotation.RestController
                public class SpringRestController {
                }
                """);
        myFixture.addClass("""
                package contract.index;

                @org.springframework.cloud.openfeign.FeignClient
                public interface OrderClient {
                }
                """);
        myFixture.addClass("""
                package contract.index;

                @org.apache.dubbo.config.annotation.Service
                public interface ApacheLegacyService {
                }
                """);
        myFixture.addClass("""
                package contract.index;

                @com.alibaba.dubbo.config.annotation.Service
                public interface AlibabaLegacyService {
                }
                """);
        myFixture.addClass("""
                package contract.index;

                @org.apache.dubbo.config.annotation.DubboService
                public interface ModernDubboService {
                }
                """);
    }

    private void addShortNameNoise() {
        myFixture.addClass("""
                package wrong.annotations;

                public @interface Controller {
                }
                """);
        myFixture.addClass("""
                package wrong.annotations;

                public @interface RestController {
                }
                """);
        myFixture.addClass("""
                package wrong.annotations;

                public @interface FeignClient {
                }
                """);
        myFixture.addClass("""
                package wrong.annotations;

                public @interface Service {
                }
                """);
        myFixture.addClass("""
                package wrong.annotations;

                public @interface DubboService {
                }
                """);
        myFixture.addClass("""
                package contract.noise;

                @wrong.annotations.Controller
                public class WrongController {
                }
                """);
        myFixture.addClass("""
                package contract.noise;

                @wrong.annotations.RestController
                public class WrongRestController {
                }
                """);
        myFixture.addClass("""
                package contract.noise;

                @wrong.annotations.FeignClient
                public interface WrongClient {
                }
                """);
        myFixture.addClass("""
                package contract.noise;

                @wrong.annotations.Service
                public interface WrongLegacyService {
                }
                """);
        myFixture.addClass("""
                package contract.noise;

                @wrong.annotations.DubboService
                public interface WrongDubboService {
                }
                """);
    }

    private void waitForIndexes() {
        PsiDocumentManager.getInstance(getProject()).commitAllDocuments();
        IndexingTestUtil.waitUntilIndexesAreReady(getProject());
    }

    private static Set<String> scan(
            Module module,
            Function<Module, List<PsiClass>> scanner) {
        return ReadAction.computeBlocking(() -> scanner.apply(module).stream()
                .map(PsiClass::getQualifiedName)
                .collect(Collectors.toSet()));
    }
}
