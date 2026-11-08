package com.liuzhihang.doc.view.service.document;

import com.intellij.util.messages.Topic;
import org.jetbrains.annotations.Nullable;

import java.util.EventListener;

/** Signals observed project-document state changes so UI components can refresh asynchronously. */
@FunctionalInterface
public interface ProjectDocumentIndexListener extends EventListener {

    Topic<ProjectDocumentIndexListener> TOPIC = Topic.create(
            "Doc View project document index state",
            ProjectDocumentIndexListener.class,
            Topic.BroadcastDirection.NONE);

    /** The listener must schedule any disk-backed status refresh on a background thread. */
    void stateChanged(long inputEpoch, @Nullable String committedSnapshotId);
}
