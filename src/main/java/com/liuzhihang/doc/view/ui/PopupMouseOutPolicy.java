package com.liuzhihang.doc.view.ui;

import javax.swing.SwingUtilities;
import java.awt.Component;
import java.awt.event.MouseEvent;

final class PopupMouseOutPolicy {

    private PopupMouseOutPolicy() {
    }

    static boolean shouldCancel(MouseEvent event, Component content, boolean pinned) {
        if (event.getID() != MouseEvent.MOUSE_PRESSED || pinned) {
            return false;
        }

        // Popup screen-coordinate geometry can disagree with the event source; internal clicks stay with the control.
        Component source = event.getComponent();
        return source != content && !SwingUtilities.isDescendingFrom(source, content);
    }
}
