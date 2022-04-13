package com.liuzhihang.doc.view.dom;

import com.intellij.util.xml.DomElement;
import com.intellij.util.xml.SubTagList;

import java.util.List;

/**
 * 解析 xml 将其中的 beans 标签映射为 BeansDomElement
 * <p>
 * https://jetbrains.org/intellij/sdk/docs/reference_guide/frameworks_and_external_apis/xml_dom_api.html
 *
 * @author liuzhihang
 * @date 2022/4/11 23:40
 */
public interface BeansDomElement extends DomElement {

    @SubTagList("dubbo:service")
    List<DubboServiceDomElement> getDubboServiceDomElements();
}
