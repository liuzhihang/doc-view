package com.liuzhihang.doc.view.tool;

import com.intellij.ide.util.treeView.NodeRenderer;
import com.intellij.ui.JBColor;
import com.intellij.util.ui.ImageUtil;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Objects;

public class CustomTreeCellRenderer extends NodeRenderer {

    private static final JBColor Level1 = new JBColor(new Color(92, 184, 92), new Color(92, 184, 92));
    private static final JBColor Level2 = new JBColor(new Color(240, 173, 78), new Color(240, 173, 78));
    private static final JBColor Level3 = new JBColor(new Color(217, 83, 79), new Color(217, 83, 79));

    public CustomTreeCellRenderer() {
        loaColor();
    }

    public static void loaColor() {
    }

    public static BufferedImage getResourceBufferedImage(String filePath) {
        if (CustomTreeCellRenderer.class.getClassLoader().getResourceAsStream(filePath) != null) {
            try {
                return ImageIO.read(Objects.requireNonNull(CustomTreeCellRenderer.class.getClassLoader().getResourceAsStream(filePath)));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return ImageUtil.createImage(10, 10, 1);
    }

    @SuppressWarnings("unchecked")
    @Override
    public void customizeCellRenderer(JTree tree, Object value, boolean selected, boolean expanded, boolean leaf, int row, boolean hasFocus) {

        super.customizeCellRenderer(tree, value, selected, expanded, leaf, row, hasFocus);

        DefaultMutableTreeNode node = (DefaultMutableTreeNode) value;
//        Question question = (Question) node.getUserObject();
//
//        if (question.getLevel() == null) {
//
//        } else if (question.getLevel() == 1) {
//            setForeground(Level1);
//        } else if (question.getLevel() == 2) {
//            setForeground(Level2);
//        } else if (question.getLevel() == 3) {
//            setForeground(Level3);
//        }
    }
}