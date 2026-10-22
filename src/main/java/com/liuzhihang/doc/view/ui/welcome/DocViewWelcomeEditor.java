package com.liuzhihang.doc.view.ui.welcome;

import com.intellij.ide.BrowserUtil;
import com.intellij.ide.ui.LafManagerListener;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorState;
import com.intellij.openapi.options.SearchableConfigurable;
import com.intellij.openapi.options.ShowSettingsUtil;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.UserDataHolderBase;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.jcef.JBCefApp;
import com.intellij.ui.jcef.JBCefBrowser;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import com.liuzhihang.doc.view.config.ApplicationSettings;
import com.liuzhihang.doc.view.config.TemplateConfigurable;
import com.liuzhihang.doc.view.DocViewBundle;
import org.cef.browser.CefBrowser;
import org.cef.browser.CefFrame;
import org.cef.handler.CefLifeSpanHandlerAdapter;
import org.cef.handler.CefRequestHandlerAdapter;
import org.cef.handler.CefResourceRequestHandler;
import org.cef.handler.CefResourceRequestHandlerAdapter;
import org.cef.misc.BoolRef;
import org.cef.network.CefRequest;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.beans.PropertyChangeListener;
import java.io.UncheckedIOException;
import java.util.Map;

/** A normal editor tab whose HTML presentation is rendered by the IDE's bundled Chromium. */
final class DocViewWelcomeEditor extends UserDataHolderBase implements FileEditor {
    private final Project project;
    private final DocViewWelcomeFile file;
    private final String version;
    private final JPanel root = new JPanel(new BorderLayout());
    private final JBCefBrowser browser;
    private volatile boolean disposed;
    private volatile long generation;
    private volatile ProgressIndicator activeIndicator;
    private volatile String documentUrl;
    private String currentPage = "welcome";

    DocViewWelcomeEditor(Project project, DocViewWelcomeFile file, String version) {
        this.project = project;
        this.file = file;
        this.version = version;
        root.getAccessibleContext().setAccessibleName(DocViewBundle.message("welcome.editor.accessible.name"));
        if (JBCefApp.isSupported()) {
            browser = new JBCefBrowser();
            Disposer.register(this, browser);
            installNavigation();
            root.add(browser.getComponent(), BorderLayout.CENTER);
        } else {
            browser = null;
            showStatus(DocViewBundle.message("welcome.error.jcef.unavailable"));
        }
        ApplicationManager.getApplication().getMessageBus().connect(this).subscribe(LafManagerListener.TOPIC,
                source -> showPage(currentPage));
        showPage(file.getRequestedPage());
    }

    private void installNavigation() {
        var client = browser.getJBCefClient();
        var cef = browser.getCefBrowser();
        var resources = new CefResourceRequestHandlerAdapter() {
            @Override public boolean onBeforeResourceLoad(CefBrowser source, CefFrame frame, CefRequest request) {
                // loadHTML uses a framework-managed local URL, not the display/base URL.
                // Static pages need no network subresources; CSP blocks other embedded content.
                return disposed || request.getURL().matches("(?i)https?://.*");
            }
        };
        var requests = new CefRequestHandlerAdapter() {
            @Override public boolean onBeforeBrowse(CefBrowser source, CefFrame frame, CefRequest request,
                                                    boolean userGesture, boolean redirect) {
                String url = request.getURL();
                if (!disposed && !userGesture && !redirect) return false;
                if (userGesture && !redirect && frame.isMain()) dispatchNavigation(url);
                return true;
            }
            @Override public boolean onOpenURLFromTab(CefBrowser source, CefFrame frame, String url, boolean userGesture) {
                if (userGesture) dispatchNavigation(url);
                return true;
            }
            @Override public CefResourceRequestHandler getResourceRequestHandler(CefBrowser source, CefFrame frame,
                    CefRequest request, boolean navigation, boolean download, String initiator, BoolRef disableDefaultHandling) {
                return resources;
            }
        };
        var popups = new CefLifeSpanHandlerAdapter() {
            @Override public boolean onBeforePopup(CefBrowser source, CefFrame frame, String url, String name) {
                return true;
            }
        };
        client.addRequestHandler(requests, cef);
        client.addLifeSpanHandler(popups, cef);
        Disposer.register(this, () -> {
            client.removeRequestHandler(requests, cef);
            client.removeLifeSpanHandler(popups, cef);
        });
    }

    void showPage(String page) {
        ApplicationManager.getApplication().assertIsDispatchThread();
        if (disposed || project.isDisposed() || !WelcomeDocuments.PAGES.contains(page)) return;
        currentPage = page;
        file.setRequestedPage(page);
        long request = nextRequest();
        if (browser == null) return;
        Map<String, String> theme = theme();
        boolean notifications = ApplicationSettings.getInstance().isShowUpdateNotification();
        root.setBackground(UIUtil.getPanelBackground());
        browser.setPageBackgroundColor(theme.get("BG"));
        ProgressManager.getInstance().run(new Task.Backgroundable(project, "Doc View", true) {
            private String html;
            @Override public void run(@NotNull ProgressIndicator indicator) {
                if (!register(request, indicator)) return;
                try {
                    html = WelcomeDocuments.render(page, version, theme, notifications);
                } catch (java.io.IOException error) {
                    throw new UncheckedIOException(error);
                }
                indicator.checkCanceled();
            }
            @Override public void onSuccess() {
                if (!current(request)) return;
                activeIndicator = null;
                documentUrl = "https://doc-view.local/" + page + "?render=" + request;
                root.removeAll();
                root.add(browser.getComponent(), BorderLayout.CENTER);
                root.revalidate();
                root.repaint();
                browser.loadHTML(html, documentUrl);
            }
            @Override public void onCancel() { fail(request, DocViewBundle.message("welcome.error.load.cancelled")); }
            @Override public void onThrowable(@NotNull Throwable error) { fail(request, DocViewBundle.message("welcome.error.load.failed")); }
        });
    }

    private static Map<String, String> theme() {
        boolean dark = !JBColor.isBright();
        return Map.of("THEME", dark ? "dark" : "light", "FONT_SIZE", Integer.toString(JBUI.scale(16)),
                "BG", hex(UIUtil.getPanelBackground()), "FG", hex(UIUtil.getLabelForeground()),
                "MUTED", dark ? "#a8adbb" : "#626875", "BORDER", dark ? "#3b3e47" : "#d8dbe2",
                "LINK", dark ? "#8cabff" : "#315fc6");
    }

    private static String hex(Color color) { return String.format("#%02x%02x%02x", color.getRed(), color.getGreen(), color.getBlue()); }

    private void dispatchNavigation(String url) {
        long request = generation;
        ApplicationManager.getApplication().invokeLater(() -> {
            if (!current(request)) return;
            String action = WelcomeDocuments.action(url);
            if (action == null) {
                if (WelcomeDocuments.isExternalLink(url)) BrowserUtil.browse(url);
                return;
            }
            switch (action) {
                case "home" -> showPage("welcome");
                case "updates" -> showPage("updates");
                case "directory" -> {
                    var window = ToolWindowManager.getInstance(project).getToolWindow("Doc View");
                    if (window != null) window.activate(null);
                }
                case "templates" -> ShowSettingsUtil.getInstance().showSettingsDialog(project, TemplateConfigurable.class);
                case "integrations" -> ShowSettingsUtil.getInstance().showSettingsDialog(project,
                        configurable -> configurable instanceof SearchableConfigurable searchable
                                && "liuzhihang.api.doc.IntegrationSettingsConfigurable".equals(searchable.getId()), null);
                case "notifications" -> {
                    var settings = ApplicationSettings.getInstance();
                    settings.setShowUpdateNotification(!settings.isShowUpdateNotification());
                    showPage(currentPage);
                }
                default -> { }
            }
        });
    }

    private synchronized long nextRequest() {
        long next = ++generation;
        if (activeIndicator != null) activeIndicator.cancel();
        activeIndicator = null;
        return next;
    }
    private synchronized boolean register(long request, ProgressIndicator indicator) {
        if (!current(request)) { indicator.cancel(); return false; }
        activeIndicator = indicator;
        return true;
    }
    private boolean current(long request) { return !disposed && !project.isDisposed() && request == generation; }
    private void fail(long request, String message) {
        if (!current(request)) return;
        activeIndicator = null;
        showStatus(message);
    }
    private void showStatus(String message) {
        root.removeAll();
        JPanel status = new JPanel(new FlowLayout(FlowLayout.LEADING, 16, 24));
        status.add(new JBLabel(message));
        JButton online = new JButton(DocViewBundle.message("welcome.action.open.guide"));
        online.addActionListener(event -> { if (!disposed && !project.isDisposed()) BrowserUtil.browse(WelcomeDocuments.GUIDE); });
        status.add(online);
        root.add(status, BorderLayout.NORTH);
        root.revalidate();
        root.repaint();
    }

    @Override public @NotNull JComponent getComponent() { return root; }
    @Override public JComponent getPreferredFocusedComponent() { return browser == null ? root : browser.getComponent(); }
    @Override public @NotNull String getName() { return "Doc View"; }
    @Override public @NotNull VirtualFile getFile() { return file; }
    @Override public void setState(@NotNull FileEditorState state) {}
    @Override public boolean isModified() { return false; }
    @Override public boolean isValid() { return !disposed && !project.isDisposed() && file.isValid(); }
    @Override public void addPropertyChangeListener(@NotNull PropertyChangeListener listener) {}
    @Override public void removePropertyChangeListener(@NotNull PropertyChangeListener listener) {}
    @Override public synchronized void dispose() { disposed = true; nextRequest(); }
}
