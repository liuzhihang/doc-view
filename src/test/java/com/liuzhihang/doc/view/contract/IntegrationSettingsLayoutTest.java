package com.liuzhihang.doc.view.contract;

import com.intellij.openapi.project.Project;
import com.intellij.testFramework.LightProjectDescriptor;
import com.intellij.testFramework.fixtures.LightJavaCodeInsightFixtureTestCase;
import com.intellij.testFramework.EdtTestUtil;
import com.intellij.ui.components.JBPasswordField;
import com.liuzhihang.doc.view.DocViewBundle;
import com.liuzhihang.doc.view.ui.ShowDocSettingForm;
import com.liuzhihang.doc.view.ui.YApiSettingForm;
import com.liuzhihang.doc.view.ui.YuQueSettingForm;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import java.awt.Component;
import java.awt.Container;
import java.awt.Rectangle;
import java.lang.reflect.Field;

public final class IntegrationSettingsLayoutTest extends LightJavaCodeInsightFixtureTestCase {

    @Override
    protected LightProjectDescriptor getProjectDescriptor() {
        return JAVA_21;
    }

    public void testCredentialClearLabelsFitNarrowSettingsContentWithoutChangingTheirAssociation() throws Exception {
        EdtTestUtil.runInEdtAndWait(() -> {
            Project project = getProject();
            ShowDocSettingForm showDoc = new ShowDocSettingForm(project);
            YApiSettingForm yapi = new YApiSettingForm(project);
            YuQueSettingForm yuque = new YuQueSettingForm(project);
            try {
                for (int width : new int[]{650, 1000}) {
                    assertClearActionFits(showDoc.getRootPanel(),
                            component(showDoc, "apiKeyTextField", JBPasswordField.class),
                            component(showDoc, "clearApiKeyCheckBox", JCheckBox.class), width,
                            "ShowDoc API key");
                    assertClearActionFits(showDoc.getRootPanel(),
                            component(showDoc, "apiTokenTextField", JBPasswordField.class),
                            component(showDoc, "clearApiTokenCheckBox", JCheckBox.class), width,
                            "ShowDoc API token");
                    assertStatusAndGuideFollow(showDoc.getRootPanel(),
                            component(showDoc, "clearApiTokenCheckBox", JCheckBox.class),
                            component(showDoc, "credentialStatusLabel", JLabel.class),
                            component(showDoc, "integrationHelpContainer", JPanel.class), "ShowDoc");
                    assertClearActionFits(yapi.getRootPanel(),
                            component(yapi, "tokenTextField", JBPasswordField.class),
                            component(yapi, "clearTokenCheckBox", JCheckBox.class), width,
                            "YApi token");
                    assertStatusAndGuideFollow(yapi.getRootPanel(),
                            component(yapi, "clearTokenCheckBox", JCheckBox.class),
                            component(yapi, "credentialStatusLabel", JLabel.class),
                            component(yapi, "integrationHelpContainer", JPanel.class), "YApi");
                    assertClearActionFits(yuque.getRootPanel(),
                            component(yuque, "tokenTextField", JBPasswordField.class),
                            component(yuque, "clearTokenCheckBox", JCheckBox.class), width,
                            "YuQue token");
                    assertStatusAndGuideFollow(yuque.getRootPanel(),
                            component(yuque, "clearTokenCheckBox", JCheckBox.class),
                            component(yuque, "credentialStatusLabel", JLabel.class),
                            component(yuque, "integrationHelpContainer", JPanel.class), "YuQue");
                }
            } finally {
                showDoc.dispose();
                yapi.dispose();
                yuque.dispose();
            }
        });
    }

    private void assertClearActionFits(JPanel root, JBPasswordField passwordField, JCheckBox clearAction,
                                      int viewportWidth, String description) {
        root.setSize(viewportWidth, 700);
        layoutRecursively(root);

        Rectangle fieldBounds = SwingUtilities.convertRectangle(passwordField.getParent(),
                passwordField.getBounds(), root);
        Rectangle actionBounds = SwingUtilities.convertRectangle(clearAction.getParent(),
                clearAction.getBounds(), root);

        assertTrue(description + " password field should retain a usable width at " + viewportWidth,
                passwordField.getWidth() >= 120);
        assertTrue(description + " clear action must be on a separate row below its password field",
                actionBounds.y >= fieldBounds.y + fieldBounds.height);
        assertEquals(description + " must retain the complete localized clear label",
                DocViewBundle.message("integration.credentials.clear"), clearAction.getText());
        assertTrue(description + " clear label is clipped by the settings content edge at " + viewportWidth,
                actionBounds.x >= 0 && actionBounds.x + actionBounds.width <= viewportWidth);
        assertTrue(description + " checkbox must receive its full preferred width",
                actionBounds.width >= clearAction.getPreferredSize().width);
    }

    private void assertStatusAndGuideFollow(JPanel root, Component finalClearAction, JLabel status,
                                            JPanel guideContainer, String provider) {
        Rectangle clearBounds = SwingUtilities.convertRectangle(finalClearAction.getParent(),
                finalClearAction.getBounds(), root);
        Rectangle statusBounds = SwingUtilities.convertRectangle(status.getParent(), status.getBounds(), root);
        Rectangle guideBounds = SwingUtilities.convertRectangle(guideContainer.getParent(),
                guideContainer.getBounds(), root);
        assertTrue(provider + " credential status should follow its clear action",
                statusBounds.y >= clearBounds.y + clearBounds.height);
        assertTrue(provider + " guide should follow credential status",
                guideBounds.y >= statusBounds.y + statusBounds.height);
    }

    private static <T extends Component> T component(Object owner, String name, Class<T> type) {
        try {
            Field field = owner.getClass().getDeclaredField(name);
            field.setAccessible(true);
            return type.cast(field.get(owner));
        } catch (ReflectiveOperationException exception) {
            throw new AssertionError("Could not access form component " + name, exception);
        }
    }

    private static void layoutRecursively(Container container) {
        container.doLayout();
        for (Component child : container.getComponents()) {
            if (child instanceof Container nested) {
                layoutRecursively(nested);
            }
        }
    }
}
