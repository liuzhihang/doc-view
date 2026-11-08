package com.liuzhihang.doc.view;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/** Build metadata expanded from the same version property as the plugin descriptor. */
public final class DocViewBuildInfo {
    private static final String VERSION = readVersion();

    private DocViewBuildInfo() {}

    public static String version() {
        return VERSION;
    }

    private static String readVersion() {
        try (InputStream input = DocViewBuildInfo.class.getResourceAsStream("/META-INF/doc-view-build.properties")) {
            if (input == null) {
                throw new IllegalStateException("Doc View build metadata is unavailable");
            }
            Properties metadata = new Properties();
            metadata.load(input);
            String version = metadata.getProperty("plugin.version");
            if (!"com.liuzhihang.doc-view".equals(metadata.getProperty("plugin.id"))
                    || version == null || version.isBlank() || version.contains("${")) {
                throw new IllegalStateException("Doc View build metadata is invalid");
            }
            return version;
        } catch (IOException failure) {
            throw new IllegalStateException("Doc View build metadata cannot be read", failure);
        }
    }
}
