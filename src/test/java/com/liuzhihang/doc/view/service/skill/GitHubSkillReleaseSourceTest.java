package com.liuzhihang.doc.view.service.skill;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.progress.ProgressIndicator;
import com.sun.net.httpserver.HttpServer;
import org.junit.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

public final class GitHubSkillReleaseSourceTest {
    private static final String COMMIT = "0123456789012345678901234567890123456789";
    private static final String TREE_SHA = "eeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeee";

    @Test
    public void verifiesEveryManifestFileAgainstTheTaggedGitTree() throws Exception {
        Map<String, byte[]> files = files("skill text", "guide text");
        SkillFixtureFactory.Fixture fixture = SkillFixtureFactory.fixture("1.7.0", files);

        GitHubSkillReleaseSource.verifyArchiveMatchesCommit(
                fixture.manifest(), fixture.archive(), commit(COMMIT, TREE_SHA), tree(TREE_SHA, files));
    }

    @Test
    public void rejectsManifestWhoseSelfDeclaredHashesDoNotMatchTheCommit() throws Exception {
        Map<String, byte[]> expectedFiles = files("release skill", "release guide");
        SkillFixtureFactory.Fixture fixture = SkillFixtureFactory.fixture("1.7.0", expectedFiles);
        Map<String, byte[]> otherCommitFiles = files("different skill", "release guide");

        assertThrows(IOException.class, () -> GitHubSkillReleaseSource.verifyArchiveMatchesCommit(
                fixture.manifest(), fixture.archive(), commit(COMMIT, TREE_SHA), tree(TREE_SHA, otherCommitFiles)));
    }

    @Test
    public void rejectsMissingAndExtraSourceFiles() throws Exception {
        Map<String, byte[]> files = files("skill", "guide");
        SkillFixtureFactory.Fixture fixture = SkillFixtureFactory.fixture("1.7.0", files);

        assertThrows(IOException.class, () -> GitHubSkillReleaseSource.verifyArchiveMatchesCommit(
                fixture.manifest(), fixture.archive(), commit(COMMIT, TREE_SHA),
                tree(TREE_SHA, Map.of("SKILL.md", files.get("SKILL.md")))));
        Map<String, byte[]> extra = new LinkedHashMap<>(files);
        extra.put("unexpected.md", "extra".getBytes(StandardCharsets.UTF_8));
        assertThrows(IOException.class, () -> GitHubSkillReleaseSource.verifyArchiveMatchesCommit(
                fixture.manifest(), fixture.archive(), commit(COMMIT, TREE_SHA), tree(TREE_SHA, extra)));
    }

    @Test
    public void rejectsWrongCommitTruncatedTreesAndNonRegularSkillFiles() throws Exception {
        Map<String, byte[]> files = files("skill", "guide");
        SkillFixtureFactory.Fixture fixture = SkillFixtureFactory.fixture("1.7.0", files);

        assertThrows(IOException.class, () -> GitHubSkillReleaseSource.verifyArchiveMatchesCommit(
                fixture.manifest(), fixture.archive(), commit("f".repeat(40), TREE_SHA), tree(TREE_SHA, files)));
        JsonObject truncated = treeObject(TREE_SHA, files);
        truncated.addProperty("truncated", true);
        assertThrows(IOException.class, () -> GitHubSkillReleaseSource.verifyArchiveMatchesCommit(
                fixture.manifest(), fixture.archive(), commit(COMMIT, TREE_SHA), truncated));
        JsonObject symlink = treeObject(TREE_SHA, files);
        JsonObject skillEntry = null;
        for (var entry : symlink.getAsJsonArray("tree")) {
            JsonObject candidate = entry.getAsJsonObject();
            if ((SkillReleaseManifest.SOURCE_PATH + "SKILL.md").equals(candidate.get("path").getAsString())) {
                skillEntry = candidate;
                break;
            }
        }
        assertNotNull("Fixture must include the manifest SKILL.md source blob", skillEntry);
        skillEntry.addProperty("mode", "120000");
        assertThrows(IOException.class, () -> GitHubSkillReleaseSource.verifyArchiveMatchesCommit(
                fixture.manifest(), fixture.archive(), commit(COMMIT, TREE_SHA), symlink));
        assertThrows(IOException.class, () -> GitHubSkillReleaseSource.verifyArchiveMatchesCommit(
                fixture.manifest(), fixture.archive(), commit(COMMIT, "f".repeat(40)), tree(TREE_SHA, files)));
    }

    @Test
    public void computesTheGitBlobObjectIdentityIncludingItsTypeAndLengthHeader() throws Exception {
        assertEquals("b6fc4c620b67d95f953a5c1c1230aaab5db5a1b0",
                GitHubSkillReleaseSource.gitBlobSha1("hello".getBytes(StandardCharsets.UTF_8)));
    }

    @Test(timeout = 10000)
    public void cancelsPendingHttpClientBodyTransferWhenTheProgressIndicatorIsCancelled() throws Exception {
        CountDownLatch bodyStarted = new CountDownLatch(1);
        CountDownLatch releaseBody = new CountDownLatch(1);
        CountDownLatch responseFinished = new CountDownLatch(1);
        AtomicBoolean serverObservedClientDisconnect = new AtomicBoolean();
        int responseSize = 8 * 1024 * 1024;
        CompletableFuture<HttpResponse<byte[]>> response;
        HttpServer server = HttpServer.create(new InetSocketAddress(InetAddress.getByName("127.0.0.1"), 0), 0);
        server.createContext("/slow-release-asset", exchange -> {
            try {
                exchange.sendResponseHeaders(200, responseSize);
                try (OutputStream body = exchange.getResponseBody()) {
                    body.write(new byte[8192]);
                    body.flush();
                    bodyStarted.countDown();
                    try {
                        if (!releaseBody.await(5, TimeUnit.SECONDS)) return;
                    } catch (InterruptedException interrupted) {
                        Thread.currentThread().interrupt();
                        return;
                    }
                    for (int offset = 8192; offset < responseSize; offset += 8192) {
                        body.write(new byte[8192]);
                    }
                }
            } catch (IOException clientCancelledTransfer) {
                serverObservedClientDisconnect.set(true);
            } finally {
                responseFinished.countDown();
                exchange.close();
            }
        });
        server.start();
        try {
            URI uri = URI.create("http://127.0.0.1:" + server.getAddress().getPort() + "/slow-release-asset");
            HttpRequest request = HttpRequest.newBuilder(uri).GET().build();
            response = HttpClient.newHttpClient().sendAsync(
                    request, HttpResponse.BodyHandlers.ofByteArray());
            assertTrue("The loopback server must begin streaming its response", bodyStarted.await(5, TimeUnit.SECONDS));
            assertFalse("The body transfer must still be in flight before cancellation", response.isDone());

            AtomicInteger cancellationChecks = new AtomicInteger();
            ProgressIndicator indicator = (ProgressIndicator) java.lang.reflect.Proxy.newProxyInstance(
                    ProgressIndicator.class.getClassLoader(), new Class<?>[]{ProgressIndicator.class},
                    (proxy, method, arguments) -> {
                        if (method.getName().equals("checkCanceled") && cancellationChecks.incrementAndGet() == 2) {
                            throw new ProcessCanceledException();
                        }
                        Class<?> returnType = method.getReturnType();
                        if (returnType == boolean.class) return false;
                        if (returnType == int.class) return 0;
                        if (returnType == long.class) return 0L;
                        if (returnType == double.class) return 0.0;
                        return null;
                    });
            assertThrows(ProcessCanceledException.class,
                    () -> GitHubSkillReleaseSource.awaitHttpResponse(response, indicator));
        } finally {
            releaseBody.countDown();
            try {
                if (bodyStarted.getCount() == 0) {
                    assertTrue("The bounded loopback handler must finish after releasing the body",
                            responseFinished.await(5, TimeUnit.SECONDS));
                }
            } finally {
                server.stop(0);
            }
        }
        Throwable transferFailure = response.handle((value, failure) -> failure).get(5, TimeUnit.SECONDS);
        assertTrue("The cancelled HTTP future must settle exceptionally", response.isCompletedExceptionally());
        assertNotNull("A cancelled HTTP body must not return a successful response", transferFailure);
        assertTrue("The loopback server must observe the client disconnect while streaming the remaining body",
                serverObservedClientDisconnect.get());
    }

    private static Map<String, byte[]> files(String skill, String guide) {
        Map<String, byte[]> files = new LinkedHashMap<>();
        files.put("SKILL.md", skill.getBytes(StandardCharsets.UTF_8));
        files.put("references/guide.md", guide.getBytes(StandardCharsets.UTF_8));
        return files;
    }

    private static JsonObject commit(String commit, String treeSha) {
        JsonObject response = new JsonObject();
        response.addProperty("sha", commit);
        JsonObject tree = new JsonObject();
        tree.addProperty("sha", treeSha);
        response.add("tree", tree);
        return response;
    }

    private static JsonObject tree(String treeSha, Map<String, byte[]> files) throws Exception {
        return treeObject(treeSha, files);
    }

    private static JsonObject treeObject(String treeSha, Map<String, byte[]> files) throws Exception {
        JsonObject response = new JsonObject();
        response.addProperty("sha", treeSha);
        response.addProperty("truncated", false);
        JsonArray tree = new JsonArray();
        JsonObject outside = new JsonObject();
        outside.addProperty("path", "README.md");
        outside.addProperty("mode", "100644");
        outside.addProperty("type", "blob");
        outside.addProperty("sha", GitHubSkillReleaseSource.gitBlobSha1("outside".getBytes(StandardCharsets.UTF_8)));
        tree.add(outside);
        JsonObject directory = new JsonObject();
        directory.addProperty("path", "skills/doc-view/references");
        directory.addProperty("mode", "040000");
        directory.addProperty("type", "tree");
        directory.addProperty("sha", "e".repeat(40));
        tree.add(directory);
        for (var entry : files.entrySet()) {
            JsonObject blob = new JsonObject();
            blob.addProperty("path", SkillReleaseManifest.SOURCE_PATH + entry.getKey());
            blob.addProperty("mode", "100644");
            blob.addProperty("type", "blob");
            blob.addProperty("sha", GitHubSkillReleaseSource.gitBlobSha1(entry.getValue()));
            tree.add(blob);
        }
        response.add("tree", tree);
        return response;
    }
}
