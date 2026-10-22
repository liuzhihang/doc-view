package com.liuzhihang.doc.view.action.toolbar.window.catalog;

import com.intellij.openapi.actionSystem.ActionPlaces;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.testFramework.DumbModeTestUtils;
import com.intellij.testFramework.fixtures.LightJavaCodeInsightFixtureTestCase;
import com.intellij.ui.treeStructure.SimpleNode;
import com.intellij.ui.treeStructure.SimpleTree;
import com.liuzhihang.doc.view.data.DocViewDataKeys;

import java.util.List;

public final class CatalogUploadActionTest extends LightJavaCodeInsightFixtureTestCase {

    public void testPlatformActionsAreEnabledWithCatalogTreeAndWithoutEditorContext() {
        SimpleTree tree = new SelectionAccessFailingTree();

        for (AnAction action : platformActions()) {
            AnActionEvent event = event(action, getProject(), tree);
            assertNull("Tool-window upload context must not require an editor", event.getData(CommonDataKeys.EDITOR));
            assertNull("Tool-window upload context must not require a PSI file", event.getData(CommonDataKeys.PSI_FILE));

            action.update(event);

            assertSame("Catalog upload updates must remain on BGT", ActionUpdateThread.BGT,
                    action.getActionUpdateThread());
            assertTrue(action.getClass().getSimpleName() + " should be enabled in the catalog context",
                    event.getPresentation().isEnabled());
            assertTrue(action.getClass().getSimpleName() + " should be visible in the catalog context",
                    event.getPresentation().isVisible());
        }
    }

    public void testPlatformActionsAreUnavailableWithoutProjectOrCatalogTree() {
        SimpleTree tree = new SelectionAccessFailingTree();

        for (AnAction action : platformActions()) {
            assertUnavailable(action, event(action, null, tree));
            assertUnavailable(action, event(action, getProject(), null));
        }
    }

    public void testPlatformActionsAreUnavailableDuringDumbMode() {
        SimpleTree tree = new SelectionAccessFailingTree();
        List<AnAction> actions = platformActions();

        DumbModeTestUtils.runInDumbModeSynchronously(getProject(), () -> {
            assertTrue("The fixture must actually be in Dumb Mode", DumbService.isDumb(getProject()));
            for (AnAction action : actions) {
                assertUnavailable(action, event(action, getProject(), tree));
            }
        });
    }

    private void assertUnavailable(AnAction action, AnActionEvent event) {
        action.update(event);
        assertFalse(action.getClass().getSimpleName() + " should be disabled for an invalid catalog context",
                event.getPresentation().isEnabled());
        assertFalse(action.getClass().getSimpleName() + " should be hidden for an invalid catalog context",
                event.getPresentation().isVisible());
    }

    private AnActionEvent event(AnAction action, Project project, SimpleTree tree) {
        DataContext context = dataId -> {
            if (CommonDataKeys.PROJECT.is(dataId)) {
                return project;
            }
            if (DocViewDataKeys.WINDOW_CATALOG_TREE.is(dataId)) {
                return tree;
            }
            return null;
        };
        return AnActionEvent.createFromAnAction(action, null, ActionPlaces.TOOLWINDOW_CONTENT, context);
    }

    private List<AnAction> platformActions() {
        return List.of(
                new CatalogUploadYApiAction(),
                new CatalogUploadShowDocAction(),
                new CatalogUploadYuQueAction()
        );
    }

    private static final class SelectionAccessFailingTree extends SimpleTree {
        @Override
        public SimpleNode getSelectedNode() {
            throw new AssertionError("Action update must not read Swing tree selection");
        }
    }
}
