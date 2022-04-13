package com.liuzhihang.doc.view.dom;

import com.intellij.util.xml.Attribute;
import com.intellij.util.xml.DomElement;
import com.intellij.util.xml.GenericAttributeValue;
import com.intellij.util.xml.NameValue;

/**
 * xml 配置中的 <dubbo:service/> 标签
 *
 * @author liuzhihang
 * @date 2022/4/11 23:54
 */
public interface DubboServiceDomElement extends DomElement {

    /**
     * interface 属性, 就是接口的全路径
     *
     * @return
     */
    @NameValue
    @Attribute("interface")
    GenericAttributeValue<String> getInterface();
}
