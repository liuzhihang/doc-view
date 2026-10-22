package com.liuzhihang.doc.view.contract;

import junit.framework.TestCase;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Properties;
import javax.xml.parsers.DocumentBuilderFactory;

public final class PluginDescriptorLocalizationTest extends TestCase {

    public void testPluginDescriptorUsesTheLocalizationBundleForVisibleLabels() throws Exception {
        Document descriptor = readDescriptor();
        Properties base = loadProperties("messages/DocViewBundle.properties");
        Properties simplifiedChinese = loadProperties("messages/DocViewBundle_zh_CN.properties");

        NodeList actions = descriptor.getElementsByTagName("action");
        NodeList groups = descriptor.getElementsByTagName("group");
        int localizedActionAttributes = assertActionSystemKeys(actions, "action", base, simplifiedChinese);
        localizedActionAttributes += assertActionSystemKeys(groups, "group", base, simplifiedChinese);
        assertTrue("Visible ActionSystem entries must use convention bundle keys", localizedActionAttributes > 10);

        NodeList configurables = descriptor.getElementsByTagName("projectConfigurable");
        assertTrue("Project settings pages should remain declared", configurables.getLength() > 0);
        for (int index = 0; index < configurables.getLength(); index++) {
            Element configurable = (Element) configurables.item(index);
            assertFalse("ConfigurableEP must resolve its name through bundle/key",
                    configurable.hasAttribute("displayName"));
            assertEquals("messages.DocViewBundle", configurable.getAttribute("bundle"));
            String key = configurable.getAttribute("key");
            assertFalse("ConfigurableEP requires a localization key", key.isBlank());
            assertNotNull("Missing English configurable message " + key, base.getProperty(key));
            assertNotNull("Missing zh_CN configurable message " + key, simplifiedChinese.getProperty(key));
        }
    }

    private static int assertActionSystemKeys(NodeList entries, String namespace,
                                              Properties base, Properties simplifiedChinese) {
        int localizedAttributes = 0;
        for (int index = 0; index < entries.getLength(); index++) {
            Element entry = (Element) entries.item(index);
            String id = entry.getAttribute("id");
            if (id.isBlank()) continue;

            for (String attribute : new String[]{"text", "description"}) {
                if (!entry.hasAttribute(attribute)) continue;
                String key = namespace + "." + id + "." + attribute;
                String english = base.getProperty(key);
                assertNotNull("Missing English ActionSystem convention message " + key, english);
                assertNotNull("Missing zh_CN ActionSystem convention message " + key,
                        simplifiedChinese.getProperty(key));
                assertEquals("XML fallback must remain readable English for " + key,
                        english, entry.getAttribute(attribute));
                localizedAttributes++;
            }
        }
        return localizedAttributes;
    }

    private static Document readDescriptor() throws Exception {
        try (InputStream stream = PluginDescriptorLocalizationTest.class.getClassLoader()
                .getResourceAsStream("META-INF/plugin.xml")) {
            if (stream == null) throw new AssertionError("Missing resource META-INF/plugin.xml");
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            return factory.newDocumentBuilder().parse(stream);
        }
    }

    private static Properties loadProperties(String name) throws IOException {
        try (InputStream stream = PluginDescriptorLocalizationTest.class.getClassLoader().getResourceAsStream(name)) {
            if (stream == null) throw new AssertionError("Missing localization resource " + name);
            Properties properties = new Properties();
            properties.load(new InputStreamReader(stream, StandardCharsets.UTF_8));
            return properties;
        }
    }
}
