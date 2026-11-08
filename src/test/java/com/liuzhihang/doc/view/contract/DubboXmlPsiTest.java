package com.liuzhihang.doc.view.contract;

import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.progress.EmptyProgressIndicator;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiFile;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.searches.DefinitionsScopedSearch;
import com.intellij.psi.xml.XmlTag;
import com.intellij.testFramework.IndexingTestUtil;
import com.intellij.testFramework.fixtures.LightJavaCodeInsightFixtureTestCase;
import com.liuzhihang.doc.view.dom.DubboDefinitionSearch;
import com.liuzhihang.doc.view.dom.DubboXmlPsi;
import com.liuzhihang.doc.view.testsupport.AnnotationStubs;
import com.liuzhihang.doc.view.utils.DubboPsiUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

public final class DubboXmlPsiTest extends LightJavaCodeInsightFixtureTestCase {

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        AnnotationStubs.installDubbo(myFixture);
    }

    public void testRecognizesIndexedBeansServiceWithDecodedInterfaceAttribute() {
        PsiClass candidate = myFixture.addClass("""
                package contract.xml;
                public interface IndexedCandidate { }
                """);
        addInterface("SecondIndexedCandidate");
        myFixture.addFileToProject("src/dubbo-index-candidate.xml", """
                <beans xmlns="urn:beans" xmlns:dubbo="urn:any">
                    <dubbo:service interface="contract&#x2e;xml&#x2e;IndexedCandidate"/>
                </beans>
                """);
        myFixture.addFileToProject("src/dubbo-second-index-candidate.xml", """
                <beans xmlns="urn:beans" xmlns:dubbo="urn:any">
                    <dubbo:service interface="contract.xml.SecondIndexedCandidate"/>
                </beans>
                """);
        waitUntilIndexesAreReady();

        Set<String> interfaceNames = serviceInterfaceNames(GlobalSearchScope.projectScope(getProject()));
        assertTrue(interfaceNames.toString(), interfaceNames.contains("contract.xml.IndexedCandidate"));
        assertTrue(interfaceNames.toString(), interfaceNames.contains("contract.xml.SecondIndexedCandidate"));
        assertTrue(ReadAction.computeBlocking(() -> DubboPsiUtils.isDubboClass(candidate)));
    }

    public void testCommittedUnsavedDeclarationIsFoundWithoutSaving() {
        PsiFile xml = myFixture.addFileToProject("src/dubbo-index-unsaved.xml", """
                <beans xmlns="urn:beans" xmlns:dubbo="urn:any"/>
                """);
        waitUntilIndexesAreReady();
        Document document = PsiDocumentManager.getInstance(getProject()).getDocument(xml);
        assertNotNull(document);

        WriteAction.run(() -> document.setText("""
                <beans xmlns="urn:beans" xmlns:dubbo="urn:any">
                    <dubbo:service interface="contract.xml.UnsavedCandidate"/>
                </beans>
                """));
        PsiDocumentManager.getInstance(getProject()).commitDocument(document);

        assertTrue(FileDocumentManager.getInstance().isDocumentUnsaved(document));
        assertTrue(PsiDocumentManager.getInstance(getProject()).isCommitted(document));
        assertTrue(serviceInterfaceNames(GlobalSearchScope.projectScope(getProject()))
                .contains("contract.xml.UnsavedCandidate"));
    }

    public void testMissingOrBlankInterfaceAndNonMatchingTagBoundariesAreIgnored() {
        PsiClass missingAttribute = addInterface("MissingAttribute");
        PsiClass blankAttribute = addInterface("BlankAttribute");
        PsiClass wrongRoot = addInterface("WrongRoot");
        PsiClass nestedTag = addInterface("NestedTag");
        PsiClass wrongTag = addInterface("WrongTag");

        myFixture.addFileToProject("src/dubbo-missing-attribute.xml", """
                <beans xmlns="urn:beans" xmlns:dubbo="urn:any">
                    <dubbo:service ref="provider"/>
                </beans>
                """);
        myFixture.addFileToProject("src/dubbo-blank-attribute.xml", """
                <beans xmlns="urn:beans" xmlns:dubbo="urn:any">
                    <dubbo:service interface="  " ref="provider"/>
                </beans>
                """);
        myFixture.addFileToProject("src/dubbo-wrong-root.xml", """
                <other xmlns="urn:other" xmlns:dubbo="urn:any">
                    <dubbo:service interface="contract.xml.WrongRoot"/>
                </other>
                """);
        myFixture.addFileToProject("src/dubbo-nested-service.xml", """
                <beans xmlns="urn:beans" xmlns:dubbo="urn:any">
                    <container><dubbo:service interface="contract.xml.NestedTag"/></container>
                </beans>
                """);
        myFixture.addFileToProject("src/dubbo-unprefixed-service.xml", """
                <beans xmlns="urn:beans" xmlns:dubbo="urn:any">
                    <service interface="contract.xml.WrongTag"/>
                </beans>
                """);
        waitUntilIndexesAreReady();

        for (PsiClass candidate : List.of(missingAttribute, blankAttribute, wrongRoot, nestedTag, wrongTag)) {
            assertFalse(candidate.getQualifiedName(), ReadAction.computeBlocking(
                    () -> DubboPsiUtils.isDubboClass(candidate)));
        }
        Set<String> discovered = ReadAction.computeBlocking(() -> DubboPsiUtils.findDocViewFromModule(getModule())
                .stream().map(PsiClass::getQualifiedName).collect(Collectors.toSet()));
        assertTrue(discovered.isEmpty());
    }

    public void testDefinitionSearchStopsWhenConsumerRejectsDeclaration() throws Exception {
        PsiClass candidate = addInterface("ConsumerStopCandidate");
        myFixture.addFileToProject("src/dubbo-consumer-stop.xml", """
                <beans xmlns="urn:beans" xmlns:dubbo="urn:any">
                    <dubbo:service interface="contract.xml.ConsumerStopCandidate"/>
                    <dubbo:service interface="contract.xml.ConsumerStopCandidate"/>
                </beans>
                """);
        waitUntilIndexesAreReady();
        AtomicInteger consumed = new AtomicInteger();
        AtomicReference<XmlTag> navigationTarget = new AtomicReference<>();

        ReadAction.run(() -> new DubboDefinitionSearch().processQuery(
                new DefinitionsScopedSearch.SearchParameters(candidate), element -> {
                    consumed.incrementAndGet();
                    assertTrue("Definition navigation must return the actual XML service tag", element instanceof XmlTag);
                    navigationTarget.set((XmlTag) element);
                    return false;
                }));

        assertEquals(1, consumed.get());
        assertNotNull(navigationTarget.get());
        assertEquals("dubbo:service", navigationTarget.get().getName());
        assertEquals("contract.xml.ConsumerStopCandidate", DubboXmlPsi.getInterfaceName(navigationTarget.get()));
    }

    public void testServiceTagSearchPropagatesCancellation() {
        myFixture.addFileToProject("src/dubbo-cancel.xml", """
                <beans xmlns="urn:beans" xmlns:dubbo="urn:any">
                    <dubbo:service interface="contract.xml.CancelCandidate"/>
                </beans>
                """);
        waitUntilIndexesAreReady();
        EmptyProgressIndicator indicator = new EmptyProgressIndicator();
        try {
            ProgressManager.getInstance().runProcess(() -> {
                indicator.cancel();
                ReadAction.computeBlocking(() -> DubboXmlPsi.processServiceTags(getProject(),
                        GlobalSearchScope.projectScope(getProject()), tag -> true));
            }, indicator);
            fail("Expected XML PSI discovery to propagate cancellation");
        } catch (ProcessCanceledException expected) {
            // Cancellation is propagated before further candidate processing.
        }
    }

    private PsiClass addInterface(String simpleName) {
        return myFixture.addClass("""
                package contract.xml;
                public interface %s { }
                """.formatted(simpleName));
    }

    private Set<String> serviceInterfaceNames(GlobalSearchScope scope) {
        Project project = getProject();
        return ReadAction.computeBlocking(() -> {
            List<String> interfaceNames = new ArrayList<>();
            DubboXmlPsi.processServiceTags(project, scope, (XmlTag tag) -> {
                String interfaceName = DubboXmlPsi.getInterfaceName(tag);
                if (interfaceName != null) {
                    interfaceNames.add(interfaceName);
                }
                return true;
            });
            return Set.copyOf(interfaceNames);
        });
    }

    private void waitUntilIndexesAreReady() {
        IndexingTestUtil.waitUntilIndexesAreReady(getProject());
    }
}
