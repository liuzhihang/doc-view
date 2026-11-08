package com.liuzhihang.doc.view.service.document;

import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProcessCanceledException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/** Coordinates in-process readers and the single publisher for one canonical project document root. */
public final class DocumentStoreLocks {

    private static final long MAX_POLL_NANOS = TimeUnit.MILLISECONDS.toNanos(40);
    private static final Object STATES_MONITOR = new Object();
    private static final ConcurrentHashMap<Path, State> STATES = new ConcurrentHashMap<>();

    private DocumentStoreLocks() {
    }

    /** Returns {@code null} when the bounded wait expires; cancellation throws {@link ProcessCanceledException}. */
    public static Lease tryAcquireRead(Path projectRoot, ProgressIndicator indicator, Duration timeout) throws IOException {
        return tryAcquire(projectRoot, indicator, timeout, false);
    }

    /** Returns {@code null} when the bounded wait expires; cancellation throws {@link ProcessCanceledException}. */
    public static Lease tryAcquireWrite(Path projectRoot, ProgressIndicator indicator, Duration timeout) throws IOException {
        return tryAcquire(projectRoot, indicator, timeout, true);
    }

    /** Resolves the project-local document root without following a symlink beneath the project root. */
    public static Path canonicalDocumentRoot(Path projectRoot) throws IOException {
        Objects.requireNonNull(projectRoot, "projectRoot");
        Path realProjectRoot = projectRoot.toRealPath();
        Path current = realProjectRoot;
        boolean missingParent = false;
        for (String segment : new String[]{"docs", "doc-view"}) {
            current = current.resolve(segment);
            if (missingParent) {
                continue;
            }
            if (!Files.exists(current, LinkOption.NOFOLLOW_LINKS)) {
                missingParent = true;
                continue;
            }
            BasicFileAttributes attributes = Files.readAttributes(current, BasicFileAttributes.class,
                    LinkOption.NOFOLLOW_LINKS);
            if (attributes.isSymbolicLink() || !attributes.isDirectory()) {
                throw new IOException("Project document root contains a non-directory or symbolic link");
            }
            Path realComponent = current.toRealPath();
            if (!realComponent.startsWith(realProjectRoot)) {
                throw new IOException("Project document root escapes the project directory");
            }
            current = realComponent;
        }
        return current.normalize();
    }

    private static Lease tryAcquire(Path projectRoot,
                                    ProgressIndicator indicator,
                                    Duration timeout,
                                    boolean write) throws IOException {
        Objects.requireNonNull(indicator, "indicator");
        Objects.requireNonNull(timeout, "timeout");
        if (timeout.isNegative()) {
            throw new IllegalArgumentException("timeout must not be negative");
        }
        Path root = canonicalDocumentRoot(projectRoot);
        State state = retain(root);
        Lock lock = write ? state.lock.writeLock() : state.lock.readLock();
        long timeoutNanos;
        try {
            timeoutNanos = timeout.toNanos();
        } catch (ArithmeticException overflow) {
            timeoutNanos = Long.MAX_VALUE;
        }
        long started = System.nanoTime();
        boolean acquired = false;
        try {
            while (true) {
                indicator.checkCanceled();
                long elapsed = System.nanoTime() - started;
                long remaining = timeoutNanos == Long.MAX_VALUE ? Long.MAX_VALUE : timeoutNanos - elapsed;
                if (remaining <= 0) {
                    return null;
                }
                long slice = Math.min(remaining, MAX_POLL_NANOS);
                try {
                    acquired = lock.tryLock(slice, TimeUnit.NANOSECONDS);
                } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                    throw new ProcessCanceledException();
                }
                if (acquired) {
                    return new Lease(root, state, lock);
                }
            }
        } finally {
            if (!acquired) {
                releaseReference(root, state);
            }
        }
    }

    private static State retain(Path root) {
        synchronized (STATES_MONITOR) {
            State state = STATES.computeIfAbsent(root, ignored -> new State());
            state.references++;
            return state;
        }
    }

    private static void releaseReference(Path root, State state) {
        synchronized (STATES_MONITOR) {
            state.references--;
            if (state.references == 0) {
                STATES.remove(root, state);
            }
        }
    }

    private static final class State {
        private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock(true);
        private int references;
    }

    public static final class Lease implements AutoCloseable {
        private final Path root;
        private final State state;
        private final Lock lock;
        private final AtomicBoolean closed = new AtomicBoolean();

        private Lease(Path root, State state, Lock lock) {
            this.root = root;
            this.state = state;
            this.lock = lock;
        }

        public Path documentRoot() {
            return root;
        }

        @Override
        public void close() {
            if (closed.compareAndSet(false, true)) {
                lock.unlock();
                releaseReference(root, state);
            }
        }
    }
}
