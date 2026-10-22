package com.liuzhihang.doc.view.contract;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.util.AbstractProgressIndicatorBase;
import com.intellij.openapi.util.Computable;
import com.intellij.testFramework.LightProjectDescriptor;
import com.intellij.testFramework.fixtures.LightJavaCodeInsightFixtureTestCase;
import com.liuzhihang.doc.view.dto.Body;
import com.liuzhihang.doc.view.utils.ParamPsiUtils;
import com.intellij.psi.PsiClass;

import java.util.LinkedList;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/** Mid-recursion cancellation coverage for DTO body traversal. */
public final class GenerationCancellationTraversalTest extends LightJavaCodeInsightFixtureTestCase {

    @Override
    protected LightProjectDescriptor getProjectDescriptor() {
        return JAVA_21;
    }

    public void testDtoTraversalStopsAfterFirstNestedFieldWhenCancelled() throws Exception {
        myFixture.addClass("""
                package contract.cancellation;

                public class ChildDto {
                    private String first;
                    private String second;
                }
                """);
        PsiClass rootClass = myFixture.addClass("""
                package contract.cancellation;

                public class RootDto {
                    private ChildDto child;
                }
                """);

        // This is the low-level mutable parse target, not a DocView returned to callers.
        Body parseTarget = new Body();
        AbstractProgressIndicatorBase indicator = new AbstractProgressIndicatorBase();
        cancelAfterFirstNestedField(parseTarget, indicator);
        Future<Boolean> cancelled = ApplicationManager.getApplication().executeOnPooledThread(() -> {
            try {
                ProgressManager.getInstance().runProcess(() ->
                        ApplicationManager.getApplication().runReadAction((Computable<Void>) () -> {
                            ParamPsiUtils.buildBodyList(rootClass, null, parseTarget);
                            return null;
                        }), indicator);
                return false;
            } catch (ProcessCanceledException expected) {
                return true;
            }
        });

        assertTrue("Cancellation must interrupt recursive field parsing", cancelled.get(10, TimeUnit.SECONDS));
        assertTrue("The first nested field insertion must cancel the active indicator", indicator.isCanceled());
        assertEquals("The root DTO should have only its nested child node", 1, parseTarget.getChildList().size());
        Body child = parseTarget.getChildList().get(0);
        assertEquals("child", child.getName());
        assertEquals("Traversal must stop before parsing the next child field", 1, child.getChildList().size());
        assertEquals("first", child.getChildList().get(0).getName());
    }

    private static void cancelAfterFirstNestedField(Body parseTarget, AbstractProgressIndicatorBase indicator) {
        parseTarget.setChildList(new LinkedList<>() {
            @Override
            public boolean add(Body rootChild) {
                if ("child".equals(rootChild.getName())) {
                    rootChild.setChildList(new LinkedList<>() {
                        @Override
                        public boolean add(Body nestedField) {
                            boolean added = super.add(nestedField);
                            if (added && "first".equals(nestedField.getName())) {
                                indicator.cancel();
                            }
                            return added;
                        }
                    });
                }
                return super.add(rootChild);
            }
        });
    }
}
