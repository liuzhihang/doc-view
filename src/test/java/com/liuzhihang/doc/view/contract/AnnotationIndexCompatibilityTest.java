package com.liuzhihang.doc.view.contract;

import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.progress.EmptyProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiFile;
import com.intellij.psi.xml.XmlFile;
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

    public void testDubboDiscoveryIncludesXmlAndAnnotatedImplementationContractsOnce() {
        myFixture.addClass("""
                package contract.index;

                public interface XmlOnlyContract {
                    String lookup(String id);
                }
                """);
        myFixture.addClass("""
                package contract.index;

                public interface ParentContract {
                    String parent();
                }
                """);
        myFixture.addClass("""
                package contract.index;

                public interface ChildContract extends ParentContract {
                    String child();
                }
                """);
        myFixture.addClass("""
                package contract.index;

                @org.apache.dubbo.config.annotation.DubboService
                public abstract class AnnotatedServiceBase implements ChildContract {
                }
                """);
        myFixture.addClass("""
                package contract.index;

                @org.apache.dubbo.config.annotation.Service
                public interface XmlAndAnnotatedContract {
                    String shared();
                }
                """);
        PsiFile xmlPsi = myFixture.addFileToProject("src/contract-index-dubbo.xml", """
                <?xml version="1.0" encoding="UTF-8"?>
                <beans xmlns="http://www.springframework.org/schema/beans"
                       xmlns:dubbo="http://dubbo.apache.org/schema/dubbo">
                    <dubbo:service interface="contract.index.XmlOnlyContract" ref="xmlOnly"/>
                    <dubbo:service interface="contract.index.XmlAndAnnotatedContract" ref="duplicate"/>
                    <dubbo:service interface="contract.index.MissingContract" ref="missing"/>
                </beans>
                """);
        assertTrue(xmlPsi instanceof XmlFile);
        waitForIndexes();

        List<PsiClass> result = scanList(getModule());
        Set<String> names = result.stream().map(PsiClass::getQualifiedName).collect(Collectors.toSet());
        assertTrue(names.toString(), names.contains("contract.index.XmlOnlyContract"));
        assertTrue(names.toString(), names.contains("contract.index.ParentContract"));
        assertTrue(names.toString(), names.contains("contract.index.ChildContract"));
        assertTrue(names.toString(), names.contains("contract.index.XmlAndAnnotatedContract"));
        assertEquals("XML and annotation registrations produce one catalog entry",
                1, result.stream().filter(psiClass -> "contract.index.XmlAndAnnotatedContract".equals(psiClass.getQualifiedName())).count());
        assertFalse(names.toString(), names.contains("contract.index.MissingContract"));
    }

    public void testDubboDiscoveryStopsWhenCanceled() {
        myFixture.addClass("""
                package contract.index;

                @org.apache.dubbo.config.annotation.DubboService
                public interface CancellableService {
                }
                """);
        waitForIndexes();
        EmptyProgressIndicator indicator = new EmptyProgressIndicator();

        try {
            ProgressManager.getInstance().runProcess(() -> {
                indicator.cancel();
                scanList(getModule());
            }, indicator);
            fail("Expected the module scan to propagate cancellation");
        } catch (com.intellij.openapi.progress.ProcessCanceledException expected) {
            // The scanner must not continue enumerating annotations or XML after cancellation.
        }
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

    private static List<PsiClass> scanList(Module module) {
        return ReadAction.computeBlocking(() -> DubboPsiUtils.findDocViewFromModule(module));
    }
}
