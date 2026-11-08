// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.liuzhihang.doc.view;

import com.intellij.DynamicBundle;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.PropertyKey;

public final class DocViewBundle {

    @NonNls
    private static final String BUNDLE = "messages.DocViewBundle";
    private static final DynamicBundle INSTANCE = new DynamicBundle(DocViewBundle.class, BUNDLE);

    private DocViewBundle() {
    }

    @NotNull
    public static String message(@NotNull @PropertyKey(resourceBundle = BUNDLE) String key, Object... params) {
        return INSTANCE.getMessage(key, params);
    }
}
