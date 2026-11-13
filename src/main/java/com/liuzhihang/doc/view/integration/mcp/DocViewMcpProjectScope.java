package com.liuzhihang.doc.view.integration.mcp;

import com.google.gson.JsonObject;
import com.intellij.openapi.project.Project;
import java.io.IOException;
import java.nio.file.Path;

/** Explicit arguments or the host-resolved call project; never the focused window. */
final class DocViewMcpProjectScope {
    record Selection(Project project, JsonObject arguments) {}
    static Selection resolve(JsonObject arguments, Project routed, Project[] openProjects) {
        JsonObject normalized = arguments.deepCopy();
        String path;
        if (!normalized.has("projectPath") && routed != null && !routed.isDisposed()) {
            path = routed.getBasePath();
        } else {
            var value = normalized.get("projectPath");
            if (value == null || !value.isJsonPrimitive() || !value.getAsJsonPrimitive().isString()) {
                throw new IllegalArgumentException("INVALID_ARGUMENT");
            }
            path = value.getAsString();
        }
        Path target = canonical(path);
        Project match = null;
        for (Project candidate : openProjects) {
            if (candidate.isDisposed() || candidate.getBasePath() == null) continue;
            Path candidatePath;
            try { candidatePath = canonical(candidate.getBasePath()); }
            catch (IllegalArgumentException unavailable) { continue; }
            if (target.equals(candidatePath)) {
                if (match != null && match != candidate) throw new IllegalArgumentException("PROJECT_NOT_OPEN");
                match = candidate;
            }
        }
        if (match == null || (routed != null && match != routed)) {
            throw new IllegalArgumentException("PROJECT_NOT_OPEN");
        }
        normalized.addProperty("projectPath", target.toString());
        return new Selection(match, normalized);
    }
    private static Path canonical(String path) {
        if (path == null || path.isBlank() || path.length() > 4096) throw new IllegalArgumentException("INVALID_ARGUMENT");
        try {
            Path value = Path.of(path);
            if (!value.isAbsolute() || !value.normalize().equals(value)) {
                throw new IllegalArgumentException("INVALID_ARGUMENT");
            }
            return value.toRealPath();
        } catch (java.nio.file.InvalidPathException failure) {
            throw new IllegalArgumentException("INVALID_ARGUMENT");
        } catch (IOException failure) {
            throw new IllegalArgumentException("PROJECT_NOT_OPEN");
        }
    }
}
