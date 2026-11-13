package com.liuzhihang.doc.view.service.skill;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.progress.ProgressIndicator;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/** Bounded, isolated probes for the selected Codex executable. */
final class CodexClientProbe {
    static final String SUPPORTED_VERSION = "codex-cli 0.167.0-alpha.4.1";
    private static final int MAX_VERSION_OUTPUT = 4096;
    private static final int MAX_SERVER_OUTPUT = 1024 * 1024;
    private static final long VERSION_TIMEOUT_MILLIS = 3_000;
    private static final long SERVER_TIMEOUT_MILLIS = 15_000;

    record Discovery(boolean discovered, String discoveredPath) {}

    static boolean isVerifiedHost() {
        return isVerifiedHost(System.getProperty("os.name", ""), System.getProperty("os.arch", ""));
    }

    static boolean isVerifiedHost(String osName, String architecture) {
        String os = osName == null ? "" : osName.toLowerCase(java.util.Locale.ROOT);
        String arch = architecture == null ? "" : architecture.toLowerCase(java.util.Locale.ROOT);
        return (os.contains("mac") || os.contains("darwin")) && (arch.equals("aarch64") || arch.equals("arm64"));
    }

    String version(Path executable, Path projectRoot, ProgressIndicator indicator) throws IOException {
        Path command = checkedExecutable(executable);
        Path temporaryHome = Files.createTempDirectory("doc-view-codex-version-");
        Path isolatedCodexHome = temporaryHome.resolve("codex-home");
        Files.createDirectory(isolatedCodexHome);
        Process process = null;
        try {
            process = start(List.of(command.toString(), "--version"), projectRoot, temporaryHome, isolatedCodexHome);
            byte[] output = awaitOutput(process, MAX_VERSION_OUTPUT, VERSION_TIMEOUT_MILLIS, indicator);
            if (process.exitValue() != 0) {
                throw new IOException("Codex --version exited unsuccessfully");
            }
            String value = new String(output, StandardCharsets.UTF_8).strip();
            if (value.isBlank() || value.length() > MAX_VERSION_OUTPUT - 1 || value.contains("\n")) {
                throw new IOException("Codex --version returned an invalid response");
            }
            return value;
        } finally {
            if (process != null) process.destroyForcibly();
            deleteTemporaryTree(temporaryHome);
        }
    }

    Discovery discover(Path executable, Path projectRoot, Path expectedSkillFile,
                       ProgressIndicator indicator) throws IOException {
        return discover(executable, projectRoot,
                new CodexSkillTarget(CodexSkillScope.PROJECT, projectRoot,
                        expectedSkillFile.toAbsolutePath().normalize().getParent()), indicator);
    }

    Discovery discover(Path executable, Path projectRoot, CodexSkillTarget target,
                       ProgressIndicator indicator) throws IOException {
        Path command = checkedExecutable(executable);
        Path temporaryHome = Files.createTempDirectory("doc-view-codex-probe-");
        Path isolatedCodexHome = temporaryHome.resolve("codex-home");
        Files.createDirectory(isolatedCodexHome);
        Path processHome = target.scope() == CodexSkillScope.USER ? target.ownerRoot() : temporaryHome;
        Process process = null;
        try {
            process = start(List.of(command.toString(), "app-server", "--stdio"), projectRoot, processHome,
                    isolatedCodexHome);
            OutputStream input = process.getOutputStream();
            input.write(initializeRequest().getBytes(StandardCharsets.UTF_8));
            input.flush();
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            String firstResponse = readResponseLine(process, output, 0, indicator);
            if (firstResponse == null) {
                throw new IOException("Codex app-server did not complete initialize");
            }
            input.write("{\"method\":\"initialized\",\"params\":{}}\n".getBytes(StandardCharsets.UTF_8));
            input.write(skillsListRequest(projectRoot).getBytes(StandardCharsets.UTF_8));
            input.flush();
            String response = readResponseLine(process, output, 1, indicator);
            if (response == null) {
                throw new IOException("Codex app-server did not complete skills/list");
            }
            input.close();
            byte[] accumulated = output.toByteArray();
            String all = new String(accumulated, StandardCharsets.UTF_8) + response + "\n";
            String wantedPath = target.targetPath().resolve("SKILL.md").toAbsolutePath().normalize().toString();
            String wantedScope = target.scope() == CodexSkillScope.USER ? "user" : "repo";
            for (String line : all.split("\\R")) {
                try {
                    JsonObject root = JsonParser.parseString(line).getAsJsonObject();
                    if (!root.has("id") || root.get("id").getAsInt() != 1 || !root.has("result")) continue;
                    var data = root.getAsJsonObject("result").getAsJsonArray("data");
                    for (var dataItem : data) {
                        var skills = dataItem.getAsJsonObject().getAsJsonArray("skills");
                        for (var skillItem : skills) {
                            JsonObject skill = skillItem.getAsJsonObject();
                            if (matchesExpectedSkill(skill, wantedPath, wantedScope)) {
                                return new Discovery(true, wantedPath);
                            }
                        }
                    }
                } catch (RuntimeException ignored) {
                    // app-server can emit non-response JSON notifications; only exact result records count.
                }
            }
            return new Discovery(false, "");
        } finally {
            if (process != null) {
                process.destroyForcibly();
                try {
                    process.waitFor(1, TimeUnit.SECONDS);
                } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                }
            }
            deleteTemporaryTree(temporaryHome);
        }
    }

    static boolean matchesExpectedSkill(JsonObject skill, String expectedPath, String expectedScope) {
        return skill.has("path") && expectedPath.equals(skill.get("path").getAsString())
                && skill.has("scope") && expectedScope.equals(skill.get("scope").getAsString())
                && skill.has("name") && "doc-view".equals(skill.get("name").getAsString());
    }

    static Path checkedExecutable(Path executable) throws IOException {
        if (executable == null || !executable.isAbsolute()) {
            throw new IOException("Choose an absolute path to the Codex executable");
        }
        Path real = executable.toRealPath();
        if (!Files.isRegularFile(real, LinkOption.NOFOLLOW_LINKS) || !Files.isExecutable(real)) {
            throw new IOException("The selected Codex executable is not an executable regular file");
        }
        return real;
    }

    private static Process start(List<String> command, Path projectRoot, Path temporaryHome) throws IOException {
        return start(command, projectRoot, temporaryHome, temporaryHome == null ? null : temporaryHome.resolve("codex-home"));
    }

    private static Process start(List<String> command, Path projectRoot, Path temporaryHome,
                                 Path isolatedCodexHome) throws IOException {
        ProcessBuilder builder = new ProcessBuilder(command);
        builder.directory(projectRoot.toFile());
        builder.redirectError(ProcessBuilder.Redirect.DISCARD);
        Map<String, String> environment = builder.environment();
        environment.clear();
        environment.put("PATH", System.getenv().getOrDefault("PATH", "/usr/bin:/bin"));
        if (temporaryHome != null) {
            environment.put("HOME", temporaryHome.toString());
            environment.put("CODEX_HOME", isolatedCodexHome.toString());
        }
        environment.put("TMPDIR", System.getProperty("java.io.tmpdir", "/tmp"));
        environment.put("LANG", "en_US.UTF-8");
        return builder.start();
    }

    private static byte[] awaitOutput(Process process, int maximum, long timeoutMillis,
                                      ProgressIndicator indicator) throws IOException {
        long deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(timeoutMillis);
        try (InputStream stream = process.getInputStream(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[512];
            while (System.nanoTime() < deadline) {
                indicator.checkCanceled();
                if (stream.available() > 0) {
                    int count = stream.read(buffer, 0, Math.min(buffer.length, maximum + 1 - output.size()));
                    if (count < 0) break;
                    output.write(buffer, 0, count);
                    if (output.size() > maximum) throw new IOException("Codex version response exceeded its limit");
                } else if (!process.isAlive()) {
                    break;
                } else {
                    pause(indicator);
                }
            }
            if (process.isAlive()) throw new IOException("Codex --version timed out");
            while (stream.available() > 0) {
                int count = stream.read(buffer, 0, Math.min(buffer.length, maximum + 1 - output.size()));
                if (count < 0) break;
                output.write(buffer, 0, count);
                if (output.size() > maximum) throw new IOException("Codex version response exceeded its limit");
            }
            return output.toByteArray();
        }
    }

    private static String readJsonLine(Process process, ByteArrayOutputStream captured, int maximum,
                                       long timeoutMillis, ProgressIndicator indicator) throws IOException {
        long deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(timeoutMillis);
        ByteArrayOutputStream line = new ByteArrayOutputStream();
        InputStream stream = process.getInputStream();
        byte[] one = new byte[1];
        while (System.nanoTime() < deadline) {
            indicator.checkCanceled();
            if (stream.available() > 0) {
                int count = stream.read(one);
                if (count < 0) return null;
                if (captured.size() >= maximum) throw new IOException("Codex app-server response exceeded its limit");
                captured.write(one[0]);
                if (one[0] == '\n') return line.toString(StandardCharsets.UTF_8).strip();
                if (line.size() >= 64 * 1024) throw new IOException("Codex app-server response line exceeded its limit");
                line.write(one[0]);
            } else if (!process.isAlive()) {
                return line.size() == 0 ? null : line.toString(StandardCharsets.UTF_8).strip();
            } else {
                pause(indicator);
            }
        }
        throw new IOException("Codex app-server request timed out");
    }

    private static boolean responseId(String json, int expectedId) {
        try {
            JsonObject root = JsonParser.parseString(json).getAsJsonObject();
            return root.has("id") && root.get("id").getAsInt() == expectedId && root.has("result");
        } catch (RuntimeException invalid) {
            return false;
        }
    }

    private static String readResponseLine(Process process, ByteArrayOutputStream captured, int expectedId,
                                           ProgressIndicator indicator) throws IOException {
        while (true) {
            String line = readJsonLine(process, captured, MAX_SERVER_OUTPUT, SERVER_TIMEOUT_MILLIS, indicator);
            if (line == null) return null;
            if (responseId(line, expectedId)) return line;
        }
    }

    private static String initializeRequest() {
        JsonObject client = new JsonObject();
        client.addProperty("name", "doc-view-skill-check");
        client.addProperty("title", "Doc View isolated skill check");
        client.addProperty("version", SUPPORTED_VERSION);
        JsonObject params = new JsonObject();
        params.add("clientInfo", client);
        JsonObject request = new JsonObject();
        request.addProperty("method", "initialize");
        request.addProperty("id", 0);
        request.add("params", params);
        return request + "\n";
    }

    private static String skillsListRequest(Path projectRoot) {
        JsonArray cwds = new JsonArray();
        cwds.add(projectRoot.toString());
        JsonObject params = new JsonObject();
        params.add("cwds", cwds);
        params.addProperty("forceReload", true);
        JsonObject request = new JsonObject();
        request.addProperty("method", "skills/list");
        request.addProperty("id", 1);
        request.add("params", params);
        return request + "\n";
    }

    private static void pause(ProgressIndicator indicator) {
        indicator.checkCanceled();
        try {
            Thread.sleep(20);
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw new ProcessCanceledException();
        }
    }

    private static void deleteTemporaryTree(Path root) {
        try (var paths = Files.walk(root)) {
            paths.sorted(java.util.Comparator.reverseOrder()).forEach(path -> {
                try { Files.deleteIfExists(path); } catch (IOException ignored) { }
            });
        } catch (IOException ignored) { }
    }
}
