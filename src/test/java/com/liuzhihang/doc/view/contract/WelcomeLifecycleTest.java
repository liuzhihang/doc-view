package com.liuzhihang.doc.view.contract;

import com.intellij.openapi.fileEditor.*;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.testFramework.LightPlatformTestCase;
import com.intellij.testFramework.LightVirtualFile;
import com.intellij.testFramework.PlatformTestUtil;
import com.liuzhihang.doc.view.DocViewBundle;
import com.liuzhihang.doc.view.action.DocViewWelcomeAction;
import com.liuzhihang.doc.view.config.ApplicationSettings;
import com.liuzhihang.doc.view.notification.DocViewStartupNotification;
import com.liuzhihang.doc.view.ui.welcome.DocViewWelcomeService;
import com.liuzhihang.doc.view.ui.welcome.DocViewWelcomeFile;
import java.util.Arrays;

public final class WelcomeLifecycleTest extends LightPlatformTestCase {
    public void testFirstSameUpgradeDowngradeAndDisabledNotification() throws Exception {
        var method = DocViewStartupNotification.class.getDeclaredMethod("startupDisplay", String.class, String.class, boolean.class);
        method.setAccessible(true);
        assertEquals("welcome", method.invoke(null, "", "1.3.14", true));
        assertEquals("none", method.invoke(null, "1.3.14", "1.3.14", true));
        assertEquals("update", method.invoke(null, "1.3.13", "1.3.14", true));
        assertEquals("none", method.invoke(null, "1.3.14", "1.3.13", true));
        assertEquals("none", method.invoke(null, "1.3.13", "1.3.14", false));
        assertEquals("none", method.invoke(null, "", null, true));
    }

    public void testWelcomeActionRefreshesLocalizedTitleAndDescription() {
        DocViewWelcomeAction action = new DocViewWelcomeAction();
        AnActionEvent event = AnActionEvent.createFromAnAction(action, null, "welcome-test", DataContext.EMPTY_CONTEXT);

        action.update(event);

        assertEquals(DocViewBundle.message("welcome.action.title"), event.getPresentation().getText());
        assertEquals(DocViewBundle.message("welcome.action.description"), event.getPresentation().getDescription());
        assertFalse(event.getPresentation().isEnabled());
    }

    public void testProviderOwnsOnlyWelcomeAndDisposesRealEditor() {
        FileEditorProvider provider = provider();
        assertFalse(provider.accept(getProject(), new LightVirtualFile("ordinary.html", "<html>ordinary</html>")));
        VirtualFile file = new DocViewWelcomeFile();
        assertTrue(provider.accept(getProject(), file));
        FileEditor editor = provider.createEditor(getProject(), file);
        try {
            assertSame(file, editor.getFile());
            assertTrue(editor.isValid());
            assertFalse(editor.isModified());
            assertNotNull(editor.getComponent());
        } finally {
            Disposer.dispose(editor);
        }
        assertFalse(editor.isValid());
    }

    public void testReopeningWelcomeReusesProjectVirtualFile() throws Exception {
        DocViewWelcomeService service = DocViewWelcomeService.getInstance(getProject());
        FileEditorManager manager = FileEditorManager.getInstance(getProject());
        service.open("updates");
        VirtualFile first = Arrays.stream(manager.getOpenFiles()).filter(DocViewWelcomeFile.class::isInstance).findFirst().orElseThrow();
        try {
            assertPage(first, "updates");
            service.open("welcome");
            assertEquals(1, Arrays.stream(manager.getOpenFiles()).filter(DocViewWelcomeFile.class::isInstance).count());
            assertSame(first, Arrays.stream(manager.getOpenFiles()).filter(DocViewWelcomeFile.class::isInstance).findFirst().orElseThrow());
            assertPage(first, "welcome");
            manager.closeFile(first);
            service.open("updates");
            assertPage(first, "updates");
        } finally {
            manager.closeFile(first);
        }
    }

    public void testQueuedNavigationCannotMutateSettingsAfterEditorDisposal() throws Exception {
        FileEditor editor = provider().createEditor(getProject(), new DocViewWelcomeFile());
        ApplicationSettings settings = ApplicationSettings.getInstance();
        boolean original = settings.isShowUpdateNotification();
        try {
            var dispatch = editor.getClass().getDeclaredMethod("dispatchNavigation", String.class);
            dispatch.setAccessible(true);
            dispatch.invoke(editor, "docview://action/notifications");
            Disposer.dispose(editor);
            PlatformTestUtil.dispatchAllInvocationEventsInIdeEventQueue();
            assertEquals("A queued browser callback must not mutate settings after disposal", original, settings.isShowUpdateNotification());
        } finally {
            settings.setShowUpdateNotification(original);
            if (!Disposer.isDisposed(editor)) Disposer.dispose(editor);
        }
    }

    public void testUnknownPageDoesNotReplaceCurrentWelcomePage() throws Exception {
        FileEditor editor = provider().createEditor(getProject(), new DocViewWelcomeFile());
        try {
            var show = editor.getClass().getDeclaredMethod("showPage", String.class);
            show.setAccessible(true);
            var current = editor.getClass().getDeclaredField("currentPage");
            current.setAccessible(true);
            assertEquals("welcome", current.get(editor));
            show.invoke(editor, "../../private.md");
            assertEquals("welcome", current.get(editor));
        } finally {
            Disposer.dispose(editor);
        }
    }

    private FileEditorProvider provider() {
        return FileEditorProvider.EP_FILE_EDITOR_PROVIDER.getExtensionList().stream()
                .filter(item -> item.getEditorTypeId().equals("doc-view-welcome")).findFirst().orElseThrow();
    }

    private void assertPage(VirtualFile file, String page) throws Exception {
        // The fixture manager uses text editors; exercise the actual provider separately.
        FileEditor editor = provider().createEditor(getProject(), file);
        try {
            var current = editor.getClass().getDeclaredField("currentPage");
            current.setAccessible(true);
            assertEquals(page, current.get(editor));
        } finally {
            Disposer.dispose(editor);
        }
    }
}
