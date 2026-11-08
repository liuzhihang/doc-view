package com.liuzhihang.doc.view.contract;

import com.intellij.ide.plugins.DynamicPluginListener;
import com.intellij.ide.plugins.IdeaPluginDescriptor;
import com.intellij.openapi.extensions.PluginId;
import com.intellij.openapi.util.IconLoader;
import icons.DocViewIcons;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.swing.Icon;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.InputStream;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;

public final class PluginUnloadIconCacheTest extends com.intellij.testFramework.fixtures.LightJavaCodeInsightFixtureTestCase {

    public void testOwnPluginUnloadListenerIsRegistered() throws Exception {
        assertNotNull("The plugin descriptor must register an own-unload lifecycle listener", registeredListener());
    }

    public void testOwnUnloadClearsThePlatformDisabledIconCache() throws Exception {
        Icon source = DocViewIcons.DOC_VIEW;
        Icon beforeUnload = IconLoader.getDisabledIcon(source);
        assertSame("The platform should return its cached disabled icon before unload",
                beforeUnload, IconLoader.getDisabledIcon(source));

        IdeaPluginDescriptor descriptor = descriptor("com.liuzhihang.doc-view");
        registeredListener().beforePluginUnload(descriptor, false);

        Icon afterUnload = IconLoader.getDisabledIcon(source);
        assertNotSame("The Doc View before-unload callback must invalidate the disabled-icon cache",
                beforeUnload, afterUnload);
    }

    public void testUnloadingAnotherPluginDoesNotClearThePlatformCache() throws Exception {
        Icon source = DocViewIcons.DOC_VIEW;
        Icon beforeUnload = IconLoader.getDisabledIcon(source);

        registeredListener().beforePluginUnload(descriptor("org.example.other-plugin"), false);

        assertSame("Unloading another plugin must not invalidate the shared icon cache",
                beforeUnload, IconLoader.getDisabledIcon(source));
    }

    @Override
    protected void tearDown() throws Exception {
        try {
            IconLoader.clearCache();
        } finally {
            super.tearDown();
        }
    }

    private DynamicPluginListener registeredListener() throws Exception {
        Document document;
        try (InputStream descriptor = getClass().getClassLoader().getResourceAsStream("META-INF/plugin.xml")) {
            assertNotNull("The plugin descriptor must be available to the test", descriptor);
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            document = factory.newDocumentBuilder().parse(descriptor);
        }

        NodeList listeners = document.getElementsByTagName("listener");
        for (int i = 0; i < listeners.getLength(); i++) {
            Element element = (Element) listeners.item(i);
            if ("com.liuzhihang.doc.view.provider.DocViewPluginLifecycleListener"
                    .equals(element.getAttribute("class"))) {
                assertEquals("com.intellij.ide.plugins.DynamicPluginListener", element.getAttribute("topic"));
                Class<?> listenerClass = Class.forName(element.getAttribute("class"));
                return (DynamicPluginListener) listenerClass.getConstructor().newInstance();
            }
        }
        return null;
    }

    private IdeaPluginDescriptor descriptor(String pluginId) {
        InvocationHandler handler = (proxy, method, args) -> {
            if ("getPluginId".equals(method.getName())) {
                return PluginId.getId(pluginId);
            }
            if ("toString".equals(method.getName())) {
                return "Test plugin descriptor " + pluginId;
            }
            throw new UnsupportedOperationException(method.getName());
        };
        return (IdeaPluginDescriptor) Proxy.newProxyInstance(
                IdeaPluginDescriptor.class.getClassLoader(), new Class<?>[]{IdeaPluginDescriptor.class}, handler);
    }
}
