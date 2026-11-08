package com.liuzhihang.doc.view.contract;

import com.intellij.ide.plugins.PluginManagerCore;
import com.intellij.openapi.extensions.PluginDescriptor;
import com.intellij.openapi.extensions.PluginId;
import com.liuzhihang.doc.view.DocViewBuildInfo;
import com.intellij.testFramework.LightPlatformTestCase;

public final class DocViewBuildInfoTest extends LightPlatformTestCase {

    private static final String PLUGIN_ID = "com.liuzhihang.doc-view";

    public void testBuildInfoMatchesTheLoadedPluginDescriptor() {
        PluginDescriptor descriptor = PluginManagerCore.getPlugin(PluginId.getId(PLUGIN_ID));

        assertNotNull("The Doc View plugin descriptor must be registered in the test runtime", descriptor);
        assertEquals(PLUGIN_ID, descriptor.getPluginId().getIdString());
        assertNotNull("The loaded Doc View plugin version must be available", descriptor.getVersion());
        assertFalse("The loaded Doc View plugin version must be nonblank", descriptor.getVersion().isBlank());
        assertEquals("Build metadata must use the same version as the registered plugin descriptor",
                descriptor.getVersion(), DocViewBuildInfo.version());
    }
}
