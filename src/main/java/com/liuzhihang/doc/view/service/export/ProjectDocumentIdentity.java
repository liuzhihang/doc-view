package com.liuzhihang.doc.view.service.export;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;

/**
 * Stable identity for one documented Java method in one project module.
 */
public final class ProjectDocumentIdentity implements Comparable<ProjectDocumentIdentity> {

    private final String moduleName;
    private final String sourcePath;
    private final String qualifiedClassName;
    private final String methodName;
    private final List<String> parameterTypes;
    private final byte[] canonicalBytes;
    private final String canonicalForm;
    private final String sha256;

    private ProjectDocumentIdentity(String moduleName,
                                    String sourcePath,
                                    String qualifiedClassName,
                                    String methodName,
                                    List<String> parameterTypes) {
        this.moduleName = requireText(moduleName, "moduleName");
        this.sourcePath = normalizeSourcePath(sourcePath);
        this.qualifiedClassName = requireText(qualifiedClassName, "qualifiedClassName");
        this.methodName = requireText(methodName, "methodName");
        this.parameterTypes = List.copyOf(Objects.requireNonNull(parameterTypes, "parameterTypes"));
        if (this.parameterTypes.stream().anyMatch(type -> type == null || type.isBlank())) {
            throw new IllegalArgumentException("parameterTypes must not contain blank values");
        }
        this.canonicalBytes = encodeCanonicalIdentity();
        this.canonicalForm = java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(canonicalBytes);
        this.sha256 = sha256(canonicalBytes);
    }

    public static ProjectDocumentIdentity of(String moduleName,
                                             String sourcePath,
                                             String qualifiedClassName,
                                             String methodName,
                                             List<String> parameterTypes) {
        return new ProjectDocumentIdentity(moduleName, sourcePath, qualifiedClassName, methodName, parameterTypes);
    }

    public String moduleName() {
        return moduleName;
    }

    public String sourcePath() {
        return sourcePath;
    }

    public String qualifiedClassName() {
        return qualifiedClassName;
    }

    public String methodName() {
        return methodName;
    }

    public List<String> parameterTypes() {
        return parameterTypes;
    }

    /**
     * An unambiguous, reversible encoding of every identity field for ownership markers.
     */
    public String canonicalForm() {
        return canonicalForm;
    }

    public String sha256() {
        return sha256;
    }

    public String displaySignature() {
        return methodName + "(" + String.join(", ", parameterTypes) + ")";
    }

    public String pageFileName() {
        String className = qualifiedClassName.substring(Math.max(qualifiedClassName.lastIndexOf('.'),
                qualifiedClassName.lastIndexOf('$')) + 1);
        return slug(className) + "-" + slug(methodName) + "-" + sha256 + ".md";
    }

    @Override
    public int compareTo(ProjectDocumentIdentity other) {
        return canonicalForm.compareTo(other.canonicalForm);
    }

    private byte[] encodeCanonicalIdentity() {
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            try (DataOutputStream output = new DataOutputStream(bytes)) {
                output.writeInt(1); // identity encoding version
                writeString(output, moduleName);
                writeString(output, sourcePath);
                writeString(output, qualifiedClassName);
                writeString(output, methodName);
                output.writeInt(parameterTypes.size());
                for (String parameterType : parameterTypes) {
                    writeString(output, parameterType);
                }
            }
            return bytes.toByteArray();
        } catch (IOException impossible) {
            throw new IllegalStateException("Could not encode project document identity", impossible);
        }
    }

    private static void writeString(DataOutputStream output, String value) throws IOException {
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        output.writeInt(bytes.length);
        output.write(bytes);
    }

    private static String normalizeSourcePath(String value) {
        String path = requireText(value, "sourcePath").replace('\\', '/');
        if (path.startsWith("/") || path.matches("^[A-Za-z]:/.*")) {
            throw new IllegalArgumentException("sourcePath must be project-relative");
        }
        List<String> normalized = new ArrayList<>();
        for (String component : path.split("/")) {
            if (component.isEmpty() || component.equals(".")) {
                continue;
            }
            if (component.equals("..")) {
                throw new IllegalArgumentException("sourcePath must not escape the project root");
            }
            normalized.add(component);
        }
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("sourcePath must identify a project file");
        }
        return String.join("/", normalized);
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value;
    }

    private static String slug(String value) {
        String slug = value.replaceAll("[^A-Za-z0-9._-]+", "-").replaceAll("-+", "-");
        while (slug.startsWith(".")) {
            slug = slug.substring(1);
        }
        if (slug.isBlank()) {
            return "api";
        }
        return slug.length() > 48 ? slug.substring(0, 48) : slug;
    }

    private static String sha256(byte[] bytes) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("SHA-256 is unavailable", impossible);
        }
    }
}
