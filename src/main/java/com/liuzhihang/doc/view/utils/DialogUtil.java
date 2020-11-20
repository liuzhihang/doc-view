package com.liuzhihang.doc.view.utils;

import com.intellij.openapi.ui.DialogWrapper;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;

/**
 * 弹出式提示工具类
 *
 * @author lvgorice@gmail.com
 * @version 1.0
 * @date 2020-11-20
 */
public class DialogUtil extends DialogWrapper {

    private final String message;

    private DialogUtil(String title, String message) {
        super(true); // use current window as parent
        this.message = message;
        init();
        setTitle(title);
    }

    @Nullable
    @Override
    protected JComponent createCenterPanel() {
        JPanel dialogPanel = new JPanel(new BorderLayout());

        JLabel label = new JLabel(message);
        dialogPanel.add(label, BorderLayout.CENTER);

        return dialogPanel;
    }

    public static boolean confirm(String title, String message) {
        return new DialogUtil(title, message).showAndGet();
    }
}