package com.liuzhihang.doc.view.contract;

import com.intellij.openapi.actionSystem.*;
import com.liuzhihang.doc.view.DocViewBundle;
import javax.swing.*;
import java.awt.*;

final class TemplateActionTestSupport {
    static Component find(Container root, String name) {
        for (Component child : root.getComponents()) {
            if (name.equals(child.getName())) return child;
            if (child instanceof Container nested) {
                Component result = find(nested, name);
                if (result != null) return result;
            }
        }
        return null;
    }
    static ActionToolbar toolbar(Container root, String framework) {
        JComponent component = (JComponent) find(root, "templateActions" + framework);
        if (component == null) throw new AssertionError("Missing compact template toolbar: " + framework);
        return (ActionToolbar) component;
    }
    static AnAction action(Container root, String key) {
        AnAction found = findAction(toolbar(root, "Spring").getActionGroup(), DocViewBundle.message(key));
        if (found == null) throw new AssertionError("Missing template action: " + key);
        return found;
    }
    private static AnAction findAction(AnAction action, String text) {
        if (text.equals(action.getTemplatePresentation().getText())) return action;
        if (action instanceof ActionGroup group) for (AnAction child : group.getChildren(null)) {
            AnAction found = findAction(child, text);
            if (found != null) return found;
        }
        return null;
    }
    static void click(Container root, String key) {
        AnAction action = action(root, key);
        action.actionPerformed(AnActionEvent.createEvent(action, DataContext.EMPTY_CONTEXT, action.getTemplatePresentation().clone(), ActionPlaces.UNKNOWN, ActionUiKind.TOOLBAR, null));
    }
    static void restoreDefaults(Container root) {
        var previous = com.intellij.openapi.ui.TestDialogManager.setTestDialog(message -> com.intellij.openapi.ui.Messages.OK);
        try { click(root, "template.restore.defaults"); }
        finally { com.intellij.openapi.ui.TestDialogManager.setTestDialog(previous); }
    }
    static Presentation presentation(Container root, String key) {
        AnAction action = action(root, key);
        AnActionEvent event = AnActionEvent.createEvent(action, DataContext.EMPTY_CONTEXT, action.getTemplatePresentation().clone(), ActionPlaces.UNKNOWN, ActionUiKind.TOOLBAR, null);
        action.update(event);
        return event.getPresentation();
    }
}
