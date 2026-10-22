package com.liuzhihang.doc.view.contract;

import com.intellij.openapi.options.ConfigurationException;
import com.intellij.testFramework.EdtTestUtil;
import com.intellij.testFramework.fixtures.LightJavaCodeInsightFixtureTestCase;
import com.intellij.ui.components.JBTextField;
import com.liuzhihang.doc.view.config.ShowDocSettings;
import com.liuzhihang.doc.view.config.YApiSettings;
import com.liuzhihang.doc.view.config.YApiSettingsConfigurable;
import com.liuzhihang.doc.view.config.YuQueSettings;
import com.liuzhihang.doc.view.ui.ShowDocSettingForm;
import com.liuzhihang.doc.view.ui.YApiSettingForm;
import com.liuzhihang.doc.view.ui.YuQueSettingForm;

import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class IntegrationSettingsLifecycleTest extends LightJavaCodeInsightFixtureTestCase {

    private ShowDocSettings showDocSettings;
    private YApiSettings yApiSettings;
    private YuQueSettings yuQueSettings;

    private String originalShowDocUrl;
    private String originalYApiUrl;
    private Long originalYApiProjectId;
    private String originalYuQueUrl;
    private String originalYuQueApiUrl;
    private String originalYuQueLogin;
    private String originalYuQueRepos;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        showDocSettings = ShowDocSettings.getInstance(getProject());
        yApiSettings = YApiSettings.getInstance(getProject());
        yuQueSettings = YuQueSettings.getInstance(getProject());

        originalShowDocUrl = showDocSettings.getUrl();
        originalYApiUrl = yApiSettings.getUrl();
        originalYApiProjectId = yApiSettings.getProjectId();
        originalYuQueUrl = yuQueSettings.getUrl();
        originalYuQueApiUrl = yuQueSettings.getApiUrl();
        originalYuQueLogin = yuQueSettings.getLogin();
        originalYuQueRepos = yuQueSettings.getRepos();

        clearSettings();
    }

    @Override
    protected void tearDown() throws Exception {
        try {
            showDocSettings.setUrl(originalShowDocUrl);
            yApiSettings.setUrl(originalYApiUrl);
            yApiSettings.setProjectId(originalYApiProjectId);
            yuQueSettings.setUrl(originalYuQueUrl);
            yuQueSettings.setApiUrl(originalYuQueApiUrl);
            yuQueSettings.setLogin(originalYuQueLogin);
            yuQueSettings.setRepos(originalYuQueRepos);
        } finally {
            super.tearDown();
        }
    }

    public void testShowDocNullValuesAreNotModified() throws Exception {
        EdtTestUtil.runInEdtAndWait(() -> {
            ShowDocSettingForm form = new ShowDocSettingForm(getProject());
            form.reset();
            assertFalse(form.isModified());
        });
    }

    public void testYApiNullValuesAreNotModified() throws Exception {
        EdtTestUtil.runInEdtAndWait(() -> {
            YApiSettingForm form = new YApiSettingForm(getProject());
            form.reset();
            assertFalse(form.isModified());
        });
    }

    public void testYuQueNullOptionalValuesAreNotModified() throws Exception {
        EdtTestUtil.runInEdtAndWait(() -> {
            YuQueSettingForm form = new YuQueSettingForm(getProject());
            form.reset();
            assertFalse(form.isModified());
        });
    }

    public void testUrlApplyNormalizesAllFormsAndLeavesThemUnmodified() throws Exception {
        showDocSettings.setUrl("https://old.showdoc.test");
        yApiSettings.setUrl("https://old.yapi.test");
        yApiSettings.setProjectId(17L);
        yuQueSettings.setUrl("https://old.yuque.test");
        yuQueSettings.setApiUrl("https://old.api.test");
        yuQueSettings.setLogin("owner");
        yuQueSettings.setRepos("repo");

        EdtTestUtil.runInEdtAndWait(() -> {
            ShowDocSettingForm showDocForm = new ShowDocSettingForm(getProject());
            showDocForm.reset();
            setText(showDocForm, "urlTextField", " https://api.example.test/// ");
            apply(showDocForm);
            assertEquals("https://api.example.test", showDocSettings.getUrl());
            assertFalse(showDocForm.isModified());

            YApiSettingForm yApiForm = new YApiSettingForm(getProject());
            yApiForm.reset();
            setText(yApiForm, "urlTextField", " https://yapi.example.test/// ");
            apply(yApiForm);
            assertEquals("https://yapi.example.test", yApiSettings.getUrl());
            assertFalse(yApiForm.isModified());

            YuQueSettingForm yuQueForm = new YuQueSettingForm(getProject());
            yuQueForm.reset();
            setText(yuQueForm, "urlTextField", " https://yuque.example.test/// ");
            setText(yuQueForm, "apiUrlTextField", " https://api.yuque.example.test/// ");
            apply(yuQueForm);
            assertEquals("https://yuque.example.test", yuQueSettings.getUrl());
            assertEquals("https://api.yuque.example.test", yuQueSettings.getApiUrl());
            assertFalse(yuQueForm.isModified());
        });
    }

    public void testIncompleteSettingsCanBeAppliedWithoutNetworkValidation() throws Exception {
        EdtTestUtil.runInEdtAndWait(() -> {
            ShowDocSettingForm showDocForm = new ShowDocSettingForm(getProject());
            showDocForm.reset();
            setText(showDocForm, "urlTextField", "https://partial.showdoc.test");
            apply(showDocForm);
            assertEquals("https://partial.showdoc.test", showDocSettings.getUrl());

            YApiSettingForm yApiForm = new YApiSettingForm(getProject());
            yApiForm.reset();
            setText(yApiForm, "urlTextField", "https://partial.yapi.test");
            apply(yApiForm);
            assertEquals("https://partial.yapi.test", yApiSettings.getUrl());
            assertNull(yApiSettings.getProjectId());

            YuQueSettingForm yuQueForm = new YuQueSettingForm(getProject());
            yuQueForm.reset();
            setText(yuQueForm, "loginTextField", "owner");
            apply(yuQueForm);
            assertEquals("owner", yuQueSettings.getLogin());
        });
    }

    public void testYApiValidDraftWithUnconfiguredSavedIdIsModified() throws Exception {
        yApiSettings.setUrl("https://yapi.example.test");

        EdtTestUtil.runInEdtAndWait(() -> {
            YApiSettingForm form = new YApiSettingForm(getProject());
            form.reset();
            setText(form, "projectIdTextField", "1");
            assertTrue(form.isModified());
        });
    }

    public void testYApiInvalidOrEmptyIdStillMarksDraftModified() throws Exception {
        yApiSettings.setUrl("https://yapi.example.test");
        yApiSettings.setProjectId(17L);

        EdtTestUtil.runInEdtAndWait(() -> {
            YApiSettingForm invalidForm = new YApiSettingForm(getProject());
            invalidForm.reset();
            setText(invalidForm, "projectIdTextField", "not-a-number");
            assertTrue(invalidForm.isModified());

            YApiSettingForm emptyForm = new YApiSettingForm(getProject());
            emptyForm.reset();
            setText(emptyForm, "projectIdTextField", "");
            assertTrue(emptyForm.isModified());
        });
    }

    public void testYApiResetClearsDraftWhenSavedIdIsNull() throws Exception {
        yApiSettings.setUrl("https://yapi.example.test");

        EdtTestUtil.runInEdtAndWait(() -> {
            YApiSettingForm form = new YApiSettingForm(getProject());
            setText(form, "projectIdTextField", "draft");
            form.reset();
            assertEquals("", text(form, "projectIdTextField"));
            assertFalse(form.isModified());
        });
    }

    public void testYApiClearingProjectIdAppliesNull() throws Exception {
        yApiSettings.setUrl("https://yapi.example.test");
        yApiSettings.setProjectId(17L);

        EdtTestUtil.runInEdtAndWait(() -> {
            YApiSettingForm form = new YApiSettingForm(getProject());
            form.reset();
            setText(form, "projectIdTextField", "");
            apply(form);
            assertNull(yApiSettings.getProjectId());
            assertFalse(form.isModified());
        });
    }

    public void testYApiValidLeadingZeroIdIsAppliedAndCanonicalized() throws Exception {
        yApiSettings.setUrl("https://yapi.example.test");

        EdtTestUtil.runInEdtAndWait(() -> {
            YApiSettingForm form = new YApiSettingForm(getProject());
            form.reset();
            setText(form, "projectIdTextField", "001");
            apply(form);
            assertEquals(Long.valueOf(1L), yApiSettings.getProjectId());
            assertEquals("1", text(form, "projectIdTextField"));
            assertFalse(form.isModified());
        });
    }

    public void testYApiMaximumLongProjectIdIsAccepted() throws Exception {
        yApiSettings.setUrl("https://yapi.example.test");

        EdtTestUtil.runInEdtAndWait(() -> {
            YApiSettingForm form = new YApiSettingForm(getProject());
            form.reset();
            setText(form, "projectIdTextField", "9223372036854775807");
            apply(form);
            assertEquals(Long.valueOf(Long.MAX_VALUE), yApiSettings.getProjectId());
            assertFalse(form.isModified());
        });
    }

    public void testYApiInvalidIdDoesNotPartiallyApply() throws Exception {
        assertYApiRejectedAtomically("not-a-number");
    }

    public void testYApiZeroIdIsRejected() throws Exception {
        assertYApiRejectedAtomically("0");
    }

    public void testYApiNegativeIdIsRejected() throws Exception {
        assertYApiRejectedAtomically("-1");
    }

    public void testYApiOverflowIdDoesNotPartiallyApply() throws Exception {
        assertYApiRejectedAtomically("9223372036854775808");
    }

    public void testYApiDescriptorIdMatchesConfigurableId() throws Exception {
        String descriptor = Files.readString(Path.of("src/main/resources/META-INF/plugin.xml"));
        Matcher matcher = Pattern.compile("instance=\"com\\.liuzhihang\\.doc\\.view\\.config\\.YApiSettingsConfigurable\"\\s+id=\"([^\"]+)\"")
                .matcher(descriptor);
        assertTrue("YApi configurable should be registered in plugin.xml", matcher.find());
        assertEquals("liuzhihang.api.doc.YApiSettingsConfigurable",
                new YApiSettingsConfigurable(getProject()).getId());
        assertEquals(matcher.group(1), new YApiSettingsConfigurable(getProject()).getId());
    }

    private void assertYApiRejectedAtomically(String projectId) throws Exception {
        yApiSettings.setUrl("https://saved.yapi.test");
        yApiSettings.setProjectId(17L);

        EdtTestUtil.runInEdtAndWait(() -> {
            YApiSettingForm form = new YApiSettingForm(getProject());
            form.reset();
            setText(form, "urlTextField", " https://draft.yapi.test/// ");
            setText(form, "projectIdTextField", projectId);
            boolean rejected = false;
            try {
                form.apply();
            } catch (ConfigurationException expected) {
                rejected = true;
            }

            assertTrue("Invalid YApi project ID should reject Apply: " + projectId, rejected);
            assertEquals("https://saved.yapi.test", yApiSettings.getUrl());
            assertEquals(Long.valueOf(17L), yApiSettings.getProjectId());
            assertEquals(projectId, text(form, "projectIdTextField"));
        });
    }

    private void clearSettings() {
        showDocSettings.setUrl(null);
        yApiSettings.setUrl(null);
        yApiSettings.setProjectId(null);
        yuQueSettings.setUrl(null);
        yuQueSettings.setApiUrl(null);
        yuQueSettings.setLogin(null);
        yuQueSettings.setRepos(null);
    }

    private static void apply(ShowDocSettingForm form) {
        try {
            form.apply();
        } catch (ConfigurationException exception) {
            throw new AssertionError("Applying ShowDoc settings should succeed", exception);
        }
    }

    private static void apply(YApiSettingForm form) {
        try {
            form.apply();
        } catch (ConfigurationException exception) {
            throw new AssertionError("Applying YApi settings should succeed", exception);
        }
    }

    private static void apply(YuQueSettingForm form) {
        try {
            form.apply();
        } catch (ConfigurationException exception) {
            throw new AssertionError("Applying YuQue settings should succeed", exception);
        }
    }

    private static void setText(Object form, String fieldName, String value) {
        field(form, fieldName, JBTextField.class).setText(value);
    }

    private static String text(Object form, String fieldName) {
        return field(form, fieldName, JBTextField.class).getText();
    }

    private static <T> T field(Object target, String fieldName, Class<T> type) {
        try {
            Field field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            return type.cast(field.get(target));
        } catch (ReflectiveOperationException exception) {
            throw new AssertionError("Could not access form field " + fieldName, exception);
        }
    }
}
