package com.liuzhihang.doc.view.service;

import com.intellij.testFramework.EdtTestUtil;
import com.intellij.testFramework.PlatformTestUtil;
import com.intellij.testFramework.ServiceContainerUtil;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.notification.Notification;
import com.intellij.notification.Notifications;
import com.intellij.credentialStore.CredentialAttributes;
import com.liuzhihang.doc.view.service.impl.YApiServiceImpl;
import com.liuzhihang.doc.view.service.impl.YuQueServiceImpl;
import com.liuzhihang.doc.view.service.impl.ShowDocServiceImpl;
import java.util.List;
import com.liuzhihang.doc.view.integration.impl.YuQueFacadeServiceImpl;
import com.liuzhihang.doc.view.integration.dto.YuQueResponse;
import com.liuzhihang.doc.view.integration.dto.YuQueCreate;
import com.liuzhihang.doc.view.integration.dto.YuQueUpdate;
import java.util.concurrent.atomic.AtomicInteger;
import com.intellij.testFramework.fixtures.LightJavaCodeInsightFixtureTestCase;
import com.liuzhihang.doc.view.dto.DocView;
import com.liuzhihang.doc.view.enums.UploadPlatform;

import java.awt.EventQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public final class DocViewUploadServiceTest extends LightJavaCodeInsightFixtureTestCase {

    public void testUploadPlatformRoutingUsesStableIdentifiersInsteadOfLocalizedLabels() {
        assertEquals(List.of(UploadPlatform.values()), DocViewUploadService.UPLOAD_OPTIONS);
        assertEquals("yapi", UploadPlatform.YAPI.getId());
        assertEquals("showdoc", UploadPlatform.SHOW_DOC.getId());
        assertEquals("yuque", UploadPlatform.YUQUE.getId());

        assertSame(ApplicationManager.getApplication().getService(YApiServiceImpl.class),
                DocViewUploadService.getInstance(UploadPlatform.YAPI));
        assertSame(ApplicationManager.getApplication().getService(ShowDocServiceImpl.class),
                DocViewUploadService.getInstance(UploadPlatform.SHOW_DOC));
        assertSame(ApplicationManager.getApplication().getService(YuQueServiceImpl.class),
                DocViewUploadService.getInstance(UploadPlatform.YUQUE));

        assertFalse("Upload dispatch must not depend on a localized display label", java.util.Arrays.stream(
                        DocViewUploadService.class.getDeclaredMethods())
                .anyMatch(method -> method.getName().equals("getInstance")
                        && java.util.Arrays.equals(method.getParameterTypes(), new Class<?>[]{String.class})));
    }

    public void testSingleUploadChecksAndUploadsInBackground() throws Exception {
        FakeUploadService service = new FakeUploadService(true);
        EdtTestUtil.runInEdtAndWait(() -> service.upload(getProject(), new DocView("fake")));

        assertTrue("settings check should run", service.checked.await(10, TimeUnit.SECONDS));
        assertTrue("upload should run", service.uploaded.await(10, TimeUnit.SECONDS));
        assertTrue(service.checkRanInBackground.get());
        assertTrue(service.uploadRanInBackground.get());
    }

    public void testMissingSettingsStopsBeforeUpload() throws Exception {
        FakeUploadService service = new FakeUploadService(false);
        EdtTestUtil.runInEdtAndWait(() -> service.upload(getProject(), new DocView("fake")));

        assertTrue("settings check should run", service.checked.await(10, TimeUnit.SECONDS));
        assertEquals(1L, service.uploaded.getCount());
        assertTrue(service.checkRanInBackground.get());
    }

    public void testBatchCancellationStopsRemainingItemsWithoutNotification() throws Exception {
        AtomicInteger notifications = trackNotifications();
        AtomicInteger uploads = new AtomicInteger();
        CountDownLatch first = new CountDownLatch(1);
        DocViewUploadService service = new DocViewUploadService() {
            @Override public boolean checkSettings(com.intellij.openapi.project.Project project) { return true; }
            @Override public void doUpload(com.intellij.openapi.project.Project project, DocView document) {
                uploads.incrementAndGet();
                ProgressManager.getInstance().getProgressIndicator().cancel();
                first.countDown();
            }
        };
        EdtTestUtil.runInEdtAndWait(() -> service.upload(getProject(), List.of(new DocView("one"), new DocView("two"))));
        assertTrue(first.await(10, TimeUnit.SECONDS));
        PlatformTestUtil.waitForAllBackgroundActivityToCalmDown();
        EdtTestUtil.runInEdtAndWait(PlatformTestUtil::dispatchAllInvocationEventsInIdeEventQueue);
        assertEquals(1, uploads.get());
        assertEquals(0, notifications.get());
    }

    public void testPlatformServicesPropagateCancellationWithoutErrorNotification() throws Exception {
        AtomicInteger notifications = trackNotifications();
        AtomicInteger reads = new AtomicInteger();
        IntegrationCredentialService credentials = new IntegrationCredentialService(getProject(),
                new IntegrationCredentialService.SecretStore() {
                    @Override public String get(CredentialAttributes attributes) {
                        reads.incrementAndGet();
                        throw new ProcessCanceledException();
                    }
                    @Override public void set(CredentialAttributes attributes, String value) {
                        throw new AssertionError("An upload must not write credentials");
                    }
                });
        ServiceContainerUtil.replaceService(getProject(), IntegrationCredentialService.class, credentials, getTestRootDisposable());
        for (DocViewUploadService service : List.of(new YApiServiceImpl(), new ShowDocServiceImpl(), new YuQueServiceImpl())) {
            assertTrue(ApplicationManager.getApplication().executeOnPooledThread(() -> {
                try { service.doUpload(getProject(), new DocView("synthetic")); return false; }
                catch (ProcessCanceledException expected) { return true; }
            }).get(10, TimeUnit.SECONDS));
        }
        EdtTestUtil.runInEdtAndWait(PlatformTestUtil::dispatchAllInvocationEventsInIdeEventQueue);
        assertEquals(3, reads.get());
        assertEquals(0, notifications.get());
    }

    public void testYuQueQueryFailureNeverCreatesOrUpdatesDocument() throws Exception {
        AtomicInteger notifications = trackNotifications();
        AtomicInteger queries = new AtomicInteger();
        AtomicInteger writes = new AtomicInteger();
        IntegrationCredentialService credentials = new IntegrationCredentialService(getProject(),
                new IntegrationCredentialService.SecretStore() {
                    @Override public String get(CredentialAttributes attributes) { return "synthetic-token"; }
                    @Override public void set(CredentialAttributes attributes, String value) { throw new AssertionError(); }
                });
        ServiceContainerUtil.replaceService(getProject(), IntegrationCredentialService.class, credentials, getTestRootDisposable());
        ServiceContainerUtil.replaceService(ApplicationManager.getApplication(), YuQueFacadeServiceImpl.class,
                new YuQueFacadeServiceImpl() {
                    @Override public YuQueResponse getDoc(String url, String token, String namespace, String slug) throws Exception {
                        queries.incrementAndGet();
                        throw new Exception("Synthetic query failure");
                    }
                    @Override public YuQueResponse create(String url, String token, String namespace, YuQueCreate input) {
                        writes.incrementAndGet(); throw new AssertionError("Query failure must stop creation");
                    }
                    @Override public YuQueResponse update(String url, String token, String namespace, Long id, YuQueUpdate input) {
                        writes.incrementAndGet(); throw new AssertionError("Query failure must stop update");
                    }
                }, getTestRootDisposable());
        DocView document = new DocView("synthetic"); document.setPath("/test");
        ApplicationManager.getApplication().executeOnPooledThread(() ->
                new YuQueServiceImpl().doUpload(getProject(), document)).get(10, TimeUnit.SECONDS);
        EdtTestUtil.runInEdtAndWait(PlatformTestUtil::dispatchAllInvocationEventsInIdeEventQueue);
        assertEquals(1, queries.get());
        assertEquals(0, writes.get());
        assertTrue("The failed upload should report a platform error", notifications.get() > 0);
    }

    private AtomicInteger trackNotifications() {
        AtomicInteger count = new AtomicInteger();
        Notifications listener = new Notifications() {
            @Override public void notify(Notification notification) {
                if ("doc-view.NotificationGroup".equals(notification.getGroupId())) count.incrementAndGet();
            }
        };
        ApplicationManager.getApplication().getMessageBus().connect(getTestRootDisposable()).subscribe(Notifications.TOPIC, listener);
        getProject().getMessageBus().connect(getTestRootDisposable()).subscribe(Notifications.TOPIC, listener);
        return count;
    }

    private static final class FakeUploadService implements DocViewUploadService {
        private final boolean configured;
        private final CountDownLatch checked = new CountDownLatch(1);
        private final CountDownLatch uploaded = new CountDownLatch(1);
        private final AtomicBoolean checkRanInBackground = new AtomicBoolean();
        private final AtomicBoolean uploadRanInBackground = new AtomicBoolean();

        private FakeUploadService(boolean configured) {
            this.configured = configured;
        }

        @Override
        public boolean checkSettings(com.intellij.openapi.project.Project project) {
            checkRanInBackground.set(!EventQueue.isDispatchThread());
            checked.countDown();
            return configured;
        }

        @Override
        public void doUpload(com.intellij.openapi.project.Project project, DocView docView) {
            uploadRanInBackground.set(!EventQueue.isDispatchThread());
            uploaded.countDown();
        }
    }
}
