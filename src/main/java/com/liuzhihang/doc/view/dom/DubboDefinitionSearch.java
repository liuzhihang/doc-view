package com.liuzhihang.doc.view.dom;

import com.intellij.openapi.application.QueryExecutorBase;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiTypeParameterListOwner;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.searches.DefinitionsScopedSearch;
import com.intellij.psi.xml.XmlElement;
import com.intellij.util.Processor;
import com.intellij.util.xml.DomElement;
import com.intellij.util.xml.DomFileElement;
import com.intellij.util.xml.DomService;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Objects;

/**
 * 加载所有 xml 中的 dubbo 接口
 *
 * @author liuzhihang
 * @date 2022-04-12 00:05:11
 */
public class DubboDefinitionSearch extends QueryExecutorBase<XmlElement, DefinitionsScopedSearch.SearchParameters> {

    public DubboDefinitionSearch() {
        super(true);
    }

    @Override
    public void processQuery(@NotNull DefinitionsScopedSearch.SearchParameters parameters,
                             @NotNull Processor<? super XmlElement> consumer) {

        PsiElement element = parameters.getElement();

        if (element instanceof PsiTypeParameterListOwner) {
            Processor<DomElement> processor = domElement -> consumer.process(domElement.getXmlElement());
            if (element instanceof PsiClass) {
                PsiClass psiClass = (PsiClass) element;
                Project project = psiClass.getProject();
                // 当前项目的所有元素 beans
                List<DomFileElement<BeansDomElement>> fileElements = DomService.getInstance()
                        .getFileElements(BeansDomElement.class, project, GlobalSearchScope.allScope(project));
                // 只需要判断 interface
                String qualifiedName = psiClass.getQualifiedName();

                for (DomFileElement<BeansDomElement> beansDomFileElement : fileElements) {
                    BeansDomElement rootElement = beansDomFileElement.getRootElement();

                    for (DubboServiceDomElement dubboServiceDomElement : rootElement.getDubboServiceDomElements()) {
                        String interfaceQualifiedName = dubboServiceDomElement.getInterface().getStringValue();
                        if (Objects.equals(qualifiedName, interfaceQualifiedName)) {
                            processor.process(dubboServiceDomElement);
                        }
                    }
                }
            }
        }

    }
}
