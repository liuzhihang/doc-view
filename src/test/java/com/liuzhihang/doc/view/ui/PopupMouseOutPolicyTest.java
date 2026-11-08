package com.liuzhihang.doc.view.ui;

import junit.framework.TestCase;

import javax.swing.JPanel;
import java.awt.Component;
import java.awt.event.MouseEvent;

public final class PopupMouseOutPolicyTest extends TestCase {

    public void testPressFromContentOrDescendantDoesNotCancelRegardlessOfAbsoluteCoordinates() {
        JPanel content = new JPanel();
        JPanel child = new JPanel();
        content.add(child);

        assertFalse(PopupMouseOutPolicy.shouldCancel(mouseEvent(content, MouseEvent.MOUSE_PRESSED, -5000, -5000),
                content, false));
        assertFalse(PopupMouseOutPolicy.shouldCancel(mouseEvent(child, MouseEvent.MOUSE_PRESSED, -5000, -5000),
                content, false));
    }

    public void testOutsidePressCancelsOnlyWhenUnpinned() {
        JPanel content = new JPanel();
        JPanel outside = new JPanel();
        MouseEvent event = mouseEvent(outside, MouseEvent.MOUSE_PRESSED, 10, 10);

        assertTrue(PopupMouseOutPolicy.shouldCancel(event, content, false));
        assertFalse(PopupMouseOutPolicy.shouldCancel(event, content, true));
    }

    public void testOtherMouseEventsDoNotCancel() {
        JPanel content = new JPanel();
        JPanel outside = new JPanel();

        assertFalse(PopupMouseOutPolicy.shouldCancel(mouseEvent(outside, MouseEvent.MOUSE_RELEASED, 10, 10),
                content, false));
        assertFalse(PopupMouseOutPolicy.shouldCancel(mouseEvent(outside, MouseEvent.MOUSE_CLICKED, 10, 10),
                content, false));
        assertFalse(PopupMouseOutPolicy.shouldCancel(mouseEvent(outside, MouseEvent.MOUSE_MOVED, 10, 10),
                content, false));
        assertFalse(PopupMouseOutPolicy.shouldCancel(mouseEvent(outside, MouseEvent.MOUSE_ENTERED, 10, 10),
                content, false));
    }

    private static MouseEvent mouseEvent(Component source, int id, int absoluteX, int absoluteY) {
        int button = id == MouseEvent.MOUSE_MOVED || id == MouseEvent.MOUSE_ENTERED
                ? MouseEvent.NOBUTTON
                : MouseEvent.BUTTON1;
        return new MouseEvent(source, id, System.currentTimeMillis(), 0,
                1, 2, absoluteX, absoluteY, 1, false, button);
    }
}
