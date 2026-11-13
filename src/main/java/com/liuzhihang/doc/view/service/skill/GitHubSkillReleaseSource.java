package com.liuzhihang.doc.view.service.skill;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.progress.ProgressIndicator;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.HexFormat;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Flow;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.regex.Pattern;

/** Reads only public release metadata during update checks; archive bytes are fetched after an explicit click. */
final class GitHubSkillReleaseSource implements SkillReleaseSource {
    private static final String API_ROOT = "https://api.github.com/repos/liuzhihang/doc-view";
    private static final Pattern STABLE_TAG = Pattern.compile("^v(?:0|[1-9][0-9]*)\\.(?:0|[1-9][0-9]*)\\.(?:0|[1-9][0-9]*)$");
    private static final int MAX_RELEASES_JSON = 1024 * 1024;
    private static final int MAX_MANIFEST_JSON = 64 * 1024;
    private static final int MAX_GIT_OBJECT_JSON = 1024 * 1024;
    private static final int MAX_GITHUB_REDIRECTS = 4;
    private static final long HTTP_TIMEOUT_SECONDS = 12;
    private static final HttpClient CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(4))
            .followRedirects(HttpClient.Redirect.NEVER)
            .build();

    static final class OfflineException extends IOException {
        OfflineException(String message, Throwable cause) { super(message, cause); }
        OfflineException(String message) { super(message); }
    }

    @Override
    public Optional<Candidate> latest(ProgressIndicator indicator) throws IOException {
        byte[] bytes = get(URI.create(API_ROOT + "/releases?per_page=50"), MAX_RELEASES_JSON,
                "application/vnd.github+json", indicator);
        JsonArray releases;
        try {
            releases = JsonParser.parseString(new String(bytes, StandardCharsets.UTF_8)).getAsJsonArray();
        } catch (RuntimeException malformed) {
            throw new IOException("Official GitHub release metadata is malformed", malformed);
        }
        for (JsonElement value : releases) {
            indicator.checkCanceled();
            JsonObject release = value.getAsJsonObject();
            if (booleanValue(release, "draft") || booleanValue(release, "prerelease")) continue;
            String tag = stringValue(release, "tag_name");
            if (!STABLE_TAG.matcher(tag).matches()) continue;
            JsonArray assets = arrayValue(release, "assets");
            JsonObject manifestAsset = findAsset(assets, "doc-view-skill-" + tag.substring(1) + ".manifest.json");
            JsonObject archiveAsset = findAsset(assets, "doc-view-skill-" + tag.substring(1) + ".zip");
            if (manifestAsset == null && archiveAsset == null) continue;
            if (manifestAsset == null || archiveAsset == null) {
                throw new IOException("Official Doc View Skill release is missing a required archive or manifest");
            }
            long manifestSize = numberValue(manifestAsset, "size");
            if (manifestSize < 1 || manifestSize > MAX_MANIFEST_JSON) {
                throw new IOException("Official Skill manifest asset exceeds 64 KiB");
            }
            long manifestAssetId = numberValue(manifestAsset, "id");
            long archiveAssetId = numberValue(archiveAsset, "id");
            long archiveAssetSize = numberValue(archiveAsset, "size");
            if (manifestAssetId <= 0 || archiveAssetId <= 0 || archiveAssetSize < 1
                    || archiveAssetSize > SkillReleaseManifest.MAX_ARCHIVE_BYTES) {
                throw new IOException("Official Skill release asset metadata is invalid");
            }

            byte[] manifestBytes = get(URI.create(API_ROOT + "/releases/assets/" + manifestAssetId),
                    MAX_MANIFEST_JSON, "application/octet-stream", indicator);
            SkillReleaseManifest manifest = SkillReleaseManifest.parse(manifestBytes);
            if (!manifest.releaseTag().equals(tag) || manifest.archive().size() != archiveAssetSize) {
                throw new IOException("Skill manifest is not bound to the GitHub release assets");
            }
            String taggedCommit = resolveTagCommit(tag, indicator);
            if (!manifest.commitSha().equals(taggedCommit)) {
                throw new IOException("Skill manifest commit does not match its immutable Git tag");
            }
            return Optional.of(new Candidate(manifest, archiveAssetId));
        }
        return Optional.empty();
    }

    @Override
    public byte[] downloadArchive(Candidate candidate, ProgressIndicator indicator) throws IOException {
        URI uri = URI.create(API_ROOT + "/releases/assets/" + candidate.archiveAssetId());
        byte[] archive = get(uri, (int) SkillReleaseManifest.MAX_ARCHIVE_BYTES,
                "application/octet-stream", indicator);
        if (archive.length != candidate.manifest().archive().size()
                || !SkillReleaseManifest.sha256(archive).equals(candidate.manifest().archive().sha256())) {
            throw new IOException("Downloaded Skill archive does not match the official manifest");
        }
        // SHA-256 in a detached release manifest proves byte consistency, not that the bytes
        // came from the immutable commit named by that manifest. Bind every file to that commit.
        JsonObject commit = getJson(URI.create(API_ROOT + "/git/commits/" + candidate.manifest().commitSha()),
                MAX_GIT_OBJECT_JSON, indicator);
        JsonObject commitTree = objectValue(commit, "tree");
        String treeSha = gitObjectSha(stringValue(commitTree, "sha"));
        JsonObject tree = getJson(URI.create(API_ROOT + "/git/trees/" + treeSha + "?recursive=1"),
                MAX_GIT_OBJECT_JSON, indicator);
        verifyArchiveMatchesCommit(candidate.manifest(), archive, commit, tree);
        return archive;
    }

    /** Verifies ZIP bytes against the selected immutable Git commit, not only its detached manifest. */
    static void verifyArchiveMatchesCommit(SkillReleaseManifest manifest, byte[] archive,
                                           JsonObject commit, JsonObject tree) throws IOException {
        if (!manifest.commitSha().equals(gitObjectSha(stringValue(commit, "sha")))) {
            throw new IOException("Skill Git commit response does not match the manifest commit");
        }
        JsonObject commitTree = objectValue(commit, "tree");
        String expectedTreeSha = gitObjectSha(stringValue(commitTree, "sha"));
        if (!expectedTreeSha.equals(gitObjectSha(stringValue(tree, "sha")))) {
            throw new IOException("Skill Git tree response does not match the selected commit");
        }
        if (booleanValue(tree, "truncated")) {
            throw new IOException("Official Skill source tree is truncated and cannot be verified");
        }

        Map<String, byte[]> files = SkillPackageArchive.validate(archive, manifest);
        Map<String, String> sourceBlobs = new HashMap<>();
        Set<String> foldedPaths = new HashSet<>();
        JsonArray entries = arrayValue(tree, "tree");
        String prefix = SkillReleaseManifest.SOURCE_PATH;
        for (JsonElement value : entries) {
            if (!value.isJsonObject()) throw new IOException("Official Git tree contains an invalid entry");
            JsonObject entry = value.getAsJsonObject();
            String path = stringValue(entry, "path");
            if (!path.startsWith(prefix)) continue;
            String relative = path.substring(prefix.length());
            SkillReleaseManifest.validateMetadataPath(relative);
            String type = stringValue(entry, "type");
            if ("tree".equals(type)) {
                if (!"040000".equals(stringValue(entry, "mode"))) {
                    throw new IOException("Official Skill Git tree contains an invalid directory mode");
                }
                continue;
            }
            if (!"blob".equals(type) || !"100644".equals(stringValue(entry, "mode"))) {
                throw new IOException("Skill source contains a non-regular file: " + path);
            }
            if (!foldedPaths.add(relative.toLowerCase(Locale.ROOT))) {
                throw new IOException("Official Skill Git tree contains duplicate or case-colliding paths");
            }
            sourceBlobs.put(relative, gitObjectSha(stringValue(entry, "sha")));
        }

        Set<String> expectedPaths = new HashSet<>();
        for (SkillReleaseManifest.FileRecord file : manifest.files()) expectedPaths.add(file.path());
        if (!sourceBlobs.keySet().equals(expectedPaths) || !files.keySet().equals(expectedPaths)) {
            throw new IOException("Skill release files do not exactly match the source tree at the tagged commit");
        }
        for (String path : expectedPaths) {
            String expectedBlob = sourceBlobs.get(path);
            String actualBlob = gitBlobSha1(files.get(path));
            if (!expectedBlob.equals(actualBlob)) {
                throw new IOException("Skill release file does not match its tagged Git blob: " + path);
            }
        }
    }

    static String gitBlobSha1(byte[] content) throws IOException {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-1");
            digest.update(("blob " + content.length + "\0").getBytes(StandardCharsets.US_ASCII));
            return HexFormat.of().formatHex(digest.digest(content));
        } catch (NoSuchAlgorithmException impossible) {
            throw new IOException("SHA-1 is unavailable for Git object identity verification", impossible);
        }
    }

    private static String gitObjectSha(String sha) throws IOException {
        if (sha == null || !sha.matches("[a-f0-9]{40}")) {
            throw new IOException("Official GitHub response contains an unsupported Git object SHA");
        }
        return sha;
    }

    private String resolveTagCommit(String tag, ProgressIndicator indicator) throws IOException {
        JsonObject ref = getJson(URI.create(API_ROOT + "/git/ref/tags/" + tag), indicator);
        JsonObject object = objectValue(ref, "object");
        for (int depth = 0; depth < 4; depth++) {
            String type = stringValue(object, "type");
            String sha = stringValue(object, "sha");
            if (!sha.matches("[a-f0-9]{40}")) throw new IOException("GitHub tag object SHA is invalid");
            if ("commit".equals(type)) return sha;
            if (!"tag".equals(type)) throw new IOException("GitHub release tag does not resolve to a commit");
            JsonObject annotatedTag = getJson(URI.create(API_ROOT + "/git/tags/" + sha), indicator);
            object = objectValue(annotatedTag, "object");
        }
        throw new IOException("GitHub release tag has too many nested annotated tags");
    }

    private JsonObject getJson(URI uri, ProgressIndicator indicator) throws IOException {
        return getJson(uri, MAX_RELEASES_JSON, indicator);
    }

    private JsonObject getJson(URI uri, int maximumBytes, ProgressIndicator indicator) throws IOException {
        byte[] bytes = get(uri, maximumBytes, "application/vnd.github+json", indicator);
        try {
            return JsonParser.parseString(new String(bytes, StandardCharsets.UTF_8)).getAsJsonObject();
        } catch (RuntimeException malformed) {
            throw new IOException("Official GitHub metadata response is malformed", malformed);
        }
    }

    private byte[] get(URI initialUri, int maximumBytes, String accept, ProgressIndicator indicator) throws IOException {
        URI uri = initialUri;
        for (int redirects = 0; redirects <= MAX_GITHUB_REDIRECTS; redirects++) {
            validateUri(uri);
            indicator.checkCanceled();
            HttpRequest request = HttpRequest.newBuilder(uri)
                    .timeout(Duration.ofSeconds(HTTP_TIMEOUT_SECONDS))
                    .header("Accept", accept)
                    .header("User-Agent", "Doc-View-Skill-Manager")
                    .header("X-GitHub-Api-Version", "2022-11-28")
                    .GET().build();
            CompletableFuture<HttpResponse<byte[]>> future = CLIENT.sendAsync(request,
                    info -> new LimitedBodySubscriber(maximumBytes));
            HttpResponse<byte[]> response;
            try {
                while (true) {
                    indicator.checkCanceled();
                    try {
                        response = future.get(100, TimeUnit.MILLISECONDS);
                        break;
                    } catch (TimeoutException wait) {
                        // Polling keeps cancellation responsive while the network operation is pending.
                    }
                }
            } catch (ProcessCanceledException cancelled) {
                future.cancel(true);
                throw cancelled;
            } catch (InterruptedException interrupted) {
                future.cancel(true);
                Thread.currentThread().interrupt();
                throw new ProcessCanceledException();
            } catch (ExecutionException failed) {
                if (failed.getCause() instanceof BodyTooLargeException tooLarge) throw tooLarge;
                throw new OfflineException("Official GitHub release check failed", failed.getCause());
            }
            int status = response.statusCode();
            if (status == 301 || status == 302 || status == 303 || status == 307 || status == 308) {
                if (redirects == MAX_GITHUB_REDIRECTS) throw new IOException("Official GitHub asset redirected too many times");
                String location = response.headers().firstValue("location")
                        .orElseThrow(() -> new IOException("Official GitHub asset redirect has no target"));
                uri = uri.resolve(location);
                continue;
            }
            if (status == 403 || status == 429 || status >= 500) {
                throw new OfflineException("Official GitHub is temporarily unavailable (HTTP " + status + ")");
            }
            if (status != 200) throw new IOException("Official GitHub release request failed (HTTP " + status + ")");
            byte[] body = response.body();
            if (body.length > maximumBytes) throw new IOException("Official GitHub response exceeds its size limit");
            return body;
        }
        throw new IOException("Official GitHub asset redirect could not be resolved");
    }

    private static void validateUri(URI uri) throws IOException {
        String host = uri.getHost();
        if (!"https".equalsIgnoreCase(uri.getScheme()) || uri.getUserInfo() != null || uri.getPort() != -1 && uri.getPort() != 443
                || host == null || !List.of("api.github.com", "github.com", "objects.githubusercontent.com",
                "release-assets.githubusercontent.com").contains(host.toLowerCase(Locale.ROOT))) {
            throw new IOException("Skill release URL is outside the fixed official GitHub hosts");
        }
        if ("api.github.com".equalsIgnoreCase(host)
                && !uri.getPath().startsWith("/repos/liuzhihang/doc-view/")) {
            throw new IOException("Skill release API path is outside the official repository");
        }
        if ("github.com".equalsIgnoreCase(host)
                && !uri.getPath().startsWith("/liuzhihang/doc-view/releases/download/")) {
            throw new IOException("Skill release download path is outside the official repository");
        }
    }

    private static JsonObject findAsset(JsonArray assets, String expectedName) throws IOException {
        JsonObject found = null;
        for (JsonElement value : assets) {
            JsonObject asset = value.getAsJsonObject();
            if (!expectedName.equals(stringValue(asset, "name"))) continue;
            if (found != null) throw new IOException("Official Skill release contains duplicate assets");
            String contentType = stringValue(asset, "content_type");
            if (contentType.isBlank()) throw new IOException("Official Skill release asset has no content type");
            found = asset;
        }
        return found;
    }

    private static JsonArray arrayValue(JsonObject object, String key) throws IOException {
        JsonElement value = object.get(key);
        if (value == null || !value.isJsonArray()) throw new IOException("Official GitHub response field is invalid: " + key);
        return value.getAsJsonArray();
    }

    private static JsonObject objectValue(JsonObject object, String key) throws IOException {
        JsonElement value = object.get(key);
        if (value == null || !value.isJsonObject()) throw new IOException("Official GitHub response field is invalid: " + key);
        return value.getAsJsonObject();
    }

    private static String stringValue(JsonObject object, String key) throws IOException {
        JsonElement value = object.get(key);
        if (value == null || !value.isJsonPrimitive() || !value.getAsJsonPrimitive().isString()) {
            throw new IOException("Official GitHub response field is invalid: " + key);
        }
        return value.getAsString();
    }

    private static long numberValue(JsonObject object, String key) throws IOException {
        JsonElement value = object.get(key);
        if (value == null || !value.isJsonPrimitive() || !value.getAsJsonPrimitive().isNumber()) {
            throw new IOException("Official GitHub response field is invalid: " + key);
        }
        try { return value.getAsLong(); } catch (RuntimeException invalid) { throw new IOException("GitHub number field is invalid", invalid); }
    }

    private static boolean booleanValue(JsonObject object, String key) throws IOException {
        JsonElement value = object.get(key);
        if (value == null || !value.isJsonPrimitive() || !value.getAsJsonPrimitive().isBoolean()) {
            throw new IOException("Official GitHub response field is invalid: " + key);
        }
        return value.getAsBoolean();
    }

    private static final class LimitedBodySubscriber implements HttpResponse.BodySubscriber<byte[]> {
        private final int limit;
        private final ByteArrayOutputStream body = new ByteArrayOutputStream();
        private final CompletableFuture<byte[]> result = new CompletableFuture<>();
        private Flow.Subscription subscription;
        private boolean failed;

        private LimitedBodySubscriber(int limit) { this.limit = limit; }

        @Override public java.util.concurrent.CompletionStage<byte[]> getBody() { return result; }

        @Override public void onSubscribe(Flow.Subscription subscription) {
            this.subscription = subscription;
            subscription.request(1);
        }

        @Override public void onNext(List<ByteBuffer> buffers) {
            if (failed) return;
            for (ByteBuffer buffer : buffers) {
                int remaining = buffer.remaining();
                if ((long) body.size() + remaining > limit) {
                    failed = true;
                    subscription.cancel();
                    result.completeExceptionally(new BodyTooLargeException("Official GitHub response exceeds its size limit"));
                    return;
                }
                byte[] chunk = new byte[remaining];
                buffer.get(chunk);
                body.writeBytes(chunk);
            }
            subscription.request(1);
        }

        @Override public void onError(Throwable failure) { result.completeExceptionally(failure); }

        @Override public void onComplete() {
            if (!failed) result.complete(body.toByteArray());
        }
    }

    private static final class BodyTooLargeException extends IOException {
        private BodyTooLargeException(String message) { super(message); }
    }
}
