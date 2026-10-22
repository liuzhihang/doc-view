package com.liuzhihang.doc.view.contract;

import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorFactory;
import com.intellij.openapi.fileTypes.FileTypeManager;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiMethod;
import com.intellij.testFramework.EdtTestUtil;
import com.intellij.testFramework.fixtures.LightJavaCodeInsightFixtureTestCase;
import com.intellij.util.xmlb.XmlSerializer;
import com.liuzhihang.doc.view.config.TemplateConfigurable;
import com.liuzhihang.doc.view.config.TemplateSettings;
import com.liuzhihang.doc.view.dto.DocView;
import com.liuzhihang.doc.view.dto.DocViewData;
import com.liuzhihang.doc.view.enums.FrameworkEnum;
import com.liuzhihang.doc.view.testsupport.AnnotationStubs;
import com.liuzhihang.doc.view.ui.TemplateSettingForm;
import org.jdom.Element;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;

public final class TemplateSettingsLifecycleTest extends LightJavaCodeInsightFixtureTestCase {

    private Project project;
    private TemplateSettings settings;
    private String originalSpring;
    private String originalDubbo;
    private EditorFactory editorFactory;
    private Set<Editor> editorsBefore;
    private final List<TemplateSettingForm> forms = new ArrayList<>();

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        project = getProject();
        settings = TemplateSettings.getInstance(project);
        originalSpring = settings.getSpringTemplate();
        originalDubbo = settings.getDubboTemplate();
        editorFactory = EditorFactory.getInstance();
        editorsBefore = editorsForProject(editorFactory, project);
        AnnotationStubs.installSpringWeb(myFixture);
        AnnotationStubs.installDubbo(myFixture);
    }

    @Override
    protected void tearDown() throws Exception {
        try {
            if (project != null && editorFactory != null) {
                EdtTestUtil.runInEdtAndWait(() -> {
                    for (TemplateSettingForm form : forms) {
                        if (!form.isDisposed()) {
                            form.disposeIfNeeded();
                        }
                    }
                    releaseCreatedEditors();
                });
            }
        } finally {
            try {
                if (settings != null) {
                    settings.setSpringTemplate(originalSpring);
                    settings.setDubboTemplate(originalDubbo);
                }
            } finally {
                super.tearDown();
            }
        }
    }

    public void testCreateResetAndReopenKeepSavedCustomTemplates() throws Exception {
        String spring = "# SPRING_A\n${DocView.name}\n";
        String dubbo = "# DUBBO_A\n${DocView.name}\n";
        setSavedTemplates(spring, dubbo);

        EdtTestUtil.runInEdtAndWait(() -> {
            TemplateConfigurable first = openPageAndReset();

            assertEquals("Creating and resetting the page must preserve the saved Spring template",
                    spring, settings.getSpringTemplate());
            assertEquals("Creating and resetting the page must preserve the saved Dubbo template",
                    dubbo, settings.getDubboTemplate());
            assertEquals(spring, editorDocument(first, "springTemplateEditor").getText());
            assertEquals(dubbo, editorDocument(first, "dubboTemplateEditor").getText());
            assertFalse("A freshly reset page should not report unsaved changes", first.isModified());

            closeWithoutApplying(first);
            TemplateConfigurable reopened = openPageAndReset();
            assertEquals(spring, settings.getSpringTemplate());
            assertEquals(dubbo, settings.getDubboTemplate());
            assertEquals(spring, editorDocument(reopened, "springTemplateEditor").getText());
            assertEquals(dubbo, editorDocument(reopened, "dubboTemplateEditor").getText());
            assertFalse("A reopened page should start without unsaved changes", reopened.isModified());
        });
    }

    public void testResetRestoresSavedTemplatesAndIsIdempotent() throws Exception {
        String spring = "# SPRING_A\n";
        String dubbo = "# DUBBO_A\n";
        setSavedTemplates(spring, dubbo);

        EdtTestUtil.runInEdtAndWait(() -> {
            TemplateConfigurable configurable = openPageAndReset();
            setDocumentText(configurable, "springTemplateEditor", "# SPRING_B\n");
            setDocumentText(configurable, "dubboTemplateEditor", "# DUBBO_B\n");
            assertTrue(configurable.isModified());

            configurable.reset();
            assertEquals(spring, settings.getSpringTemplate());
            assertEquals(dubbo, settings.getDubboTemplate());
            assertEquals(spring, editorDocument(configurable, "springTemplateEditor").getText());
            assertEquals(dubbo, editorDocument(configurable, "dubboTemplateEditor").getText());
            assertFalse(configurable.isModified());

            configurable.reset();
            assertEquals(spring, settings.getSpringTemplate());
            assertEquals(dubbo, settings.getDubboTemplate());
            assertEquals(spring, editorDocument(configurable, "springTemplateEditor").getText());
            assertEquals(dubbo, editorDocument(configurable, "dubboTemplateEditor").getText());
            assertFalse("Repeated Reset should remain unchanged", configurable.isModified());
        });
    }

    public void testApplySpringTemplatePreservesSavedDubboTemplate() throws Exception {
        String spring = "# SPRING_A\n";
        String dubbo = "# DUBBO_A\n";
        String updatedSpring = "# SPRING_B\n";
        setSavedTemplates(spring, dubbo);

        EdtTestUtil.runInEdtAndWait(() -> {
            TemplateConfigurable configurable = openPageAndReset();
            setDocumentText(configurable, "springTemplateEditor", updatedSpring);
            assertTrue(configurable.isModified());

            apply(configurable);

            assertEquals(updatedSpring, settings.getSpringTemplate());
            assertEquals(dubbo, settings.getDubboTemplate());
            assertEquals(updatedSpring, editorDocument(configurable, "springTemplateEditor").getText());
            assertEquals(dubbo, editorDocument(configurable, "dubboTemplateEditor").getText());
            assertFalse(configurable.isModified());
        });
    }

    public void testApplyDubboTemplatePreservesSavedSpringTemplate() throws Exception {
        String spring = "# SPRING_A\n";
        String dubbo = "# DUBBO_A\n";
        String updatedDubbo = "# DUBBO_B\n";
        setSavedTemplates(spring, dubbo);

        EdtTestUtil.runInEdtAndWait(() -> {
            TemplateConfigurable configurable = openPageAndReset();
            setDocumentText(configurable, "dubboTemplateEditor", updatedDubbo);
            assertTrue(configurable.isModified());

            apply(configurable);

            assertEquals(spring, settings.getSpringTemplate());
            assertEquals(updatedDubbo, settings.getDubboTemplate());
            assertEquals(spring, editorDocument(configurable, "springTemplateEditor").getText());
            assertEquals(updatedDubbo, editorDocument(configurable, "dubboTemplateEditor").getText());
            assertFalse(configurable.isModified());
        });
    }

    public void testApplyBothTemplatesPreservesExactTextAndIsIdempotent() throws Exception {
        String spring = "# SPRING_A\n";
        String dubbo = "# DUBBO_A\n";
        String updatedSpring = "# Spring：中文\n  保留首尾空格  \n\n${DocView.name}\n";
        String updatedDubbo = "";
        setSavedTemplates(spring, dubbo);

        EdtTestUtil.runInEdtAndWait(() -> {
            TemplateConfigurable configurable = openPageAndReset();
            setDocumentText(configurable, "springTemplateEditor", updatedSpring);
            setDocumentText(configurable, "dubboTemplateEditor", updatedDubbo);
            assertTrue(configurable.isModified());

            apply(configurable);
            assertEquals(updatedSpring, settings.getSpringTemplate());
            assertEquals(updatedDubbo, settings.getDubboTemplate());
            assertFalse(configurable.isModified());

            apply(configurable);
            assertEquals(updatedSpring, settings.getSpringTemplate());
            assertEquals(updatedDubbo, settings.getDubboTemplate());
            assertEquals(updatedSpring, editorDocument(configurable, "springTemplateEditor").getText());
            assertEquals(updatedDubbo, editorDocument(configurable, "dubboTemplateEditor").getText());
            assertFalse("Repeated Apply should be idempotent", configurable.isModified());
        });
    }

    public void testDisposeWithoutApplyKeepsSavedTemplatesForReopen() throws Exception {
        String spring = "# SPRING_A\n";
        String dubbo = "# DUBBO_A\n";
        setSavedTemplates(spring, dubbo);

        EdtTestUtil.runInEdtAndWait(() -> {
            TemplateConfigurable first = openPageAndReset();
            setDocumentText(first, "springTemplateEditor", "# SPRING_B\n");
            setDocumentText(first, "dubboTemplateEditor", "# DUBBO_B\n");
            assertTrue(first.isModified());
            closeWithoutApplying(first);

            assertEquals(spring, settings.getSpringTemplate());
            assertEquals(dubbo, settings.getDubboTemplate());
            TemplateConfigurable reopened = openPageAndReset();
            assertEquals(spring, editorDocument(reopened, "springTemplateEditor").getText());
            assertEquals(dubbo, editorDocument(reopened, "dubboTemplateEditor").getText());
            assertFalse(reopened.isModified());
        });
    }

    public void testDisposeAfterApplyKeepsLatestAppliedTemplatesAndDropsDrafts() throws Exception {
        String spring = "# SPRING_A\n";
        String dubbo = "# DUBBO_A\n";
        String appliedSpring = "# SPRING_B\n";
        String appliedDubbo = "# DUBBO_B\n";
        setSavedTemplates(spring, dubbo);

        EdtTestUtil.runInEdtAndWait(() -> {
            TemplateConfigurable first = openPageAndReset();
            setDocumentText(first, "springTemplateEditor", appliedSpring);
            setDocumentText(first, "dubboTemplateEditor", appliedDubbo);
            apply(first);
            assertEquals(appliedSpring, settings.getSpringTemplate());
            assertEquals(appliedDubbo, settings.getDubboTemplate());

            setDocumentText(first, "springTemplateEditor", "# SPRING_C\n");
            setDocumentText(first, "dubboTemplateEditor", "# DUBBO_C\n");
            assertTrue(first.isModified());
            closeWithoutApplying(first);

            assertEquals(appliedSpring, settings.getSpringTemplate());
            assertEquals(appliedDubbo, settings.getDubboTemplate());
            TemplateConfigurable reopened = openPageAndReset();
            assertEquals(appliedSpring, editorDocument(reopened, "springTemplateEditor").getText());
            assertEquals(appliedDubbo, editorDocument(reopened, "dubboTemplateEditor").getText());
            assertFalse(reopened.isModified());
        });
    }

    public void testAppliedTemplatesSurviveXmlSerializationWithoutDrafts() throws Exception {
        String spring = "# SPRING_A\n";
        String dubbo = "# DUBBO_A\n";
        String appliedSpring = "# SPRING_B\n";
        setSavedTemplates(spring, dubbo);

        EdtTestUtil.runInEdtAndWait(() -> {
            TemplateConfigurable configurable = openPageAndReset();
            setDocumentText(configurable, "springTemplateEditor", appliedSpring);
            apply(configurable);
            setDocumentText(configurable, "dubboTemplateEditor", "# DUBBO_DRAFT\n");
            assertTrue(configurable.isModified());
        });

        Element serialized = XmlSerializer.serialize(settings.getState());
        TemplateSettings deserialized = XmlSerializer.deserialize(serialized, TemplateSettings.class);
        TemplateSettings restored = new TemplateSettings();
        restored.loadState(deserialized);

        assertEquals(appliedSpring, restored.getSpringTemplate());
        assertEquals(dubbo, restored.getDubboTemplate());
    }

    public void testGeneratedSpringAndDubboDocsUseTheirAppliedTemplates() throws Exception {
        String springMarker = "SPRING_GENERATION_MARKER";
        String dubboMarker = "DUBBO_GENERATION_MARKER";
        setSavedTemplates("# SPRING_INITIAL\n", "# DUBBO_INITIAL\n");

        EdtTestUtil.runInEdtAndWait(() -> {
            TemplateConfigurable configurable = openPageAndReset();
            setDocumentText(configurable, "springTemplateEditor", springMarker + ":${DocView.name}");
            setDocumentText(configurable, "dubboTemplateEditor", dubboMarker + ":${DocView.name}");
            apply(configurable);
            setDocumentText(configurable, "springTemplateEditor", "SPRING_UNAPPLIED_DRAFT");
            setDocumentText(configurable, "dubboTemplateEditor", "DUBBO_UNAPPLIED_DRAFT");
        });

        PsiClass controller = myFixture.addClass("""
                package contract.templates;

                import org.springframework.web.bind.annotation.GetMapping;
                import org.springframework.web.bind.annotation.RestController;

                @RestController
                public class TemplateController {
                    @GetMapping("/spring")
                    public void springEndpoint() {
                    }
                }
                """);
        PsiMethod springMethod = controller.findMethodsByName("springEndpoint", false)[0];
        DocView springDoc = com.liuzhihang.doc.view.service.DocViewService
                .getInstance(project, controller).buildClassMethodDoc(controller, springMethod);

        PsiClass dubboInterface = myFixture.addClass("""
                package contract.templates;

                @org.apache.dubbo.config.annotation.DubboService
                public interface TemplateDubboService {
                    void dubboEndpoint();
                }
                """);
        PsiMethod dubboMethod = dubboInterface.findMethodsByName("dubboEndpoint", false)[0];
        DocView dubboDoc = com.liuzhihang.doc.view.service.DocViewService
                .getInstance(project, dubboInterface).buildClassMethodDoc(dubboInterface, dubboMethod);

        assertEquals(FrameworkEnum.SPRING, springDoc.getType());
        assertEquals(FrameworkEnum.DUBBO, dubboDoc.getType());
        String springMarkdown = DocViewData.markdownText(project, springDoc);
        String dubboMarkdown = DocViewData.markdownText(project, dubboDoc);
        assertTrue(springMarkdown.contains(springMarker));
        assertTrue(springMarkdown.contains(springDoc.getName()));
        assertFalse(springMarkdown.contains("SPRING_UNAPPLIED_DRAFT"));
        assertTrue(dubboMarkdown.contains(dubboMarker));
        assertTrue(dubboMarkdown.contains(dubboDoc.getName()));
        assertFalse(dubboMarkdown.contains("DUBBO_UNAPPLIED_DRAFT"));
    }

    public void testDisposeReleasesOwnedEditorsAndAllowsSameConfigurableToReopen() throws Exception {
        EdtTestUtil.runInEdtAndWait(() -> {
            Editor unrelatedEditor = editorFactory.createEditor(
                    editorFactory.createDocument("UNRELATED_EDITOR"), project,
                    FileTypeManager.getInstance().getFileTypeByExtension("md"), true);
            Set<Editor> beforeFirstOpen = editorsForProject(editorFactory, project);

            TemplateConfigurable configurable = new TemplateConfigurable(project);
            createPage(configurable);
            configurable.reset();
            TemplateSettingForm firstForm = formFor(configurable);
            Set<Editor> firstEditors = newEditorsSince(beforeFirstOpen);
            assertEquals("A template form should own its two templates and description editors", 3, firstEditors.size());

            configurable.disposeUIResources();
            boolean firstFormDisposed = firstForm.isDisposed();
            boolean firstEditorsReleased = editorsReleased(firstEditors);
            boolean firstReferenceCleared = formFor(configurable) == null;
            boolean unrelatedEditorSurvivedFirstDispose = isEditorAlive(unrelatedEditor);

            Set<Editor> beforeReopen = editorsForProject(editorFactory, project);
            createPage(configurable);
            configurable.reset();
            TemplateSettingForm secondForm = formFor(configurable);
            Set<Editor> secondEditors = newEditorsSince(beforeReopen);
            assertEquals("Reopening should create a fresh set of three editors", 3, secondEditors.size());
            boolean reopenedWithNewForm = firstForm != secondForm;

            configurable.disposeUIResources();
            configurable.disposeUIResources();
            boolean secondFormDisposed = secondForm.isDisposed();
            boolean secondEditorsReleased = editorsReleased(secondEditors);
            boolean secondReferenceCleared = formFor(configurable) == null;
            boolean unrelatedEditorSurvivedSecondDispose = isEditorAlive(unrelatedEditor);

            TemplateConfigurable neverOpened = new TemplateConfigurable(project);
            neverOpened.disposeUIResources();
            boolean unopenedConfigurableHasNoForm = formFor(neverOpened) == null;

            assertTrue("Disposing should dispose the first DialogWrapper", firstFormDisposed);
            assertTrue("Disposing should release only the first form's three editors", firstEditorsReleased);
            assertTrue("Disposing should clear the Configurable form reference", firstReferenceCleared);
            assertTrue("The same Configurable should create a fresh form after disposal", reopenedWithNewForm);
            assertTrue("Disposing the reopened form should dispose its DialogWrapper", secondFormDisposed);
            assertTrue("Disposing the reopened form should release its three editors", secondEditorsReleased);
            assertTrue("Repeated disposal should leave the Configurable reference clear", secondReferenceCleared);
            assertTrue("An unrelated project editor should survive both disposals",
                    unrelatedEditorSurvivedFirstDispose && unrelatedEditorSurvivedSecondDispose);
            assertTrue("Disposing a Configurable before component creation should be safe", unopenedConfigurableHasNoForm);
        });
    }

    private TemplateConfigurable openPageAndReset() {
        TemplateConfigurable configurable = new TemplateConfigurable(project);
        createPage(configurable);
        configurable.reset();
        return configurable;
    }

    private void createPage(TemplateConfigurable configurable) {
        assertNotNull("Template settings component should be created", configurable.createComponent());
        TemplateSettingForm form = formFor(configurable);
        assertNotNull("Template settings form should be created", form);
        forms.add(form);
    }

    private void setSavedTemplates(String spring, String dubbo) {
        settings.setSpringTemplate(spring);
        settings.setDubboTemplate(dubbo);
    }

    private void setDocumentText(TemplateConfigurable configurable, String editorField, String text) {
        Document document = editorDocument(configurable, editorField);
        WriteCommandAction.runWriteCommandAction(project, () -> document.setText(text));
    }

    private static void apply(TemplateConfigurable configurable) {
        try {
            configurable.apply();
        } catch (Exception exception) {
            throw new AssertionError("Applying template settings should succeed", exception);
        }
    }

    private static void closeWithoutApplying(TemplateConfigurable configurable) {
        TemplateSettingForm form = formFor(configurable);
        assertNotNull("Template form should exist before closing", form);
        configurable.disposeUIResources();
    }

    private static Document editorDocument(TemplateConfigurable configurable, String editorField) {
        TemplateSettingForm form = formFor(configurable);
        assertNotNull("Template form should exist", form);
        try {
            Field field = TemplateSettingForm.class.getDeclaredField(editorField);
            field.setAccessible(true);
            Editor editor = (Editor) field.get(form);
            return editor.getDocument();
        } catch (ReflectiveOperationException exception) {
            throw new AssertionError("Could not read template editor document " + editorField, exception);
        }
    }

    private static TemplateSettingForm formFor(TemplateConfigurable configurable) {
        try {
            Field field = TemplateConfigurable.class.getDeclaredField("templateSettingForm");
            field.setAccessible(true);
            return (TemplateSettingForm) field.get(configurable);
        } catch (ReflectiveOperationException exception) {
            throw new AssertionError("Could not read template settings form", exception);
        }
    }

    private void releaseCreatedEditors() {
        for (Editor editor : editorFactory.getAllEditors()) {
            if (project.equals(editor.getProject()) && !editorsBefore.contains(editor) && !editor.isDisposed()) {
                editorFactory.releaseEditor(editor);
            }
        }
    }

    private Set<Editor> newEditorsSince(Set<Editor> previousEditors) {
        Set<Editor> created = Collections.newSetFromMap(new IdentityHashMap<>());
        for (Editor editor : editorFactory.getAllEditors()) {
            if (project.equals(editor.getProject()) && !previousEditors.contains(editor)) {
                created.add(editor);
            }
        }
        return created;
    }

    private boolean editorsReleased(Set<Editor> expectedEditors) {
        Set<Editor> liveEditors = editorsForProject(editorFactory, project);
        return Collections.disjoint(expectedEditors, liveEditors)
                && expectedEditors.stream().allMatch(Editor::isDisposed);
    }

    private boolean isEditorAlive(Editor expectedEditor) {
        for (Editor editor : editorFactory.getAllEditors()) {
            if (editor == expectedEditor) {
                return !editor.isDisposed();
            }
        }
        return false;
    }

    private static Set<Editor> editorsForProject(EditorFactory editorFactory, Project project) {
        Set<Editor> editors = Collections.newSetFromMap(new IdentityHashMap<>());
        for (Editor editor : editorFactory.getAllEditors()) {
            if (project.equals(editor.getProject())) {
                editors.add(editor);
            }
        }
        return editors;
    }
}
