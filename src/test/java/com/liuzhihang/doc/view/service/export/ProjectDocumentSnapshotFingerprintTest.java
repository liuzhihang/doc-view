package com.liuzhihang.doc.view.service.export;

import com.intellij.testFramework.fixtures.LightJavaCodeInsightFixtureTestCase;
import com.liuzhihang.doc.view.config.Settings;
import com.liuzhihang.doc.view.config.TemplateSettings;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public final class ProjectDocumentSnapshotFingerprintTest extends LightJavaCodeInsightFixtureTestCase {

    private String originalSpringTemplate;
    private String originalDubboTemplate;
    private HashSet<String> originalMethodAnnotations;
    private HashSet<String> originalExcludeFields;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        TemplateSettings templates = TemplateSettings.getInstance(getProject());
        Settings settings = Settings.getInstance(getProject());
        originalSpringTemplate = templates.getSpringTemplate();
        originalDubboTemplate = templates.getDubboTemplate();
        originalMethodAnnotations = new HashSet<>(settings.getContainMethodAnnotationName());
        originalExcludeFields = new HashSet<>(settings.getExcludeFieldNames());
    }

    @Override
    protected void tearDown() throws Exception {
        try {
            TemplateSettings templates = TemplateSettings.getInstance(getProject());
            templates.setSpringTemplate(originalSpringTemplate);
            templates.setDubboTemplate(originalDubboTemplate);
            Settings settings = Settings.getInstance(getProject());
            settings.setContainMethodAnnotationName(originalMethodAnnotations);
            settings.setExcludeFieldNames(originalExcludeFields);
        } finally {
            super.tearDown();
        }
    }

    public void testFingerprintChangesWithOutputTemplateAndParserSettings() {
        Settings settings = Settings.getInstance(getProject());
        TemplateSettings templates = TemplateSettings.getInstance(getProject());
        String initial = ProjectDocumentSnapshotService.generatorFingerprint(settings, templates);

        templates.setSpringTemplate(templates.getSpringTemplate() + "\n<!-- changed -->");
        String templateChanged = ProjectDocumentSnapshotService.generatorFingerprint(settings, templates);
        assertFalse(initial.equals(templateChanged));

        settings.setExcludeFieldNames(new HashSet<>(List.of("serialVersionUID", "internalValue")));
        String parserChanged = ProjectDocumentSnapshotService.generatorFingerprint(settings, templates);
        assertFalse(templateChanged.equals(parserChanged));
    }

    public void testSetIterationOrderDoesNotChangeFingerprint() {
        Settings settings = Settings.getInstance(getProject());
        TemplateSettings templates = TemplateSettings.getInstance(getProject());
        HashSet<String> values = new HashSet<>(settings.getContainMethodAnnotationName());
        String initial = ProjectDocumentSnapshotService.generatorFingerprint(settings, templates);

        List<String> reversed = new ArrayList<>(values);
        java.util.Collections.reverse(reversed);
        settings.setContainMethodAnnotationName(new HashSet<>(reversed));

        assertEquals(initial, ProjectDocumentSnapshotService.generatorFingerprint(settings, templates));
    }
    public void testJavadocSettingsInvalidateGeneratorFingerprintAndSchemaCache() {
        Settings settings = new Settings();
        TemplateSettings templates = new TemplateSettings();
        String initial = ProjectDocumentSnapshotService.generatorFingerprint(settings, templates);
        settings.setCommentTitleMaxLength(40);
        String shorter = ProjectDocumentSnapshotService.generatorFingerprint(settings, templates);
        assertFalse(initial.equals(shorter));
        settings.setNameCommentFirstLine(false);
        String full = ProjectDocumentSnapshotService.generatorFingerprint(settings, templates);
        assertFalse(shorter.equals(full));
        settings.setDescCommentBody(false);
        assertFalse(full.equals(ProjectDocumentSnapshotService.generatorFingerprint(settings, templates)));
    }

}
