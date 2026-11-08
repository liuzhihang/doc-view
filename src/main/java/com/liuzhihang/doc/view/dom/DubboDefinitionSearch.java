package com.liuzhihang.doc.view.dom;

import com.intellij.openapi.application.QueryExecutorBase;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.searches.DefinitionsScopedSearch;
import com.intellij.psi.xml.XmlElement;
import com.intellij.util.Processor;
import org.jetbrains.annotations.NotNull;

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

        if (element instanceof PsiClass psiClass) {
            Project project = psiClass.getProject();
            String qualifiedName = psiClass.getQualifiedName();
            if (qualifiedName == null) {
                return;
            }

            DubboXmlPsi.processServiceTags(project, GlobalSearchScope.allScope(project), serviceTag -> {
                if (!Objects.equals(qualifiedName, DubboXmlPsi.getInterfaceName(serviceTag))) {
                    return true;
                }
                return consumer.process(serviceTag);
            });
        }

    }
}
