package com.liuzhihang.doc.view.enums;

import com.liuzhihang.doc.view.DocViewBundle;
import org.jetbrains.annotations.NotNull;

/** Stable upload target identity, independent from its localized UI label. */
public enum UploadPlatform {
    YAPI("yapi", "platform.name.yapi"),
    SHOW_DOC("showdoc", "platform.name.showdoc"),
    YUQUE("yuque", "platform.name.yuque");

    private final String id;
    private final String displayNameKey;

    UploadPlatform(String id, String displayNameKey) {
        this.id = id;
        this.displayNameKey = displayNameKey;
    }

    @NotNull
    public String getId() {
        return id;
    }

    @NotNull
    public String getDisplayName() {
        return DocViewBundle.message(displayNameKey);
    }
}
