package com.liuzhihang.doc.view.contract;

import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ModifiableRootModel;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.roots.ModuleOrderEntry;
import com.intellij.openapi.roots.OrderEntry;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.PsiErrorElement;
import com.intellij.psi.xml.XmlFile;
import com.intellij.testFramework.IndexingTestUtil;
import com.intellij.testFramework.LightProjectDescriptor;
import com.intellij.testFramework.fixtures.DefaultLightProjectDescriptor;
import com.intellij.testFramework.fixtures.LightJavaCodeInsightFixtureTestCase;
import com.intellij.testFramework.VfsTestUtil;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.xml.XmlTag;
import com.liuzhihang.doc.view.testsupport.AnnotationStubs;
import com.liuzhihang.doc.view.utils.DubboPsiUtils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public final class DubboMultiModuleCatalogTest extends LightJavaCodeInsightFixtureTestCase {

    private static final String PROVIDER_MODULE = "doc-view-dubbo-provider";
    private static final String DUPLICATE_API_MODULE = "doc-view-dubbo-duplicate-api";

    @Override
    protected LightProjectDescriptor getProjectDescriptor() {
        return new DubboCatalogProjectDescriptor();
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        AnnotationStubs.installDubbo(myFixture);
    }

    public void testProviderAnnotationsAndXmlAreAttributedToResolvedApiSourceModule() {
        Module apiModule = ModuleManager.getInstance(getProject())
                .findModuleByName(LightProjectDescriptor.TEST_MODULE_NAME);
        Module providerModule = ModuleManager.getInstance(getProject()).findModuleByName(PROVIDER_MODULE);
        Module duplicateApiModule = ModuleManager.getInstance(getProject()).findModuleByName(DUPLICATE_API_MODULE);
        assertNotNull(apiModule);
        assertNotNull(providerModule);
        assertNotNull(duplicateApiModule);

        myFixture.addClass("""
                package contract.multimodule;

                public interface SharedApi {
                    String shared();
                }
                """);
        myFixture.addClass("""
                package contract.multimodule;

                public interface XmlOnlyApi {
                    String xmlOnly();
                }
                """);
        VirtualFile providerSource = addFile(providerModule, "contract/provider/SharedApiProvider.java", """
                package contract.provider;

                @org.apache.dubbo.config.annotation.DubboService
                public class SharedApiProvider implements contract.multimodule.SharedApi {
                    public String shared() { return "ok"; }
                }
                """);
        addFile(duplicateApiModule, "contract/multimodule/SharedApi.java", """
                package contract.multimodule;

                public interface SharedApi {
                    String duplicate();
                }
                """);
        addFile(duplicateApiModule, "contract/multimodule/XmlOnlyApi.java", """
                package contract.multimodule;

                public interface XmlOnlyApi {
                    String duplicate();
                }
                """);
        VirtualFile xml = addFile(providerModule, "dubbo-provider.xml", """
                <?xml version="1.0" encoding="UTF-8"?>
                <beans xmlns="http://www.springframework.org/schema/beans"
                       xmlns:dubbo="http://dubbo.apache.org/schema/dubbo">
                    <dubbo:service interface="contract.multimodule.SharedApi" ref="provider"/>
                    <dubbo:service interface="contract.multimodule.XmlOnlyApi" ref="xmlOnly"/>
                </beans>
                """);
        PsiDocumentManager.getInstance(getProject()).commitAllDocuments();
        IndexingTestUtil.waitUntilIndexesAreReady(getProject());

        PsiFile xmlPsi = PsiManager.getInstance(getProject()).findFile(xml);
        assertTrue(xmlPsi instanceof XmlFile);
        PsiFile providerPsi = PsiManager.getInstance(getProject()).findFile(providerSource);
        assertNotNull(providerPsi);
        assertTrue("The provider fixture must parse without PSI errors",
                PsiTreeUtil.findChildrenOfType(providerPsi, PsiErrorElement.class).isEmpty());
        XmlTag root = ((XmlFile) xmlPsi).getRootTag();
        assertNotNull("The provider XML must expose its root through XML PSI", root);
        assertEquals("beans", root.getName());
        assertEquals("dubbo:service", root.getSubTags()[0].getName());

        assertEquals(Set.of("contract.multimodule.SharedApi", "contract.multimodule.XmlOnlyApi"), names(apiModule));
        assertTrue("Provider declarations belong to API source module, not provider source module",
                names(providerModule).isEmpty());
        assertTrue("The same FQN in an unrelated module is not resolved from provider XML",
                names(duplicateApiModule).isEmpty());
    }

    private VirtualFile addFile(Module module, String relativePath, String text) {
        VirtualFile sourceRoot = ModuleRootManager.getInstance(module).getSourceRoots()[0];
        return VfsTestUtil.createFile(sourceRoot, relativePath, text);
    }

    private Set<String> names(Module module) {
        return ReadAction.computeBlocking(() -> DubboPsiUtils.findDocViewFromModule(module).stream()
                .map(PsiClass::getQualifiedName)
                .collect(Collectors.toSet()));
    }

    private static final class DubboCatalogProjectDescriptor extends DefaultLightProjectDescriptor {

        @Override
        public void setUpProject(Project project, SetupHandler handler) throws Exception {
            super.setUpProject(project, handler);
            Module apiModule = ModuleManager.getInstance(project)
                    .findModuleByName(LightProjectDescriptor.TEST_MODULE_NAME);
            if (apiModule == null) {
                throw new IllegalStateException("Default Java API module was not created");
            }
            String uniqueSuffix = UUID.randomUUID().toString();
            Module providerModule = createModule(project,
                    Path.of(FileUtil.getTempDirectory(), PROVIDER_MODULE + ".iml"));
            Module duplicateApiModule = createModule(project,
                    Path.of(FileUtil.getTempDirectory(), DUPLICATE_API_MODULE + ".iml"));
            configureSourceRoot(providerModule, "doc-view-dubbo-provider-src-" + uniqueSuffix);
            configureSourceRoot(duplicateApiModule, "doc-view-dubbo-duplicate-api-src-" + uniqueSuffix);
            WriteAction.run(() -> {
                ModifiableRootModel model = ModuleRootManager.getInstance(providerModule).getModifiableModel();
                model.addModuleOrderEntry(apiModule);
                model.commit();
            });
        }

        private void configureSourceRoot(Module module, String rootName) {
            WriteAction.run(() -> {
                VirtualFile sourceRoot = createSourceRoot(module, rootName);
                createContentEntry(module, sourceRoot);
            });
        }
    }
}
