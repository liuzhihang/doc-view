package com.liuzhihang.doc.view.contract;

import com.intellij.openapi.actionSystem.ActionGroup;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.testFramework.LightPlatformTestCase;
import com.liuzhihang.doc.view.DocViewBundle;

public final class PluginActionLocalizationTest extends LightPlatformTestCase {

    public void testActionSystemUsesBundleConventionForWelcomeAndUploadEntries() {
        ActionManager actionManager = ActionManager.getInstance();

        assertPresentation(actionManager, "DocView.Welcome",
                "action.DocView.Welcome.text", "action.DocView.Welcome.description");
        assertPresentation(actionManager, "liuzhihang.doc.upload",
                "group.liuzhihang.doc.upload.text", null);
        assertPresentation(actionManager, "liuzhihang.doc.tool.window.catalog.action.upload",
                "group.liuzhihang.doc.tool.window.catalog.action.upload.text",
                "group.liuzhihang.doc.tool.window.catalog.action.upload.description");
        assertPresentation(actionManager, "liuzhihang.doc.tool.window.catalog.action.upload.yapi",
                "action.liuzhihang.doc.tool.window.catalog.action.upload.yapi.text",
                "action.liuzhihang.doc.tool.window.catalog.action.upload.yapi.description");
        assertPresentation(actionManager, "liuzhihang.doc.tool.window.catalog.action.upload.showdoc",
                "action.liuzhihang.doc.tool.window.catalog.action.upload.showdoc.text",
                "action.liuzhihang.doc.tool.window.catalog.action.upload.showdoc.description");
        assertPresentation(actionManager, "liuzhihang.doc.tool.window.catalog.action.upload.yuque",
                "action.liuzhihang.doc.tool.window.catalog.action.upload.yuque.text",
                "action.liuzhihang.doc.tool.window.catalog.action.upload.yuque.description");
    }

    private static void assertPresentation(ActionManager actionManager, String actionId,
                                          String textKey, String descriptionKey) {
        AnAction action = actionManager.getAction(actionId);
        assertNotNull("The plugin.xml action must be registered with ActionManager: " + actionId, action);
        if (textKey.startsWith("group.")) {
            assertTrue("The registered ActionSystem entry must remain a group: " + actionId,
                    action instanceof ActionGroup);
        }

        Presentation presentation = action.getTemplatePresentation();
        assertEquals("ActionManager must resolve the convention text key for " + actionId,
                DocViewBundle.message(textKey), presentation.getText());
        if (descriptionKey != null) {
            assertEquals("ActionManager must resolve the convention description key for " + actionId,
                    DocViewBundle.message(descriptionKey), presentation.getDescription());
        }
    }
}
