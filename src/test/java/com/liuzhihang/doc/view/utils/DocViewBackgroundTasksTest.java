package com.liuzhihang.doc.view.utils;

import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.progress.EmptyProgressIndicator;
import com.intellij.openapi.project.Project;
import com.intellij.testFramework.PlatformTestUtil;
import com.intellij.testFramework.fixtures.LightJavaCodeInsightFixtureTestCase;

import java.lang.reflect.Proxy;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public final class DocViewBackgroundTasksTest extends LightJavaCodeInsightFixtureTestCase {

    public void testQueuedReadResultIsDroppedWhenCanceledBeforeEdtDelivery() throws Exception {
        Application application = ApplicationManager.getApplication();
        EmptyProgressIndicator indicator = new EmptyProgressIndicator();
        AtomicBoolean delivered = new AtomicBoolean();
        CountDownLatch eventQueueTail = new CountDownLatch(1);
        application.invokeAndWait(() -> {
            DocViewBackgroundTasks.invokeLaterIfNotCanceled(getProject(), indicator, () -> delivered.set(true));
            indicator.cancel();
            application.invokeLater(eventQueueTail::countDown);
        });

        if (application.isDispatchThread()) {
            PlatformTestUtil.dispatchAllEventsInIdeEventQueue();
        } else {
            assertTrue("The queued delivery and following event should complete",
                    eventQueueTail.await(5, TimeUnit.SECONDS));
        }
        assertFalse("Cancellation after queueing must prevent the read result callback", delivered.get());
    }

    public void testQueuedReadResultIsDroppedWhenProjectDisposesBeforeEdtDelivery() throws Exception {
        Application application = ApplicationManager.getApplication();
        EmptyProgressIndicator indicator = new EmptyProgressIndicator();
        AtomicBoolean disposed = new AtomicBoolean();
        AtomicBoolean delivered = new AtomicBoolean();
        CountDownLatch eventQueueTail = new CountDownLatch(1);
        Project queuedProject = (Project) Proxy.newProxyInstance(
                Project.class.getClassLoader(), new Class<?>[]{Project.class}, (proxy, method, args) -> {
                    return switch (method.getName()) {
                        case "isDisposed" -> disposed.get();
                        case "toString" -> "queued-test-project";
                        case "hashCode" -> System.identityHashCode(proxy);
                        case "equals" -> proxy == args[0];
                        default -> throw new UnsupportedOperationException(method.toString());
                    };
                });

        application.invokeAndWait(() -> {
            DocViewBackgroundTasks.invokeLaterIfNotCanceled(queuedProject, indicator, () -> delivered.set(true));
            disposed.set(true);
        });
        application.invokeLater(eventQueueTail::countDown);

        if (application.isDispatchThread()) {
            PlatformTestUtil.dispatchAllEventsInIdeEventQueue();
        } else {
            assertTrue("The queued delivery and following event should complete",
                    eventQueueTail.await(5, TimeUnit.SECONDS));
        }
        assertFalse("Project disposal after queueing must prevent the result callback", delivered.get());
    }
}
