package com.liuzhihang.doc.view.contract;

import com.google.gson.JsonParser;
import com.intellij.testFramework.LightPlatformTestCase;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.util.Computable;
import com.intellij.openapi.progress.EmptyProgressIndicator;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.progress.ProgressManager;
import com.liuzhihang.doc.view.integration.dto.ShowDocUpdateRequest;
import com.liuzhihang.doc.view.integration.dto.YapiSave;
import com.liuzhihang.doc.view.integration.dto.YApiCat;
import com.liuzhihang.doc.view.integration.dto.YuQueCreate;
import com.liuzhihang.doc.view.integration.dto.YuQueUpdate;
import com.liuzhihang.doc.view.integration.impl.ShowDocFacadeServiceImpl;
import com.liuzhihang.doc.view.integration.impl.YApiFacadeServiceImpl;
import com.liuzhihang.doc.view.integration.impl.YuQueFacadeServiceImpl;
import com.liuzhihang.doc.view.utils.HttpUtils;
import com.sun.net.httpserver.HttpServer;

import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.HashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/** All requests are confined to an ephemeral loopback server with synthetic data. */
public final class PlatformUploadTransportTest extends LightPlatformTestCase {
    private static final String SENTINEL = "FAKE_SENSITIVE_RESPONSE_16";
    private HttpServer server;
    private ExecutorService executor;
    private String base;
    private volatile int status;
    private volatile String response;
    private volatile boolean redirect;
    private volatile boolean dropConnection;
    private volatile CountDownLatch bodyGate;
    private final AtomicInteger requestCount = new AtomicInteger();
    private final AtomicReference<Request> request = new AtomicReference<>();

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        status = 200;
        response = "{}";
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        executor = Executors.newCachedThreadPool();
        server.setExecutor(executor);
        server.createContext("/", exchange -> {
            requestCount.incrementAndGet();
            request.set(new Request(exchange.getRequestMethod(), exchange.getRequestURI(),
                    new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8),
                    exchange.getRequestHeaders().getFirst("X-Auth-Token")));
            if (dropConnection) { exchange.close(); return; }
            byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
            int code = status;
            if (redirect && !exchange.getRequestURI().getPath().equals("/redirect-target")) {
                code = 302;
                exchange.getResponseHeaders().set("Location", base + "/redirect-target");
            } else if (redirect) {
                code = 200;
            }
            exchange.sendResponseHeaders(code, bytes.length);
            try (var body = exchange.getResponseBody()) {
                CountDownLatch gate = bodyGate;
                if (gate != null) {
                    try { gate.await(40, TimeUnit.SECONDS); }
                    catch (InterruptedException interrupted) { Thread.currentThread().interrupt(); return; }
                }
                body.write(bytes);
            }
            exchange.close();
        });
        server.start();
        base = "http://127.0.0.1:" + server.getAddress().getPort();
    }

    @Override
    protected void tearDown() throws Exception {
        try {
            if (server != null) server.stop(0);
            if (executor != null) executor.shutdownNow();
        } finally {
            super.tearDown();
        }
    }

    public void testYuQueOnlyActual404MeansMissingRegardlessOfBody() throws Exception {
        var facade = new YuQueFacadeServiceImpl();
        status = 404;
        response = "文档不存在";
        assertNull(facade.getDoc(base, "fake-token", "owner/repo", "article"));
        for (int code : new int[]{401, 403, 429, 500}) {
            status = code;
            response = "404 Not Found " + SENTINEL;
            expectSafeFailure(() -> facade.getDoc(base, "fake-token", "owner/repo", "article"));
        }
        assertEquals("No implicit create, retry or redirect is allowed", 5, requestCount.get());
    }

    public void testHttpErrorsDoNotExposeResponseUrlOrPayload() throws Exception {
        status = 500;
        response = SENTINEL;
        expectSafeFailure(() -> HttpUtils.get(base + "/?token=" + SENTINEL));
        expectSafeFailure(() -> HttpUtils.post(base, SENTINEL));
        expectSafeFailure(() -> HttpUtils.put(base, SENTINEL, null));
        assertEquals(3, requestCount.get());
    }

    public void testHttpRedirectIsNotFollowed() throws Exception {
        redirect = true;
        response = "redirect";
        expectSafeFailure(() -> HttpUtils.get(base + "/start?token=fake"));
        assertEquals(1, requestCount.get());
        assertEquals("/start", request.get().uri().getPath());
    }

    public void testDroppedConnectionIsNotRetried() throws Exception {
        dropConnection = true;
        expectSafeFailure(() -> HttpUtils.get(base));
        assertEquals("A transport failure must not replay the request", 1, requestCount.get());
    }

    public void testPreCancelledRequestDoesNotReachServer() throws Exception {
        assertTrue(ApplicationManager.getApplication().executeOnPooledThread(() -> {
            EmptyProgressIndicator indicator = new EmptyProgressIndicator();
            try {
                ProgressManager.getInstance().runProcess((Computable<Boolean>) () -> {
                    indicator.cancel();
                    try { HttpUtils.get(base); }
                    catch (ProcessCanceledException expected) { throw expected; }
                    catch (Exception exception) { throw new AssertionError(exception); }
                    return false;
                }, indicator);
                return false;
            } catch (ProcessCanceledException expected) { return true; }
        }).get(10, TimeUnit.SECONDS));
        assertEquals(0, requestCount.get());
    }

    public void testSocketReadTimeoutStopsSlowResponseWithoutRetry() throws Exception {
        bodyGate = new CountDownLatch(1);
        long start = System.nanoTime();
        try {
            ApplicationManager.getApplication().executeOnPooledThread(() -> {
                try { expectSafeFailure(() -> HttpUtils.get(base)); }
                catch (Exception exception) { throw new AssertionError(exception); }
            }).get(38, TimeUnit.SECONDS);
            long elapsed = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);
            assertTrue("The configured read timeout must end a stalled response", elapsed >= 29_000 && elapsed < 38_000);
            assertEquals(1, requestCount.get());
        } finally { bodyGate.countDown(); }
    }

    public void testYuQueNullMalformedAndIncompleteResponsesFailSafely() throws Exception {
        var facade = new YuQueFacadeServiceImpl();
        for (String body : new String[]{"", "null", "{\"data\":null}", "{}", "{\"data\":{}}", "{invalid " + SENTINEL}) {
            response = body;
            expectSafeFailure(() -> facade.getDoc(base, "fake-token", "owner/repo", "article"));
        }
        response = "{\"data\":{\"id\":7}}";
        assertEquals(Long.valueOf(7), facade.getDoc(base, "fake-token", "owner/repo", "article").getData().getId());
        expectSafeFailure(() -> facade.create(base, "fake-token", "owner/repo", new YuQueCreate()));
        expectSafeFailure(() -> facade.update(base, "fake-token", "owner/repo", 7L, new YuQueUpdate()));
    }

    public void testYApiQueryPreservesSingleTokenValue() throws Exception {
        response = "{\"errcode\":0,\"data\":[]}";
        String token = "fake+&next=value";
        assertTrue(new YApiFacadeServiceImpl().getCatMenu(base, 9L, token).isEmpty());
        Map<String, String> query = new HashMap<>();
        for (String pair : request.get().uri().getRawQuery().split("&")) {
            String[] parts = pair.split("=", 2);
            query.put(URLDecoder.decode(parts[0], StandardCharsets.UTF_8),
                    URLDecoder.decode(parts[1], StandardCharsets.UTF_8));
        }
        assertEquals(Map.of("project_id", "9", "token", token), query);
    }

    public void testYApiBusinessMalformedAndMissingDataAreSafeFailures() throws Exception {
        var facade = new YApiFacadeServiceImpl();
        for (String body : new String[]{"", "null", "{}", "{\"errcode\":0}", "{\"errcode\":1,\"errmsg\":\"" + SENTINEL + "\"}", "{invalid " + SENTINEL}) {
            response = body;
            expectSafeFailure(() -> facade.getCatMenu(base, 9L, "fake-token"));
        }
        YapiSave save = new YapiSave();
        save.setYapiUrl(base);
        response = "{\"errcode\":1,\"errmsg\":\"" + SENTINEL + "\"}";
        expectSafeFailure(() -> facade.save(save));
    }

    public void testYApiCategoryResponsesRequireValidIds() throws Exception {
        var facade = new YApiFacadeServiceImpl();
        response = "{\"errcode\":\"0\",\"data\":[{\"_id\":3,\"name\":\"APIs\"}]}";
        assertEquals(Long.valueOf(3), facade.getCatMenu(base, 9L, "fake-token").getFirst().getId());
        for (String data : new String[]{"null", "{}", "[null]", "[{}]", "[{\"_id\":0}]"}) {
            response = "{\"errcode\":0,\"data\":" + data + "}";
            expectSafeFailure(() -> facade.getCatMenu(base, 9L, "fake-token"));
        }
        YApiCat category = new YApiCat(); category.setYapiUrl(base); category.setToken("fake-token"); category.setName("APIs");
        for (String data : new String[]{"null", "{}", "{\"_id\":-1}"}) {
            response = "{\"errcode\":0,\"data\":" + data + "}";
            expectSafeFailure(() -> facade.addCat(category));
        }
        response = "{\"errcode\":0,\"data\":{\"_id\":3}}";
        assertEquals(Long.valueOf(3), facade.addCat(category).getId());
        assertRequest("POST", "/api/interface/add_cat");
        assertEquals("APIs", JsonParser.parseString(request.get().body()).getAsJsonObject().get("name").getAsString());
    }

    public void testShowDocRequiresSuccessAndLinkData() throws Exception {
        var facade = new ShowDocFacadeServiceImpl();
        var input = new ShowDocUpdateRequest();
        input.setShowDocUrl(base);
        for (String body : new String[]{"", "null", "{}", "{\"error_code\":0}", "{\"error_code\":0,\"data\":{}}", "{\"error_code\":1,\"error_message\":\"" + SENTINEL + "\"}", "{invalid " + SENTINEL}) {
            response = body;
            expectSafeFailure(() -> facade.updateByApi(input));
        }
    }

    public void testSuccessfulRequestsKeepExistingPayloadHeadersAndRoutes() throws Exception {
        var yapi = new YapiSave();
        yapi.setYapiUrl(base);
        yapi.setToken("fake-yapi");
        yapi.setTitle("Example API");
        response = "{\"errcode\":0}";
        new YApiFacadeServiceImpl().save(yapi);
        assertRequest("POST", "/api/interface/save");
        assertEquals("fake-yapi", JsonParser.parseString(request.get().body()).getAsJsonObject().get("token").getAsString());
        assertEquals("Example API", JsonParser.parseString(request.get().body()).getAsJsonObject().get("title").getAsString());

        var showDoc = new ShowDocUpdateRequest();
        showDoc.setShowDocUrl(base);
        showDoc.setApiKey("fake-key");
        showDoc.setApiToken("fake-showdoc");
        showDoc.setPageContent("# Example\n中文正文");
        response = "{\"error_code\":0,\"data\":{\"item_id\":\"1\",\"page_id\":\"2\"}}";
        assertEquals("2", new ShowDocFacadeServiceImpl().updateByApi(showDoc).getData().getPageId());
        assertRequest("POST", "/api/item/updateByApi");
        assertEquals("# Example\n中文正文", JsonParser.parseString(request.get().body()).getAsJsonObject().get("page_content").getAsString());

        var yuque = new YuQueFacadeServiceImpl();
        response = "{\"data\":{\"id\":7,\"slug\":\"article\"}}";
        assertEquals(Long.valueOf(7), yuque.getDoc(base, "fake-yuque", "owner/repo", "article").getData().getId());
        assertRequest("GET", "/repos/owner/repo/docs/article");
        assertEquals("fake-yuque", request.get().token());
        var create = new YuQueCreate();
        create.setTitle("Example"); create.setSlug("article"); create.setBody("# Example");
        assertEquals("article", yuque.create(base, "fake-yuque", "owner/repo", create).getData().getSlug());
        assertRequest("POST", "/repos/owner/repo/docs/");
        assertEquals("# Example", JsonParser.parseString(request.get().body()).getAsJsonObject().get("body").getAsString());
        var update = new YuQueUpdate();
        update.setBody("# Updated");
        yuque.update(base, "fake-yuque", "owner/repo", 7L, update);
        assertRequest("PUT", "/repos/owner/repo/docs/7");
        assertEquals("# Updated", JsonParser.parseString(request.get().body()).getAsJsonObject().get("body").getAsString());
    }

    private void assertRequest(String method, String path) {
        assertEquals(method, request.get().method());
        assertEquals(path, request.get().uri().getPath());
    }

    private void expectSafeFailure(ThrowingCall call) throws Exception {
        try {
            call.run();
            fail("The platform response must be rejected");
        } catch (Exception expected) {
            for (Throwable error = expected; error != null; error = error.getCause()) {
                String message = String.valueOf(error.getMessage());
                assertFalse("Sensitive response leaked", message.contains(SENTINEL));
                assertFalse("Request URL leaked", message.contains(base));
            }
        }
    }

    private interface ThrowingCall { void run() throws Exception; }
    private record Request(String method, URI uri, String body, String token) { }
}
