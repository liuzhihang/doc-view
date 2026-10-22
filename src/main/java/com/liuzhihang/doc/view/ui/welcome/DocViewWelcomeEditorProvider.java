package com.liuzhihang.doc.view.ui.welcome;

import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorPolicy;
import com.intellij.openapi.fileEditor.FileEditorProvider;
import com.intellij.openapi.extensions.PluginAware;
import com.intellij.openapi.extensions.PluginDescriptor;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

public final class DocViewWelcomeEditorProvider implements FileEditorProvider, DumbAware, PluginAware {
    private String version = "development";

    @Override
    public void setPluginDescriptor(@NotNull PluginDescriptor descriptor) {
        version = descriptor.getVersion();
    }
    @Override
    public boolean accept(@NotNull Project project, @NotNull VirtualFile file) {
        return file instanceof DocViewWelcomeFile;
    }

    @Override
    public @NotNull FileEditor createEditor(@NotNull Project project, @NotNull VirtualFile file) {
        return new DocViewWelcomeEditor(project, (DocViewWelcomeFile) file, version);
    }

    @Override
    public @NotNull String getEditorTypeId() {
        return "doc-view-welcome";
    }

    @Override
    public @NotNull FileEditorPolicy getPolicy() {
        return FileEditorPolicy.HIDE_DEFAULT_EDITOR;
    }
}
