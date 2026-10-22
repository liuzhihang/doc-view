package com.liuzhihang.doc.view.ui.window;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.util.AbstractProgressIndicatorBase;
import com.intellij.openapi.util.Computable;
import com.intellij.psi.PsiClass;
import com.intellij.testFramework.LightProjectDescriptor;
import com.intellij.testFramework.ServiceContainerUtil;
import com.intellij.testFramework.fixtures.LightJavaCodeInsightFixtureTestCase;
import com.liuzhihang.doc.view.config.Settings;
import com.liuzhihang.doc.view.testsupport.AnnotationStubs;

import java.util.Set;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/** Cancellation coverage for the real PSI-to-class-node method traversal. */
public final class ClassNodeCancellationTest extends LightJavaCodeInsightFixtureTestCase {

    @Override
    protected LightProjectDescriptor getProjectDescriptor() {
        return JAVA_21;
    }

    public void testCancellationDuringFirstMethodNodeStopsFilteringFollowingMethods() throws Exception {
        AnnotationStubs.installSpringWeb(myFixture);
        PsiClass controller = myFixture.addClass("""
                package contract.cancellation;
                import org.springframework.web.bind.annotation.GetMapping;
                import org.springframework.web.bind.annotation.RestController;

                @RestController
                public class CancellationController {
                    @GetMapping("/first") public void first(String value) {}
                    @GetMapping("/second") public void second(String value) {}
                }
                """);

        // Build the node with ordinary settings first, then cancel while the first
        // matching method is being rebuilt.
        ClassNode node = new ClassNode(new RootNode(), controller);
        AbstractProgressIndicatorBase indicator = new AbstractProgressIndicatorBase();
        CancelAfterFirstMethodSettings settings = new CancelAfterFirstMethodSettings(indicator);
        ServiceContainerUtil.replaceService(getProject(), Settings.class, settings, getTestRootDisposable());

        Future<Boolean> canceled = ApplicationManager.getApplication().executeOnPooledThread(() -> {
            try {
                ProgressManager.getInstance().runProcess(() ->
                        ApplicationManager.getApplication().runReadAction((Computable<Void>) () -> {
                            node.updateNode(getProject());
                            return null;
                        }), indicator);
                return false;
            } catch (ProcessCanceledException expected) {
                return true;
            }
        });

        assertTrue("Cancellation must propagate out of the class method traversal", canceled.get(10, TimeUnit.SECONDS));
        assertTrue("The test seam must cancel the active refresh indicator", indicator.isCanceled());
        assertEquals("The first method must pass the method filter before cancellation", 1,
                settings.methodFilterCalls.get());
        assertEquals("The first eligible method must enter MethodNode construction", 1,
                settings.methodNodeNameLookups.get());

    }

    private static final class CancelAfterFirstMethodSettings extends Settings {
        private final ProgressIndicator indicator;
        private final AtomicInteger methodFilterCalls = new AtomicInteger();
        private final AtomicInteger methodNodeNameLookups = new AtomicInteger();

        private CancelAfterFirstMethodSettings(ProgressIndicator indicator) {
            this.indicator = indicator;
            setNameUseSwagger(false);
            setNameUseCommentTag(false);
            setNameMethodComment(false);
            setDescUseSwagger3(false);
            setDescUseSwagger(false);
        }

        @Override
        public Set<String> getContainMethodAnnotationName() {
            methodFilterCalls.incrementAndGet();
            return super.getContainMethodAnnotationName();
        }

        @Override
        public Boolean getNameUseSwagger3() {
            if (methodNodeNameLookups.incrementAndGet() == 1) {
                indicator.cancel();
            }
            return false;
        }
    }
}
