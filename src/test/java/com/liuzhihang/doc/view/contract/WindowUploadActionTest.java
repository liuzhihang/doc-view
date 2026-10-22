package com.liuzhihang.doc.view.contract;

import com.intellij.openapi.actionSystem.ActionPlaces;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.DataKey;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.testFramework.DumbModeTestUtils;
import com.intellij.testFramework.fixtures.LightJavaCodeInsightFixtureTestCase;
import com.liuzhihang.doc.view.action.toolbar.window.WindowUploadAction;
import com.liuzhihang.doc.view.data.DocViewDataKeys;
import com.liuzhihang.doc.view.ui.window.RootNode;

import javax.swing.JPanel;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;

public final class WindowUploadActionTest extends LightJavaCodeInsightFixtureTestCase {

    public void testWindowUploadIsEnabledWithoutEditorContextEvenWhenRootIsEmpty() {
        WindowUploadAction action = new WindowUploadAction();
        AnActionEvent event = event(action, getProject(), windowData());

        assertNull("Window upload must not require an editor", event.getData(CommonDataKeys.EDITOR));
        assertNull("Window upload must not require a PSI file", event.getData(CommonDataKeys.PSI_FILE));
        assertSame("Window upload updates must remain on BGT", ActionUpdateThread.BGT,
                action.getActionUpdateThread());

        action.update(event);

        assertTrue("An available Window context enables the action even before there are rows",
                event.getPresentation().isEnabled());
        assertTrue("An available Window context makes the action visible", event.getPresentation().isVisible());
    }

    public void testWindowUploadIsUnavailableWhenRequiredContextIsMissing() {
        WindowUploadAction action = new WindowUploadAction();
        Map<DataKey<?>, Object> data = windowData();

        assertUnavailable(action, event(action, null, data), "missing project");
        assertUnavailable(action, event(action, disposedProject(), data), "disposed project");

        Map<DataKey<?>, Object> missingRoot = new HashMap<>(data);
        missingRoot.remove(DocViewDataKeys.WINDOW_ROOT_NODE);
        assertUnavailable(action, event(action, getProject(), missingRoot), "missing root node");

        Map<DataKey<?>, Object> missingToolbar = new HashMap<>(data);
        missingToolbar.remove(DocViewDataKeys.WINDOW_TOOLBAR);
        assertUnavailable(action, event(action, getProject(), missingToolbar), "missing toolbar");
    }

    public void testWindowUploadIsUnavailableDuringDumbMode() {
        WindowUploadAction action = new WindowUploadAction();
        DumbModeTestUtils.runInDumbModeSynchronously(getProject(), () -> {
            assertTrue("The fixture must actually be in Dumb Mode", DumbService.isDumb(getProject()));
            assertUnavailable(action, event(action, getProject(), windowData()), "Dumb Mode");
        });
    }

    private void assertUnavailable(WindowUploadAction action, AnActionEvent event, String reason) {
        assertSame("Window upload updates must remain on BGT", ActionUpdateThread.BGT,
                action.getActionUpdateThread());
        action.update(event);
        assertFalse("Window upload should be disabled for " + reason, event.getPresentation().isEnabled());
        assertFalse("Window upload should be hidden for " + reason, event.getPresentation().isVisible());
    }

    private AnActionEvent event(WindowUploadAction action, Project project, Map<DataKey<?>, Object> values) {
        DataContext context = dataId -> {
            if (CommonDataKeys.PROJECT.is(dataId)) {
                return project;
            }
            for (Map.Entry<DataKey<?>, Object> entry : values.entrySet()) {
                if (entry.getKey().is(dataId)) {
                    return entry.getValue();
                }
            }
            return null;
        };
        return AnActionEvent.createFromAnAction(action, null, ActionPlaces.TOOLWINDOW_CONTENT, context);
    }

    private Map<DataKey<?>, Object> windowData() {
        Map<DataKey<?>, Object> data = new HashMap<>();
        data.put(DocViewDataKeys.WINDOW_ROOT_NODE, new RootNode());
        data.put(DocViewDataKeys.WINDOW_TOOLBAR, new JPanel());
        return data;
    }

    private Project disposedProject() {
        return (Project) Proxy.newProxyInstance(
                Project.class.getClassLoader(),
                new Class<?>[]{Project.class},
                (proxy, method, args) -> {
                    if ("isDisposed".equals(method.getName())) {
                        return true;
                    }
                    if ("toString".equals(method.getName())) {
                        return "DisposedProject";
                    }
                    if ("hashCode".equals(method.getName())) {
                        return System.identityHashCode(proxy);
                    }
                    if ("equals".equals(method.getName())) {
                        return proxy == args[0];
                    }
                    return null;
                });
    }
}
