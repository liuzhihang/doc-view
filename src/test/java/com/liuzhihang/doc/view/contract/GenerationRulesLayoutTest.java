package com.liuzhihang.doc.view.contract;

import com.intellij.testFramework.LightProjectDescriptor;
import com.intellij.testFramework.fixtures.LightJavaCodeInsightFixtureTestCase;
import com.intellij.ui.ContextHelpLabel;
import com.intellij.ui.TitledSeparator;
import com.intellij.ui.components.JBTextArea;
import com.intellij.util.ui.UIUtil;
import com.liuzhihang.doc.view.config.Settings;
import com.liuzhihang.doc.view.ui.GenerationRulesForm;
import com.liuzhihang.doc.view.ui.SettingsForm;

import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.border.TitledBorder;
import java.awt.Component;
import java.awt.Container;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public final class GenerationRulesLayoutTest extends LightJavaCodeInsightFixtureTestCase {

    private static final String[] CHECKBOX_NAMES = {
            "titleCommentTagCheckBox", "titleClassCommentCheckBox", "titleSimpleClassNameCheckBox",
            "titleFullClassNameCheckBox", "nameSwagger3CheckBox", "nameSwaggerCheckBox",
            "nameCommentTagCheckBox", "nameMethodCommentCheckBox", "descSwagger3CheckBox",
            "descSwaggerCheckBox", "requireCommentTagCheckBox", "fieldNameJsonPropertyCheckBox"
    };

    @Override
    protected LightProjectDescriptor getProjectDescriptor() {
        return JAVA_21;
    }

    public void testGenerationRulesUseNativeGroupsAndConciseAccessibleHelp() throws Exception {
        GenerationRulesForm form = new GenerationRulesForm(getProject());
        JPanel root = form.getRootPanel();

        String[] groups = {"generationTitleGroup", "generationNameGroup", "generationDescriptionGroup",
                "generationRequiredGroup", "generationFieldNameGroup"};
        String[] separators = {"generationTitleSeparator", "generationNameSeparator",
                "generationDescriptionSeparator", "generationRequiredSeparator", "generationFieldNameSeparator"};
        String[] summaryNames = {"generationTitleSummary", "generationNameSummary",
                "generationDescriptionSummary", "generationRequiredSummary", "generationFieldNameSummary"};
        String[] helpNames = {"generationTitleHelpIcon", "generationNameHelpIcon",
                "generationDescriptionHelpIcon", "generationRequiredHelpIcon", "generationFieldNameHelpIcon"};
        String[] detailSnippets = {"@DocView.Title", "@Operation summary", "@Operation description",
                "@Schema(requiredMode=", "@JsonProperty"};

        for (int index = 0; index < groups.length; index++) {
            Component groupComponent = findByName(root, groups[index]);
            assertNotNull("Missing rule group " + groups[index], groupComponent);
            assertTrue(groupComponent instanceof JPanel);
            assertFalse("Rule groups should use IDE separators instead of boxed borders",
                    ((JPanel) groupComponent).getBorder() instanceof TitledBorder);

            Component separatorComponent = findByName(root, separators[index]);
            assertTrue("Missing native separator " + separators[index], separatorComponent instanceof TitledSeparator);

            JBTextArea summary = assertWrappingHelp(root, summaryNames[index]);
            assertTrue("The visible helper should be concise", summary.getText().length() <= 40);
            assertFalse("Detailed rule order should not remain in the main layout",
                    summary.getText().contains("@Operation") || summary.getText().contains("@ApiOperation")
                            || summary.getText().contains("Schema.requiredMode") || summary.getText().contains("Parameter.required"));
            assertEquals(UIUtil.getContextHelpForeground(), summary.getForeground());
            assertTrue("Summary text should use the platform small font",
                    summary.getFont().getSize2D() < findCheckBox(root, CHECKBOX_NAMES[0]).getFont().getSize2D());

            Component helpComponent = findByName(root, helpNames[index]);
            assertTrue("Missing context help icon " + helpNames[index], helpComponent instanceof ContextHelpLabel);
            var accessible = helpComponent.getAccessibleContext();
            assertNotNull(accessible.getAccessibleName());
            assertFalse("Help icon needs an accessible name", accessible.getAccessibleName().isBlank());
            assertNotNull(accessible.getAccessibleDescription());
            assertTrue("Rule details should use explicit lines instead of relying on tooltip wrapping",
                    accessible.getAccessibleDescription().contains("\n1."));
            assertTrue("Tooltip/accessibility should explain the configured rule order",
                    accessible.getAccessibleDescription().contains(detailSnippets[index]));
            int expectedParagraphBreaks = index < 3 ? 1 : index == 3 ? 2 : 1;
            String tooltipFragment = toTooltipHtml(accessible.getAccessibleDescription());
            assertTrue("HelpTooltip should render explicit rule lines", tooltipFragment.contains("<br>"));
            assertEquals("HelpTooltip should use paragraph separators for the intended groups",
                    expectedParagraphBreaks, countOccurrences(tooltipFragment, "<p>"));
            assertFalse("HelpTooltip supplies its own HTML wrapper", tooltipFragment.contains("<html>")
                    || tooltipFragment.contains("<body>") || tooltipFragment.contains("</p>"));
            int expectedAccessibleParagraphs = expectedParagraphBreaks + 1;
            assertEquals("Accessible help should expose the same paragraph grouping",
                    expectedAccessibleParagraphs,
                    accessible.getAccessibleDescription().split("\\n\\n", -1).length);
        }

        assertEquals("There should be one native separator per rule group", 5, findAll(root, TitledSeparator.class).size());
        assertEquals("Each rule group should keep its own detailed help icon", 5,
                findAll(root, ContextHelpLabel.class).stream()
                        .filter(icon -> icon.getName() != null && icon.getName().startsWith("generation")
                                && icon.getName().endsWith("HelpIcon"))
                        .count());
        Component settingsGuide = findByName(root, "settingsGuideHelpIcon");
        assertTrue("The page guide should be separate from the five rule help icons",
                settingsGuide instanceof ContextHelpLabel);
        assertEquals("The page should have five rule details and one separate guide icon",
                6, findAll(root, ContextHelpLabel.class).size());
        assertEquals("The visual change must retain the existing rule controls", 12, findAll(root, JCheckBox.class).size());
        for (String name : CHECKBOX_NAMES) {
            assertNotNull("Missing existing rule control " + name, findByName(root, name));
        }

        JBTextArea requiredSummary = (JBTextArea) findByName(root, "generationRequiredSummary");
        int wideWidth = layoutAtWidth(root, requiredSummary, 760);
        int narrowWidth = layoutAtWidth(root, requiredSummary, 520);
        assertTrue("Rule help should have a visible width in a narrow viewport", narrowWidth > 0);
        assertTrue("Rule help should track the available width", narrowWidth < wideWidth);
        assertTrue("Rule help must stay inside the settings viewport", narrowWidth <= root.getWidth());

        assertEquals("Tooltip text must escape translated content before adding markup",
                "&lt;label&gt; &amp; value<br>next<p>section",
                toTooltipHtml("<label> & value\nnext\n\nsection"));
    }

    public void testRuleControlsRetainApplyResetAndModifiedSemantics() {
        GenerationRulesForm form = new GenerationRulesForm(getProject());
        Settings settings = Settings.getInstance(getProject());
        form.reset();
        assertFalse(form.isModified());

        JCheckBox titleCommentTag = findCheckBox(form.getRootPanel(), "titleCommentTagCheckBox");
        boolean originalValue = settings.getTitleUseCommentTag();
        titleCommentTag.setSelected(!originalValue);
        assertTrue(form.isModified());

        form.apply();
        assertTrue(settings.getTitleUseCommentTag() != originalValue);
        assertFalse(form.isModified());

        titleCommentTag.setSelected(originalValue);
        assertTrue(form.isModified());
        form.reset();
        assertEquals(!originalValue, titleCommentTag.isSelected());
        assertFalse(form.isModified());

        titleCommentTag.setSelected(originalValue);
        form.apply();
        assertTrue(settings.getTitleUseCommentTag() == originalValue);
        form.reset();
    }

    public void testGeneralSettingsDescriptionsWrapInsideTheAvailableWidth() {
        SettingsForm form = new SettingsForm(getProject());
        JPanel root = form.getRootPanel();

        JBTextArea lineMarker = assertWrappingHelp(root, "settingsLineMarkerDescription");
        JBTextArea normalInterfaces = assertWrappingHelp(root, "settingsNormalInterfaceDescription");
        assertNarrowerWidthReducesHelpWidth(root, lineMarker);
        assertNarrowerWidthReducesHelpWidth(root, normalInterfaces);
    }

    public void testSettingsGuideUsesNativeContextHelpIcon() {
        SettingsForm form = new SettingsForm(getProject());
        Component guide = findByName(form.getRootPanel(), "settingsGuideHelpIcon");
        assertTrue("The settings guide should use the native question-mark help icon",
                guide instanceof ContextHelpLabel);
    }

    private JBTextArea assertWrappingHelp(Container root, String name) {
        Component component = findByName(root, name);
        assertNotNull("Missing wrapped help component " + name, component);
        assertTrue(name + " must use a wrapping text component", component instanceof JBTextArea);
        JBTextArea textArea = (JBTextArea) component;
        assertTrue(name + " must wrap long text", textArea.getLineWrap());
        assertTrue(name + " must wrap at word boundaries", textArea.getWrapStyleWord());
        assertFalse(name + " must remain read-only", textArea.isEditable());
        assertFalse(name + " must not take keyboard focus", textArea.isFocusable());
        assertEquals(name + " must align with the section content", Component.LEFT_ALIGNMENT,
                textArea.getAlignmentX(), 0.01f);
        return textArea;
    }

    private JCheckBox findCheckBox(Container root, String name) {
        Component component = findByName(root, name);
        assertNotNull("Missing checkbox " + name, component);
        assertTrue("Expected a checkbox named " + name, component instanceof JCheckBox);
        return (JCheckBox) component;
    }

    private void assertNarrowerWidthReducesHelpWidth(JPanel root, JBTextArea textArea) {
        int wideWidth = layoutAtWidth(root, textArea, 760);
        int narrowWidth = layoutAtWidth(root, textArea, 520);
        assertTrue("Help text should have a visible width in a narrow settings viewport", narrowWidth > 0);
        assertTrue("Help text should track viewport width instead of retaining its long-line width",
                narrowWidth < wideWidth);
        assertTrue("Help text must stay inside the settings viewport", narrowWidth <= root.getWidth());
    }

    private static int layoutAtWidth(JPanel root, JBTextArea textArea, int width) {
        root.setSize(width, 1200);
        layoutRecursively(root);
        return textArea.getWidth();
    }

    private static void layoutRecursively(Container container) {
        container.doLayout();
        for (Component child : container.getComponents()) {
            if (child instanceof Container nested) {
                layoutRecursively(nested);
            }
        }
    }

    private static String toTooltipHtml(String text) throws Exception {
        Method formatter = GenerationRulesForm.class.getDeclaredMethod("toTooltipHtml", String.class);
        formatter.setAccessible(true);
        return (String) formatter.invoke(null, text);
    }

    private static int countOccurrences(String text, String needle) {
        int count = 0;
        int offset = 0;
        while ((offset = text.indexOf(needle, offset)) >= 0) {
            count++;
            offset += needle.length();
        }
        return count;
    }

    private static Component findByName(Container container, String name) {
        for (Component child : container.getComponents()) {
            if (name.equals(child.getName())) {
                return child;
            }
            if (child instanceof Container nested) {
                Component match = findByName(nested, name);
                if (match != null) {
                    return match;
                }
            }
        }
        return null;
    }

    private static <T extends Component> List<T> findAll(Container container, Class<T> type) {
        List<T> result = new ArrayList<>();
        for (Component child : container.getComponents()) {
            if (type.isInstance(child)) {
                result.add(type.cast(child));
            }
            if (child instanceof Container nested) {
                result.addAll(findAll(nested, type));
            }
        }
        return result;
    }
}
