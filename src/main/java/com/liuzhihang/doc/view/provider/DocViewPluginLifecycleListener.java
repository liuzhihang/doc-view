package com.liuzhihang.doc.view.provider;

import com.intellij.ide.plugins.DynamicPluginListener;
import com.intellij.ide.plugins.IdeaPluginDescriptor;
import com.intellij.openapi.util.IconLoader;
import org.jetbrains.annotations.NotNull;

/**
 * Releases platform icon-cache entries that can retain this plugin's class loader during dynamic unload.
 */
public final class DocViewPluginLifecycleListener implements DynamicPluginListener {

    private static final String DOC_VIEW_PLUGIN_ID = "com.liuzhihang.doc-view";

    @Override
    public void beforePluginUnload(@NotNull IdeaPluginDescriptor pluginDescriptor, boolean isUpdate) {
        if (DOC_VIEW_PLUGIN_ID.equals(pluginDescriptor.getPluginId().getIdString())) {
            IconLoader.clearCache();
        }
    }
}
