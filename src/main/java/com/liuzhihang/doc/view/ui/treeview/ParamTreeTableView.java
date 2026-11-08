package com.liuzhihang.doc.view.ui.treeview;

import com.intellij.ide.util.treeView.NodeRenderer;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.openapi.util.NlsContexts;
import com.intellij.psi.PsiElement;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.components.JBTextArea;
import com.intellij.ui.dualView.TreeTableView;
import com.intellij.ui.treeStructure.treetable.ListTreeTableModelOnColumns;
import com.intellij.ui.treeStructure.treetable.TreeColumnInfo;
import com.intellij.util.ui.ColumnInfo;
import com.intellij.util.ui.JBUI;
import com.liuzhihang.doc.view.DocViewBundle;
import com.liuzhihang.doc.view.dto.DocViewParamData;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.table.TableCellEditor;
import javax.swing.tree.DefaultMutableTreeNode;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.HashMap;
import java.util.Map;

/**
 * @author liuzhihang
 * @date 2021/10/20 19:51
 */
@Getter
public class ParamTreeTableView extends TreeTableView {

    // {"参数名", "类型", "必选", "描述"}
    public static final ColumnInfo[] COLUMN_INFOS = new ColumnInfo[]{
            new TreeColumnInfo(DocViewBundle.message("param.column.name")) {

                @NlsContexts.Tooltip
                @Nullable
                @Override
                public String getTooltipText() {
                    return DocViewBundle.message("param.column.readonly");
                }
            },
            new ColumnInfo(DocViewBundle.message("param.column.type")) {
                @Nullable
                @Override
                public Object valueOf(Object o) {
                    if (o instanceof DefaultMutableTreeNode node) {
                        if (node.getUserObject() instanceof DocViewParamData paramData) {
                            return paramData.getType();
                        }

                    }
                    return o;
                }

                @NlsContexts.Tooltip
                @Nullable
                @Override
                public String getTooltipText() {
                    return DocViewBundle.message("param.column.readonly");
                }
            },
            new ColumnInfo(DocViewBundle.message("param.column.required")) {
                @Nullable
                @Override
                public Object valueOf(Object o) {
                    if (o instanceof DefaultMutableTreeNode) {
                        DefaultMutableTreeNode node = (DefaultMutableTreeNode) o;
                        if (node.getUserObject() instanceof DocViewParamData) {
                            DocViewParamData paramData = (DocViewParamData) node.getUserObject();
                            return paramData.getRequired();
                        }

                    }
                    return o;
                }

                @Override
                public int getWidth(JTable table) {
                    return 80;
                }
            },
            new ColumnInfo(DocViewBundle.message("param.column.description")) {
                @Nullable
                @Override
                public Object valueOf(Object o) {
                    if (o instanceof DefaultMutableTreeNode node) {
                        if (node.getUserObject() instanceof DocViewParamData paramData) {
                            return paramData.getDesc();
                        }

                    }
                    return o;
                }
            }
    };

    /**
     * 修改集合
     */
    private Map<PsiElement, DocViewParamData> modifiedMap = new HashMap<>();
    private final TableCellEditor descriptionCellEditor = new DescriptionCellEditor();

    public ParamTreeTableView(ListTreeTableModelOnColumns treeTableModel) {
        super(treeTableModel);
        // ColoredTreeCellRenderer retains the last tree it renders; keep one instance per table.
        super.setTreeCellRenderer(createNodeRenderer());
    }

    private static NodeRenderer createNodeRenderer() {
        return new NodeRenderer() {
            @Override
            public void customizeCellRenderer(@NotNull JTree tree, Object value, boolean selected,
                                              boolean expanded, boolean leaf, int row, boolean hasFocus) {
                if (value instanceof DefaultMutableTreeNode node
                        && node.getUserObject() instanceof DocViewParamData paramData) {
                    append(paramData.getName() == null ? "" : paramData.getName());
                }
            }
        };
    }

    @Override
    public TableCellEditor getCellEditor(int row, int column) {

        if (column == 2) {
            return new DefaultCellEditor(new JComboBox<>(new Boolean[]{true, false}));
        }
        if (column == 3) {
            return descriptionCellEditor;
        }
        return super.getCellEditor(row, column);
    }

    private static final class DescriptionCellEditor extends AbstractCellEditor implements TableCellEditor {
        private static final String INSERT_LINE_BREAK = "docView.description.insertLineBreak";
        private static final String COMMIT_EDITING = "docView.description.commitEditing";
        private static final String CANCEL_EDITING = "docView.description.cancelEditing";
        private static final String NO_LOCAL_ACTION = "none";

        private final JBTextArea textArea = new JBTextArea();
        private final JBScrollPane scrollPane = new JBScrollPane(textArea,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

        private DescriptionCellEditor() {
            textArea.setLineWrap(true);
            textArea.setWrapStyleWord(true);
            textArea.setBorder(JBUI.Borders.empty(2, 4));
            scrollPane.setBorder(JBUI.Borders.empty());

            InputMap inputMap = textArea.getInputMap(JComponent.WHEN_FOCUSED);
            ActionMap actionMap = textArea.getActionMap();
            inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), INSERT_LINE_BREAK);
            int primaryModifier = SystemInfo.isMac ? InputEvent.META_DOWN_MASK : InputEvent.CTRL_DOWN_MASK;
            inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, primaryModifier), COMMIT_EDITING);
            inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), CANCEL_EDITING);
            // Leave Tab navigation to JTable's ancestor binding; Ctrl+Tab remains the popup's focus-transfer key.
            inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_TAB, 0), NO_LOCAL_ACTION);
            inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_TAB, InputEvent.SHIFT_DOWN_MASK), NO_LOCAL_ACTION);

            actionMap.put(INSERT_LINE_BREAK, new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent event) {
                    textArea.replaceSelection("\n");
                }
            });
            actionMap.put(COMMIT_EDITING, new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent event) {
                    stopCellEditing();
                }
            });
            actionMap.put(CANCEL_EDITING, new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent event) {
                    cancelCellEditing();
                }
            });
        }

        @Override
        public Component getTableCellEditorComponent(JTable table, Object value, boolean selected, int row, int column) {
            textArea.setFont(table.getFont());
            textArea.setForeground(selected ? table.getSelectionForeground() : table.getForeground());
            textArea.setBackground(selected ? table.getSelectionBackground() : table.getBackground());
            textArea.setText(value == null ? "" : value.toString());
            textArea.setCaretPosition(textArea.getDocument().getLength());
            return scrollPane;
        }

        @Override
        public Object getCellEditorValue() {
            return textArea.getText();
        }
    }


    @Override
    public Object getValueAt(int row, int column) {
        ListTreeTableModelOnColumns tableModel = (ListTreeTableModelOnColumns) getTableModel();
        DefaultMutableTreeNode node = (DefaultMutableTreeNode) tableModel.getRowValue(row);
        DocViewParamData paramData = (DocViewParamData) node.getUserObject();
        switch (column) {
            case 0:
                return paramData.getName();
            case 1:
                return paramData.getType();
            case 2:
                return paramData.getRequired();
            case 3:
                return paramData.getDesc();
        }
        return "";
    }


    @Override
    public void setValueAt(Object value, int row, int column) {

        ListTreeTableModelOnColumns tableModel = (ListTreeTableModelOnColumns) getTableModel();
        DefaultMutableTreeNode node = (DefaultMutableTreeNode) tableModel.getRowValue(row);
        DocViewParamData paramData = (DocViewParamData) node.getUserObject();

        // "参数名", "类型", "必选", "描述"
        if (column == 2) {
            paramData.setRequired(String.valueOf(value).equalsIgnoreCase("true"));
            modifiedMap.put(paramData.getPsiElement(), paramData);
        } else if (column == 3) {
            paramData.setDesc(String.valueOf(value));
            modifiedMap.put(paramData.getPsiElement(), paramData);
        }

        super.setValueAt(value, row, column);

    }

    @Override
    public boolean isCellEditable(int row, int column) {

        if (column == 2) {
            return true;
        }
        if (column == 3) {
            return true;
        }

        return false;
    }

    @Override
    public int getRowHeight() {
        return 24;
    }


}
