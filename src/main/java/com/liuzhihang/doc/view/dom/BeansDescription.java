package com.liuzhihang.doc.view.dom;

import com.intellij.openapi.module.Module;
import com.intellij.psi.xml.XmlFile;
import com.intellij.psi.xml.XmlTag;
import com.intellij.util.xml.DomFileDescription;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author liuzhihang
 * @link https://jetbrains.org/intellij/sdk/docs/reference_guide/frameworks_and_external_apis/xml_dom_api.html
 * @date 2022-04-11 23:49:37
 */
public class BeansDescription extends DomFileDescription<BeansDomElement> {

    public BeansDescription() {
        super(BeansDomElement.class, "beans");
    }

    @Override
    public boolean isMyFile(@NotNull XmlFile file, @Nullable Module module) {
        XmlTag rootTag = file.getRootTag();
        return rootTag != null && rootTag.getName().equals(getRootTagName());
    }

    @Override
    public boolean acceptsOtherRootTagNames() {
        return true;
    }
}
