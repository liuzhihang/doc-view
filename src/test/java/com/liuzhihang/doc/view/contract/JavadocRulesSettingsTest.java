package com.liuzhihang.doc.view.contract;

import com.intellij.testFramework.fixtures.LightJavaCodeInsightFixtureTestCase;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.util.xmlb.XmlSerializer;
import com.liuzhihang.doc.view.config.Settings;
import com.liuzhihang.doc.view.config.GenerationRulesConfigurable;
import javax.swing.*;
import java.awt.*;

public final class JavadocRulesSettingsTest extends LightJavaCodeInsightFixtureTestCase {
    public void testApplyResetCancelPersistenceAndInvalidInputAreAtomic() throws Exception {
        Settings settings = Settings.getInstance(getProject());
        settings.loadState(new Settings());
        GenerationRulesConfigurable configurable = new GenerationRulesConfigurable(getProject());
        try {
            JComponent root = configurable.createComponent();
            configurable.reset();
            JTextField limit = (JTextField) find(root, "commentTitleMaxLengthField");
            JCheckBox first = (JCheckBox) find(root, "nameCommentFirstLineCheckBox");
            JCheckBox desc = (JCheckBox) find(root, "descCommentBodyCheckBox");
            assertNotNull("A configurable title limit must be visible", limit);
            assertNotNull(first);
            assertNotNull(desc);
            assertEquals("80", limit.getText());
            assertTrue(first.isSelected());
            assertTrue(desc.isSelected());
            limit.setText("40"); first.setSelected(false); desc.setSelected(false);
            assertTrue(configurable.isModified());
            configurable.reset();
            assertEquals("80", limit.getText());
            assertTrue(first.isSelected());
            limit.setText("40"); first.setSelected(false); desc.setSelected(false);
            configurable.apply();
            assertFalse(configurable.isModified());
            Settings restored = XmlSerializer.deserialize(XmlSerializer.serialize(settings), Settings.class);
            settings.loadState(restored);
            configurable.reset();
            assertEquals("40", limit.getText());
            assertFalse(first.isSelected());
            for (String invalid : new String[]{"0", "201", "abc", "1.5", ""}) {
                limit.setText(invalid); first.setSelected(true);
                try { configurable.apply(); fail("Invalid limit must reject apply: " + invalid); }
                catch (ConfigurationException expected) { assertTrue(expected.getMessage().contains("200")); }
                configurable.reset();
                assertEquals("40", limit.getText());
                assertFalse(first.isSelected());
            }
            limit.setText("99");
            configurable.disposeUIResources();
            root = configurable.createComponent(); configurable.reset();
            assertEquals("40", ((JTextField) find(root, "commentTitleMaxLengthField")).getText());
        } finally { configurable.disposeUIResources(); settings.loadState(new Settings()); }
    }
    private static Component find(Container root, String name) {
        for (Component child : root.getComponents()) {
            if (name.equals(child.getName())) return child;
            if (child instanceof Container c) { Component found = find(c, name); if (found != null) return found; }
        }
        return null;
    }
}
