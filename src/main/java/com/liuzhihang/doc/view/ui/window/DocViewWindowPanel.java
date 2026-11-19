package com.liuzhihang.doc.view.ui.window;

import com.intellij.ide.util.treeView.AbstractTreeStructure;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.ActionPlaces;
import com.intellij.openapi.actionSystem.ActionToolbar;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.DataSink;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.fileEditor.FileEditorOpenRequest;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.SimpleToolWindowPanel;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.pom.Navigatable;
import com.intellij.psi.PsiElement;
import com.intellij.ui.DocumentAdapter;
import com.intellij.ui.PopupHandler;
import com.intellij.ui.ScrollPaneFactory;
import com.intellij.ui.SearchTextField;
import com.intellij.ui.TreeSpeedSearch;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBTextField;
import com.intellij.ui.tree.AsyncTreeModel;
import com.intellij.ui.tree.StructureTreeModel;
import com.intellij.ui.treeStructure.SimpleNode;
import com.intellij.ui.treeStructure.SimpleTree;
import com.intellij.ui.treeStructure.SimpleTreeStructure;
import com.intellij.util.ui.JBUI;
import com.intellij.util.Alarm;
import com.liuzhihang.doc.view.DocViewBundle;
import com.liuzhihang.doc.view.action.toolbar.window.ProjectDocumentExportAction;
import com.liuzhihang.doc.view.data.DocViewDataKeys;
import com.liuzhihang.doc.view.ui.window.ProjectSearchResultRenderer.Row;
import com.liuzhihang.doc.view.service.document.ProjectDocumentIndexListener;
import com.liuzhihang.doc.view.service.document.ProjectDocumentIndexMarkdownResult;
import com.liuzhihang.doc.view.service.document.ProjectDocumentIndexService;
import com.liuzhihang.doc.view.service.document.ProjectDocumentIndexStatus;
import com.liuzhihang.doc.view.service.document.ProjectDocumentQuery;
import com.liuzhihang.doc.view.service.document.ProjectDocumentQueryException;
import com.liuzhihang.doc.view.utils.FileEditorOpenSupport;
import com.liuzhihang.doc.view.service.document.ProjectDocumentQueryResult;
import com.liuzhihang.doc.view.service.document.ProjectDocumentMarkdownResult;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeSelectionModel;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseListener;
import java.awt.event.MouseEvent;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/** The Doc View tool window and its project-document search surface. */
public class DocViewWindowPanel extends SimpleToolWindowPanel implements Disposable {

    private static final String TOOL_WINDOW_ID = "Doc View";
    private static final String CATALOG_CARD = "catalog";
    private static final String SEARCH_CARD = "search";

    private volatile RootNode rootNode = new RootNode();

    private final Project project;
    private final SimpleTree catalogTree;
    private final TreeSpeedSearch catalogTreeSpeedSearch;
    private final ToolWindow toolWindow;
    private final StructureTreeModel<AbstractTreeStructure> treeModel;
    private final JLabel inlineStatus = new JBLabel();
    private final JButton generateProjectDocsButton = new JButton();
    private final JPanel centerCards = new JPanel(new CardLayout());
    private final DefaultListModel<Row> searchResultsModel = new DefaultListModel<>();
    private final SearchTextField searchField = new SearchTextField(false) {
        @Override
        protected boolean toClearTextOnEscape() {
            return false; // Esc returns to the catalog. The native clear icon resets the query.
        }
    };
    private final JButton backToCatalogButton = new JButton();
    private final JButton openSelectedButton = new JButton();
    private final JButton nextPageButton = new JButton();
    private final JList<Row> searchResults = new JList<>(searchResultsModel);
    private final JPanel searchCard;
    private final Alarm projectStatusRefreshAlarm;
    private final Alarm liveSearchAlarm = new Alarm(Alarm.ThreadToUse.SWING_THREAD, this);

    private volatile long refreshGeneration;
    private volatile boolean disposed;
    private volatile ProgressIndicator refreshIndicator;
    private volatile long documentStatusGeneration;
    private volatile ProgressIndicator documentStatusIndicator;
    private volatile long documentActionGeneration;
    private volatile ProgressIndicator documentActionIndicator;
    private volatile CompletableFuture<?> documentOpenFuture;
    private volatile String catalogStatusText = "";
    private volatile ProjectStatusText projectStatusText = new ProjectStatusText("");
    private volatile String projectStatusDetail = "";
    private volatile ProjectDocumentIndexStatus currentDocumentStatus;
    private String searchSnapshotId;
    private String searchCursor;
    private boolean firstSearchPagePending;
    private boolean focusResultsWhenReady;

    public DocViewWindowPanel(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        super(Boolean.TRUE, Boolean.TRUE);
        this.project = project;
        this.toolWindow = toolWindow;
        projectStatusRefreshAlarm = new Alarm(Alarm.ThreadToUse.POOLED_THREAD, this);

        final ActionManager actionManager = ActionManager.getInstance();
        ActionToolbar actionToolbar = actionManager.createActionToolbar(ActionPlaces.TOOLWINDOW_TOOLBAR_BAR,
                (DefaultActionGroup) actionManager.getAction("liuzhihang.doc.tool.window.toolbar.action"),
                true);
        actionToolbar.setTargetComponent(this);
        setToolbar(actionToolbar.getComponent());

        treeModel = new StructureTreeModel<>(new SimpleTreeStructure() {
            @NotNull
            @Override
            public Object getRootElement() {
                return rootNode;
            }
        }, null, this);
        catalogTree = new SimpleTree(new AsyncTreeModel(treeModel, this));
        initCatalogTree();

        JPanel statusStrip = new JPanel(new BorderLayout(JBUI.scale(8), 0));
        inlineStatus.setBorder(JBUI.Borders.empty(4, 8));
        generateProjectDocsButton.setText(DocViewBundle.message("project.library.generate.short"));
        generateProjectDocsButton.setIcon(AllIcons.Actions.AddFile);
        generateProjectDocsButton.setToolTipText(DocViewBundle.message("project.library.generate"));
        generateProjectDocsButton.getAccessibleContext().setAccessibleName(DocViewBundle.message("project.library.generate"));
        generateProjectDocsButton.setVisible(false);
        generateProjectDocsButton.addActionListener(event -> ProjectDocumentExportAction.generateProject(project));
        statusStrip.add(inlineStatus, BorderLayout.CENTER);
        statusStrip.add(generateProjectDocsButton, BorderLayout.EAST);

        centerCards.add(ScrollPaneFactory.createScrollPane(catalogTree), CATALOG_CARD);
        searchCard = createSearchCard();
        centerCards.add(searchCard, SEARCH_CARD);

        JPanel content = new JPanel(new BorderLayout());
        content.add(statusStrip, BorderLayout.NORTH);
        content.add(centerCards, BorderLayout.CENTER);
        setContent(content);

        catalogTreeSpeedSearch = TreeSpeedSearch.installOn(catalogTree, false,
                TreeSpeedSearch.NODE_PRESENTATION_FUNCTION);
        project.getMessageBus().connect(this).subscribe(ProjectDocumentIndexListener.TOPIC,
                (inputEpoch, committedSnapshotId) -> ApplicationManager.getApplication().invokeLater(() -> {
                    if (!disposed && !project.isDisposed()) scheduleProjectDocumentStatusRefresh();
                }));
        updateCatalogTree();
        refreshProjectDocumentStatus();
    }

    private JPanel createSearchCard() {
        JPanel card = new JPanel(new BorderLayout(JBUI.scale(6), JBUI.scale(6)));
        card.setBorder(JBUI.Borders.empty(6));

        String backLabel = DocViewBundle.message("project.library.back.catalog");
        backToCatalogButton.setIcon(AllIcons.Actions.Back);
        int backSize = backToCatalogButton.getPreferredSize().height;
        backToCatalogButton.setPreferredSize(new Dimension(backSize, backSize));
        backToCatalogButton.setToolTipText(backLabel);
        backToCatalogButton.getAccessibleContext().setAccessibleName(backLabel);
        backToCatalogButton.addActionListener(event -> showCatalog());
        JBTextField editor = searchField.getTextEditor();
        editor.getAccessibleContext().setAccessibleName(DocViewBundle.message("project.library.search.query"));
        editor.setToolTipText(DocViewBundle.message("project.library.search.query"));
        editor.getEmptyText().setText(DocViewBundle.message("project.library.search.placeholder"));
        searchField.addDocumentListener(new DocumentAdapter() {
            @Override
            protected void textChanged(@NotNull DocumentEvent event) {
                searchDocuments(null, true);
            }
        });

        JPanel queryRow = new JPanel(new BorderLayout(JBUI.scale(6), 0));
        queryRow.add(backToCatalogButton, BorderLayout.WEST);
        queryRow.add(searchField, BorderLayout.CENTER);
        card.add(queryRow, BorderLayout.NORTH);

        searchResults.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        searchResults.addListSelectionListener(event -> {
            if (!event.getValueIsAdjusting()) updateSearchSelectionState();
        });
        searchResults.setCellRenderer(new ProjectSearchResultRenderer());
        searchResults.getAccessibleContext().setAccessibleName(DocViewBundle.message("project.library.search.results"));
        editor.getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, 0), "focus-results");
        editor.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent event) {
                focusResultsWhenReady = false;
            }
        });
        editor.getActionMap().put("focus-results", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent event) {
                if (firstSearchPagePending) {
                    focusResultsWhenReady = true;
                } else if (searchSnapshotId != null && !searchResultsModel.isEmpty()) {
                    focusSearchResult(Math.max(searchResults.getSelectedIndex(), 0));
                }
            }
        });
        searchResults.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent event) {
                if (event.getClickCount() == 2 && SwingUtilities.isLeftMouseButton(event)) {
                    int index = searchResults.locationToIndex(event.getPoint());
                    if (index >= 0 && searchResults.getCellBounds(index, index).contains(event.getPoint())) {
                        searchResults.setSelectedIndex(index);
                        openSelectedMarkdown();
                    }
                }
            }
        });
        searchResults.getInputMap(JComponent.WHEN_FOCUSED).put(
                KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "open-project-markdown");
        searchResults.getActionMap().put("open-project-markdown", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent event) {
                openSelectedMarkdown();
            }
        });
        card.add(ScrollPaneFactory.createScrollPane(searchResults), BorderLayout.CENTER);

        AbstractAction back = new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent event) {
                showCatalog();
            }
        };
        // Keep Escape scoped to the focusable search controls.
        KeyStroke escape = KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0);
        for (JComponent control : List.of(editor, searchResults, backToCatalogButton, nextPageButton, openSelectedButton)) {
            control.getInputMap(JComponent.WHEN_FOCUSED).put(escape, "back-to-catalog");
            control.getActionMap().put("back-to-catalog", back);
        }

        openSelectedButton.setText(DocViewBundle.message("project.library.open.markdown"));
        openSelectedButton.setIcon(AllIcons.FileTypes.Markdown);
        openSelectedButton.addActionListener(event -> openSelectedMarkdown());
        nextPageButton.setText(DocViewBundle.message("project.library.search.more"));
        nextPageButton.setVisible(false);
        nextPageButton.addActionListener(event -> searchDocuments(searchCursor, false));
        JPanel actionRow = new JPanel(new BorderLayout());
        actionRow.add(nextPageButton, BorderLayout.WEST);
        actionRow.add(openSelectedButton, BorderLayout.EAST);
        card.add(actionRow, BorderLayout.SOUTH);
        return card;
    }

    /** Initializes the project-document search view without generating or scanning source files. */
    public void showProjectSearch() {
        if (!isUsable()) return;
        ((CardLayout) centerCards.getLayout()).show(centerCards, SEARCH_CARD);
        searchField.requestFocusInWindow();
        searchDocuments(null, false);
    }

    /** Returns to the live API catalog and prevents late search results from changing the panel. */
    public void showCatalog() {
        if (!isUsable()) return;
        cancelDocumentAction();
        ((CardLayout) centerCards.getLayout()).show(centerCards, CATALOG_CARD);
        searchResultsModel.clear();
        searchSnapshotId = null;
        searchCursor = null;
        nextPageButton.setVisible(false);
        updateProjectStatusDetail("");
        catalogTree.requestFocusInWindow();
    }

    /** Opens the verified, on-disk Markdown directory index for the current committed snapshot. */
    public void openProjectDocs() {
        if (!isUsable()) return;
        long request = beginDocumentAction(DocViewBundle.message("project.library.open.pending"));
        ProgressManager.getInstance().run(new Task.Backgroundable(project,
                DocViewBundle.message("task.project.docs.open"), true) {
            private ProjectDocumentIndexMarkdownResult result;
            private VirtualFile file;

            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                if (!registerDocumentAction(request, indicator)) return;
                ProjectDocumentIndexService service = ProjectDocumentIndexService.getInstance(project);
                ProjectDocumentIndexStatus status = service.status(indicator);
                applyStatusFromBackground(request, status);
                if (status.indexState() == ProjectDocumentIndexStatus.IndexState.NOT_GENERATED) {
                    throw new ProjectDocumentQueryException(ProjectDocumentQueryException.Code.INDEX_NOT_FOUND,
                            "Project documentation has not been generated");
                }
                if (status.indexState() != ProjectDocumentIndexStatus.IndexState.READY
                        || status.snapshotId() == null) {
                    throw statusException(status);
                }
                result = service.getIndexMarkdown(status.snapshotId(), indicator);
                file = findVerifiedFile(result.relativePath());
                if (file == null) {
                    throw new ProjectDocumentQueryException(ProjectDocumentQueryException.Code.IO_ERROR,
                            "The verified project index is not available in the IDE file system",
                            result.relativePath());
                }
            }

            @Override
            public void onSuccess() {
                if (!isCurrentDocumentAction(request)) return;
                documentActionIndicator = null;
                openVerifiedFile(file, request, result.relativePath());
            }

            @Override
            public void onCancel() {
                if (isCurrentDocumentAction(request)) {
                    documentActionIndicator = null;
                    showQueryError(new ProjectDocumentQueryException(ProjectDocumentQueryException.Code.CANCELLED,
                            "Opening project documentation was cancelled"));
                }
            }

            @Override
            public void onThrowable(@NotNull Throwable error) {
                if (isCurrentDocumentAction(request)) {
                    documentActionIndicator = null;
                    showQueryError(error);
                }
            }
        });
    }

    private void openVerifiedFile(VirtualFile file, long request, String relativePath) {
        if (!isCurrentDocumentAction(request) || file == null) return;
        try {
            var openResult = FileEditorManager.getInstance(project).requestOpenFile(file,
                    FileEditorOpenRequest.withFocus(true).withSelectAsCurrent(true));
            documentOpenFuture = openResult;
            FileEditorOpenSupport.whenOpened(project, openResult,
                    () -> isCurrentDocumentAction(request),
                    opened -> {
                        documentOpenFuture = null;
                        updateProjectStatusDetail(DocViewBundle.message("project.library.opened", relativePath));
                    },
                    error -> {
                        documentOpenFuture = null;
                        showQueryError(new ProjectDocumentQueryException(ProjectDocumentQueryException.Code.IO_ERROR,
                                "The verified Markdown document could not be opened", relativePath));
                    });
        } catch (RuntimeException error) {
            if (!FileEditorOpenSupport.isCancellation(error) && isCurrentDocumentAction(request)) {
                showQueryError(new ProjectDocumentQueryException(ProjectDocumentQueryException.Code.IO_ERROR,
                        "The verified Markdown document could not be opened", relativePath));
            }
        }
    }

    private void focusSearchResult(int index) {
        searchResults.setSelectedIndex(index);
        searchResults.ensureIndexIsVisible(index);
        searchResults.requestFocusInWindow();
    }

    private void searchDocuments(String cursor, boolean afterTyping) {
        if (!isUsable() || !searchCard.isVisible()) return;
        String requestedQuery = searchField.getText();
        if (cursor == null) {
            searchSnapshotId = null;
            searchCursor = null;
            searchResultsModel.clear();
            nextPageButton.setVisible(false);
        }
        long request = beginDocumentAction(DocViewBundle.message("project.library.search.pending"));
        firstSearchPagePending = cursor == null;
        if (afterTyping) {
            liveSearchAlarm.addRequest(() -> loadSearchPage(request, requestedQuery, cursor), 200);
        } else {
            loadSearchPage(request, requestedQuery, cursor);
        }
    }

    private void loadSearchPage(long request, String requestedQuery, String cursor) {
        if (!isCurrentDocumentAction(request)) return;
        ProgressManager.getInstance().run(new Task.Backgroundable(project,
                DocViewBundle.message("task.project.docs.search"), true) {
            private ProjectDocumentQueryResult result;
            private List<Row> rows;
            private ProjectDocumentQueryException failure;

            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                if (!registerDocumentAction(request, indicator)) return;
                try {
                    ProjectDocumentIndexService service = ProjectDocumentIndexService.getInstance(project);
                    result = service.search(
                            new ProjectDocumentQuery(requestedQuery, null, null, null,
                                    ProjectDocumentQuery.DEFAULT_LIMIT, cursor), indicator);
                    rows = ProjectSearchResultRenderer.rows(result.items(), requestedQuery, indicator);
                } catch (ProjectDocumentQueryException exception) {
                    failure = exception;
                }
            }

            @Override
            public void onSuccess() {
                if (!isCurrentDocumentAction(request)) return;
                documentActionIndicator = null;
                boolean enterFirstResult = focusResultsWhenReady;
                resetSearchNavigation();
                if (failure != null) {
                    if (cursor != null && failure.code() == ProjectDocumentQueryException.Code.CURSOR_STALE) {
                        if (nextPageButton.isFocusOwner()) searchField.requestFocusInWindow();
                        searchDocuments(null, false);
                    } else {
                        showQueryError(failure);
                    }
                    return;
                }
                if (result == null) {
                    showQueryError(new ProjectDocumentQueryException(ProjectDocumentQueryException.Code.IO_ERROR,
                            "Project document search returned no result"));
                    return;
                }
                searchSnapshotId = result.indexStatus().snapshotId();
                searchCursor = result.nextCursor();
                boolean continueFromMore = cursor != null && nextPageButton.isFocusOwner();
                int firstAddedRow = searchResultsModel.size();
                searchResultsModel.addAll(rows);
                nextPageButton.setVisible(searchCursor != null);
                String searchMessage = result.totalMatched() == 0
                        ? DocViewBundle.message("project.library.search.no.results")
                        : DocViewBundle.message("project.library.search.ready",
                                searchResultsModel.size(), result.totalMatched());
                applyDocumentStatus(result.indexStatus());
                updateProjectStatusDetail(searchMessage);
                if (!rows.isEmpty() && (continueFromMore
                        || (enterFirstResult && searchField.getTextEditor().isFocusOwner()))) {
                    focusSearchResult(firstAddedRow);
                }
            }

            @Override
            public void onCancel() {
                if (isCurrentDocumentAction(request)) {
                    documentActionIndicator = null;
                    resetSearchNavigation();
                    showQueryError(new ProjectDocumentQueryException(ProjectDocumentQueryException.Code.CANCELLED,
                            "Project document search was cancelled"));
                }
            }

            @Override
            public void onThrowable(@NotNull Throwable error) {
                if (isCurrentDocumentAction(request)) {
                    documentActionIndicator = null;
                    resetSearchNavigation();
                    showQueryError(error);
                }
            }
        });
    }

    private void openSelectedMarkdown() {
        if (!isUsable() || !searchResults.isEnabled()) return;
        Row selected = searchResults.getSelectedValue();
        String expectedSnapshot = searchSnapshotId;
        if (selected == null || expectedSnapshot == null) {
            updateProjectStatusDetail(DocViewBundle.message("project.library.search.select.result"));
            return;
        }
        String apiId = selected.entry().apiId();
        long request = beginDocumentAction(DocViewBundle.message("project.library.open.pending"));
        ProgressManager.getInstance().run(new Task.Backgroundable(project,
                DocViewBundle.message("task.project.docs.open"), true) {
            private ProjectDocumentMarkdownResult result;
            private VirtualFile file;

            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                if (!registerDocumentAction(request, indicator)) return;
                result = ProjectDocumentIndexService.getInstance(project).getMarkdown(apiId, expectedSnapshot, indicator);
                file = findVerifiedFile(result.relativePath());
                if (file == null) {
                    throw new ProjectDocumentQueryException(ProjectDocumentQueryException.Code.IO_ERROR,
                            "The verified API Markdown is not available in the IDE file system",
                            result.relativePath());
                }
            }

            @Override
            public void onSuccess() {
                if (!isCurrentDocumentAction(request)) return;
                documentActionIndicator = null;
                openVerifiedFile(file, request, result.relativePath());
            }

            @Override
            public void onCancel() {
                if (isCurrentDocumentAction(request)) {
                    documentActionIndicator = null;
                    showQueryError(new ProjectDocumentQueryException(ProjectDocumentQueryException.Code.CANCELLED,
                            "Opening API Markdown was cancelled"));
                }
            }

            @Override
            public void onThrowable(@NotNull Throwable error) {
                if (isCurrentDocumentAction(request)) {
                    documentActionIndicator = null;
                    showQueryError(error);
                }
            }
        });
    }

    private VirtualFile findVerifiedFile(String relativePath) {
        String basePath = project.getBasePath();
        if (basePath == null || relativePath == null || relativePath.isBlank()) return null;
        Path documentRoot = Path.of(basePath).resolve(ProjectDocumentIndexService.DOCUMENT_ROOT).normalize();
        Path target = documentRoot.resolve(relativePath).normalize();
        if (!target.startsWith(documentRoot)) {
            throw new ProjectDocumentQueryException(ProjectDocumentQueryException.Code.INDEX_INCONSISTENT,
                    "The verified document path leaves the project document root", relativePath);
        }
        return LocalFileSystem.getInstance().refreshAndFindFileByNioFile(target);
    }

    private void resetSearchNavigation() {
        firstSearchPagePending = false;
        focusResultsWhenReady = false;
    }

    private long beginDocumentAction(String detail) {
        resetSearchNavigation();
        liveSearchAlarm.cancelAllRequests();
        long request = ++documentActionGeneration;
        cancelPendingEditorOpen();
        ProgressIndicator previous = documentActionIndicator;
        documentActionIndicator = null;
        if (previous != null) previous.cancel();
        updateProjectStatusDetail(detail);
        return request;
    }

    private synchronized boolean registerDocumentAction(long request, ProgressIndicator indicator) {
        if (!isCurrentDocumentAction(request)) {
            indicator.cancel();
            return false;
        }
        documentActionIndicator = indicator;
        return true;
    }

    private boolean isCurrentDocumentAction(long request) {
        return isUsable() && request == documentActionGeneration;
    }

    private void cancelDocumentAction() {
        resetSearchNavigation();
        liveSearchAlarm.cancelAllRequests();
        documentActionGeneration++;
        cancelPendingEditorOpen();
        ProgressIndicator previous = documentActionIndicator;
        documentActionIndicator = null;
        if (previous != null) previous.cancel();
    }

    private void cancelPendingEditorOpen() {
        CompletableFuture<?> pending = documentOpenFuture;
        documentOpenFuture = null;
        if (pending != null) pending.cancel(true);
    }

    private void refreshProjectDocumentStatus() {
        if (!isUsable()) return;
        long request = ++documentStatusGeneration;
        ProgressIndicator previous = documentStatusIndicator;
        documentStatusIndicator = null;
        if (previous != null) previous.cancel();
        if (currentDocumentStatus == null) {
            projectStatusText = new ProjectStatusText(DocViewBundle.message("project.library.status.loading"));
            renderInlineStatus();
        }
        ProgressManager.getInstance().run(new Task.Backgroundable(project,
                DocViewBundle.message("task.project.docs.status"), true) {
            private ProjectDocumentIndexStatus result;

            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                if (!registerStatusIndicator(request, indicator)) return;
                result = ProjectDocumentIndexService.getInstance(project).status(indicator);
            }

            @Override
            public void onSuccess() {
                if (!isCurrentStatusRequest(request)) return;
                documentStatusIndicator = null;
                applyDocumentStatus(result);
            }

            @Override
            public void onCancel() {
                if (isCurrentStatusRequest(request)) {
                    documentStatusIndicator = null;
                    projectStatusText = new ProjectStatusText(DocViewBundle.message("project.library.status.cancelled"));
                    renderInlineStatus();
                }
            }

            @Override
            public void onThrowable(@NotNull Throwable error) {
                if (isCurrentStatusRequest(request)) {
                    documentStatusIndicator = null;
                    projectStatusText = new ProjectStatusText(queryErrorMessage(error));
                    renderInlineStatus();
                }
            }
        });
    }

    private void scheduleProjectDocumentStatusRefresh() {
        if (!isUsable()) return;
        projectStatusRefreshAlarm.cancelAllRequests();
        projectStatusRefreshAlarm.addRequest(() -> ApplicationManager.getApplication().invokeLater(() -> {
            if (isUsable()) refreshProjectDocumentStatus();
        }), 150);
    }

    private synchronized boolean registerStatusIndicator(long request, ProgressIndicator indicator) {
        if (!isCurrentStatusRequest(request)) {
            indicator.cancel();
            return false;
        }
        documentStatusIndicator = indicator;
        return true;
    }

    private boolean isCurrentStatusRequest(long request) {
        return isUsable() && request == documentStatusGeneration;
    }

    private void applyStatusFromBackground(long actionRequest, ProjectDocumentIndexStatus status) {
        if (!isCurrentDocumentAction(actionRequest)) return;
        ApplicationManager.getApplication().invokeLater(() -> {
            if (isCurrentDocumentAction(actionRequest)) applyDocumentStatus(status);
        });
    }

    private void applyDocumentStatus(ProjectDocumentIndexStatus status) {
        if (!isUsable() || status == null) return;
        currentDocumentStatus = status;
        boolean notGenerated = status.indexState() == ProjectDocumentIndexStatus.IndexState.NOT_GENERATED;
        generateProjectDocsButton.setVisible(notGenerated);
        String base = switch (status.indexState()) {
            case NOT_GENERATED -> DocViewBundle.message("project.library.status.not.generated");
            case UNSUPPORTED_SCHEMA -> DocViewBundle.message("project.library.status.unsupported");
            case INVALID -> DocViewBundle.message("project.library.status.invalid");
            case INCONSISTENT -> DocViewBundle.message("project.library.status.inconsistent");
            case READY -> switch (status.freshness()) {
                case CURRENT -> DocViewBundle.message("project.library.status.current");
                case STALE -> DocViewBundle.message("project.library.status.stale");
                case UNKNOWN -> DocViewBundle.message("project.library.status.unknown");
            };
        };
        String compact = status.indexState() == ProjectDocumentIndexStatus.IndexState.READY
                ? DocViewBundle.message(switch (status.freshness()) {
                    case CURRENT -> "project.library.search.freshness.current";
                    case STALE -> "project.library.search.freshness.stale";
                    case UNKNOWN -> "project.library.search.freshness.unknown";
                })
                : base;
        String total = status.entryCount() == null ? ""
                : DocViewBundle.message("project.library.status.entries", status.entryCount());
        String unsaved = Boolean.TRUE.equals(status.hasUnsavedDocuments())
                ? DocViewBundle.message("project.library.status.unsaved") : "";
        projectStatusText = new ProjectStatusText(joinStatus(base, total, unsaved), compact);
        renderInlineStatus();
    }

    private ProjectDocumentQueryException statusException(ProjectDocumentIndexStatus status) {
        ProjectDocumentQueryException.Code code = switch (status.indexState()) {
            case NOT_GENERATED -> ProjectDocumentQueryException.Code.INDEX_NOT_FOUND;
            case UNSUPPORTED_SCHEMA -> ProjectDocumentQueryException.Code.UNSUPPORTED_SCHEMA;
            case INVALID -> ProjectDocumentQueryException.Code.INDEX_INVALID;
            case INCONSISTENT -> ProjectDocumentQueryException.Code.INDEX_INCONSISTENT;
            case READY -> ProjectDocumentQueryException.Code.SNAPSHOT_CHANGED;
        };
        return new ProjectDocumentQueryException(code, "The project documentation index is not available");
    }

    private void showQueryError(Throwable error) {
        if (!isUsable()) return;
        if (searchSnapshotId == null) searchResultsModel.clear();
        if (error instanceof ProjectDocumentQueryException query
                && query.code() == ProjectDocumentQueryException.Code.INDEX_NOT_FOUND) {
            currentDocumentStatus = new ProjectDocumentIndexStatus(ProjectDocumentIndexStatus.IndexState.NOT_GENERATED,
                    ProjectDocumentIndexStatus.Freshness.UNKNOWN, null, null, null, null,
                    ProjectDocumentQueryException.Code.INDEX_NOT_FOUND.name());
            generateProjectDocsButton.setVisible(true);
            projectStatusText = new ProjectStatusText(DocViewBundle.message("project.library.status.not.generated"));
        }
        updateProjectStatusDetail(queryErrorMessage(error));
        updateSearchSelectionState();
    }

    private String queryErrorMessage(Throwable error) {
        if (error instanceof ProjectDocumentQueryException query) {
            return switch (query.code()) {
                case INDEX_NOT_FOUND -> DocViewBundle.message("project.library.status.not.generated");
                case UNSUPPORTED_SCHEMA -> DocViewBundle.message("project.library.status.unsupported");
                case INDEX_INVALID -> DocViewBundle.message("project.library.status.invalid");
                case INDEX_INCONSISTENT -> DocViewBundle.message("project.library.status.inconsistent");
                case BUSY -> DocViewBundle.message("project.export.already.running");
                case CANCELLED -> DocViewBundle.message("project.library.status.cancelled");
                case TIMEOUT -> DocViewBundle.message("project.library.status.timeout");
                case SNAPSHOT_CHANGED, CURSOR_STALE -> DocViewBundle.message("project.library.status.snapshot.changed");
                case API_NOT_FOUND -> DocViewBundle.message("project.library.status.api.missing");
                case LIMIT_EXCEEDED -> DocViewBundle.message("project.library.status.limit");
                case IO_ERROR -> DocViewBundle.message("project.library.open.failed");
                case INVALID_ARGUMENT, PROJECT_NOT_OPEN -> DocViewBundle.message("project.library.status.failed");
            };
        }
        return DocViewBundle.message("project.library.status.failed");
    }

    private void updateProjectStatusDetail(String text) {
        projectStatusDetail = text == null ? "" : text;
        renderInlineStatus();
    }

    private void renderInlineStatus() {
        if (!SwingUtilities.isEventDispatchThread()) {
            ApplicationManager.getApplication().invokeLater(this::renderInlineStatus);
            return;
        }
        ProjectStatusText status = projectStatusText;
        String detail = projectStatusDetail;
        boolean searching = searchCard.isVisible();
        String fullText = searching
                ? joinStatus(detail, status.full())
                : joinStatus(catalogStatusText, status.full(), detail);
        String visibleText = searching ? joinStatus(detail, status.compact()) : fullText;
        inlineStatus.setText(visibleText);
        inlineStatus.setToolTipText(fullText.isEmpty() ? null : fullText);
        inlineStatus.getAccessibleContext().setAccessibleDescription(inlineStatus.getToolTipText());
        inlineStatus.setVisible(!visibleText.isEmpty() || generateProjectDocsButton.isVisible());
        updateSearchSelectionState();
    }

    private static String joinStatus(String... parts) {
        return Stream.of(parts).filter(part -> !part.isBlank()).distinct().collect(Collectors.joining(" · "));
    }

    private record ProjectStatusText(String full, String compact) {
        private ProjectStatusText(String text) {
            this(text, text);
        }
    }

    private void updateSearchSelectionState() {
        if (openSelectedButton == null) return;
        searchResults.setEnabled(searchSnapshotId != null);
        openSelectedButton.setEnabled(isUsable() && searchResults.getSelectedValue() != null
                && searchSnapshotId != null);
        nextPageButton.setEnabled(isUsable() && searchCursor != null);
    }

    private boolean isUsable() {
        return !disposed && !project.isDisposed() && !toolWindow.isDisposed();
    }

    private void initCatalogTree() {
        catalogTree.setRootVisible(false);
        catalogTree.setShowsRootHandles(true);
        catalogTree.getEmptyText().clear();
        catalogTree.setBorder(BorderFactory.createEmptyBorder());
        catalogTree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
        PopupHandler.installPopupMenu(catalogTree, "liuzhihang.doc.tool.window.catalog.action", ActionPlaces.TOOLWINDOW_CONTENT);
    }

    public void updateCatalogTree() {
        if (!isUsable()) return;
        long request = beginRefresh();
        DumbService.getInstance(project).smartInvokeLater(() -> {
            if (isCurrentRefresh(request)) doUpdateCatalogTree(request);
        });
    }

    private synchronized long beginRefresh() {
        long request = ++refreshGeneration;
        ProgressIndicator previous = refreshIndicator;
        refreshIndicator = null;
        if (previous != null) previous.cancel();
        setCatalogStatus(DocViewBundle.message(DumbService.isDumb(project)
                ? "catalog.status.indexing" : "catalog.status.searching"));
        return request;
    }

    private boolean isCurrentRefresh(long request) {
        return isUsable() && request == refreshGeneration;
    }

    private synchronized boolean registerRefreshIndicator(long request, ProgressIndicator indicator) {
        if (!isCurrentRefresh(request)) {
            indicator.cancel();
            return false;
        }
        refreshIndicator = indicator;
        return true;
    }

    private void doUpdateCatalogTree(long request) {
        setCatalogStatus(DocViewBundle.message("catalog.status.searching"));
        ProgressManager.getInstance().run(new Task.Backgroundable(project,
                DocViewBundle.message("task.catalog.search"), true) {
            private RootNode result;

            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                if (!registerRefreshIndicator(request, indicator)) return;
                result = ApplicationManager.getApplication().runReadAction((com.intellij.openapi.util.Computable<RootNode>) () -> {
                    indicator.checkCanceled();
                    RootNode snapshot = new RootNode();
                    snapshot.updateNode(project);
                    indicator.checkCanceled();
                    return snapshot;
                });
            }

            @Override
            public void onSuccess() {
                completeRefresh(request, result, null, false);
            }

            @Override
            public void onCancel() {
                completeRefresh(request, null, null, true);
            }

            @Override
            public void onThrowable(@NotNull Throwable error) {
                completeRefresh(request, null, error, false);
            }
        });
    }

    private void completeRefresh(long request, RootNode result, Throwable failure, boolean cancelled) {
        if (!isCurrentRefresh(request)) return;
        refreshIndicator = null;
        if (cancelled) {
            setCatalogStatus(DocViewBundle.message("catalog.status.cancelled"));
        } else if (failure != null || result == null) {
            setCatalogStatus(DocViewBundle.message("catalog.status.failed"));
        } else {
            rootNode = result;
            setCatalogStatus(result.getChildCount() == 0 ? DocViewBundle.message("catalog.status.empty") : "");
            treeModel.invalidateAsync();
        }
    }

    private void setCatalogStatus(String text) {
        catalogStatusText = text;
        catalogTree.getEmptyText().setText(text);
        renderInlineStatus();
    }

    static PsiElement sourceElement(SimpleNode node) {
        PsiElement element = node instanceof MethodNode method ? method.getPsiMethod()
                : node instanceof ClassNode type ? type.getPsiClass() : null;
        return element != null && element.isValid() ? element : null;
    }

    public static void openProjectDocuments(@NotNull Project project) {
        runWhenPanelAvailable(project, DocViewWindowPanel::openProjectDocs);
    }

    public static void showProjectDocumentSearch(@NotNull Project project) {
        runWhenPanelAvailable(project, DocViewWindowPanel::showProjectSearch);
    }

    public static void setProjectDocumentStatus(@NotNull Project project, @NotNull String message) {
        if (project.isDisposed()) return;
        ApplicationManager.getApplication().invokeLater(() -> {
            DocViewWindowPanel panel = findPanel(project);
            if (panel != null && panel.isUsable()) panel.updateProjectStatusDetail(message);
        });
    }

    private static void runWhenPanelAvailable(Project project, Consumer<DocViewWindowPanel> operation) {
        if (project.isDisposed()) return;
        ToolWindow window = ToolWindowManager.getInstance(project).getToolWindow(TOOL_WINDOW_ID);
        if (window == null || window.isDisposed()) return;
        window.activate(() -> ApplicationManager.getApplication().invokeLater(() -> {
            DocViewWindowPanel panel = findPanel(project);
            if (panel != null && panel.isUsable()) operation.accept(panel);
        }));
    }

    private static DocViewWindowPanel findPanel(Project project) {
        if (project.isDisposed()) return null;
        ToolWindow window = ToolWindowManager.getInstance(project).getToolWindow(TOOL_WINDOW_ID);
        if (window == null || window.isDisposed()) return null;
        for (var content : window.getContentManager().getContents()) {
            if (content.getComponent() instanceof DocViewWindowPanel panel) return panel;
        }
        return null;
    }

    @Override
    public void dispose() {
        ProgressIndicator refresh;
        ProgressIndicator status;
        ProgressIndicator action;
        synchronized (this) {
            if (disposed) return;
            disposed = true;
            refreshGeneration++;
            documentStatusGeneration++;
            documentActionGeneration++;
            refresh = refreshIndicator;
            status = documentStatusIndicator;
            action = documentActionIndicator;
            refreshIndicator = null;
            documentStatusIndicator = null;
            documentActionIndicator = null;
        }
        cancelPendingEditorOpen();
        if (refresh != null) refresh.cancel();
        if (status != null) status.cancel();
        if (action != null) action.cancel();
        projectStatusRefreshAlarm.cancelAllRequests();
        liveSearchAlarm.cancelAllRequests();

        var application = ApplicationManager.getApplication();
        if (application.isDispatchThread()) {
            disposeCatalogTree();
        } else {
            application.invokeAndWait(this::disposeCatalogTree, ModalityState.any());
        }
    }

    private void disposeCatalogTree() {
        ApplicationManager.getApplication().assertIsDispatchThread();
        catalogTreeSpeedSearch.hidePopup();
        catalogTree.clearSelection();
        catalogTree.setModel(new DefaultTreeModel(new DefaultMutableTreeNode()));
        for (MouseListener listener : catalogTree.getMouseListeners()) {
            catalogTree.removeMouseListener(listener);
        }
        catalogTree.setUI(null);
        Container parent = catalogTree.getParent();
        if (parent != null) {
            parent.remove(catalogTree);
            parent.revalidate();
            parent.repaint();
        }
    }

    @Override
    public void uiDataSnapshot(@NotNull DataSink sink) {
        super.uiDataSnapshot(sink);
        sink.set(CommonDataKeys.PROJECT, project);
        sink.set(DocViewDataKeys.WINDOW_PANE, this);
        sink.set(DocViewDataKeys.WINDOW_ROOT_NODE, rootNode);
        sink.set(DocViewDataKeys.WINDOW_CATALOG_TREE, catalogTree);
        sink.set(DocViewDataKeys.WINDOW_TOOLBAR, getToolbar());
        SimpleNode selected = catalogTree.getSelectedNode();
        sink.lazy(CommonDataKeys.PSI_ELEMENT, () -> sourceElement(selected));
        sink.lazy(CommonDataKeys.NAVIGATABLE, () -> {
            PsiElement element = sourceElement(selected);
            return element instanceof Navigatable navigatable ? navigatable : null;
        });
    }
}
