package com.liuzhihang.doc.view.service.document;

import com.intellij.openapi.project.Project;
import com.liuzhihang.doc.view.dto.Body;
import com.liuzhihang.doc.view.dto.DocView;
import com.liuzhihang.doc.view.dto.DocViewData;
import com.liuzhihang.doc.view.dto.DocViewParamData;
import com.liuzhihang.doc.view.dto.Param;
import com.liuzhihang.doc.view.enums.FrameworkEnum;
import com.liuzhihang.doc.view.service.export.ProjectDocumentIdentity;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/** Projects the existing Doc View parse result into an immutable, PSI-free JSON model. */
public final class ProjectDocumentProjection {

    private ProjectDocumentProjection() {
    }

    public static ProjectDocumentData.ApiData fromDocView(DocView docView,
                                                          ProjectDocumentIdentity identity,
                                                          String framework,
                                                          ProjectDocumentData.SourceData source) {
        Objects.requireNonNull(docView, "docView");
        Objects.requireNonNull(identity, "identity");
        Objects.requireNonNull(source, "source");
        validateSourceBounds(docView);

        Project project = docView.getPsiClass() == null ? null : docView.getPsiClass().getProject();
        if (project == null) {
            throw new IllegalArgumentException("Doc View projection requires its source project");
        }
        DocViewData viewData = new DocViewData(project, docView);
        String normalizedFramework = normalizeFramework(framework);
        List<String> tags = tags(docView.getClassTags(), docView.getTags());
        String httpMethod = httpValue(normalizedFramework, docView.getMethod(), true);
        String httpPath = httpValue(normalizedFramework, docView.getPath(), false);
        boolean methodParameters = docView.getType() == FrameworkEnum.DUBBO;
        ProjectionBudget projectionBudget = new ProjectionBudget();

        return new ProjectDocumentData.ApiData(
                identity.sha256(),
                identity(identity),
                text(viewData.getName()),
                text(viewData.getDesc()),
                normalizedFramework,
                httpMethod,
                httpPath,
                tags,
                source,
                headers(viewData.getRequestHeaderDataList()),
                fields(viewData.getRequestParamDataList(), projectionBudget),
                body(docView.getReqBody(), viewData.getRequestBodyDataList(), identity, methodParameters, true, projectionBudget),
                body(docView.getRespBody(), viewData.getResponseParamDataList(), identity, methodParameters, false, projectionBudget),
                new ProjectDocumentData.ExamplesData(nullableText(viewData.getRequestExample()),
                        nullableText(viewData.getResponseExample())),
                text(docView.getRemark()));
    }

    private static void validateSourceBounds(DocView docView) {
        ProjectionBudget budget = new ProjectionBudget();
        List<?> headers = docView.getHeaderList();
        if (headers != null && headers.size() > ProjectDocumentFormat.MAX_FIELDS_PER_API) {
            throw ProjectDocumentFormat.limitExceeded("API header count exceeds the item limit");
        }
        List<Param> parameters = docView.getReqParamList();
        if (parameters != null) budget.add(parameters.size());
        countBodyTree(docView.getReqBody(), budget);
        countBodyTree(docView.getRespBody(), budget);
    }

    private static void countBodyTree(Body root, ProjectionBudget budget) {
        if (root == null || root.getChildList() == null || root.getChildList().isEmpty()) return;
        ArrayDeque<BodyDepth> pending = new ArrayDeque<>();
        budget.add(root.getChildList().size());
        for (Body child : root.getChildList()) pending.addLast(new BodyDepth(child, 1));
        while (!pending.isEmpty()) {
            BodyDepth current = pending.removeFirst();
            List<Body> children = current.body().getChildList();
            if (children == null || children.isEmpty()) continue;
            int childDepth = current.depth() + 1;
            if (childDepth > ProjectDocumentFormat.MAX_FIELD_DEPTH) {
                throw ProjectDocumentFormat.limitExceeded("API field depth exceeds the limit");
            }
            budget.add(children.size());
            for (Body child : children) pending.addLast(new BodyDepth(child, childDepth));
        }
    }

    private static ProjectDocumentData.IdentityData identity(ProjectDocumentIdentity identity) {
        return new ProjectDocumentData.IdentityData(identity.moduleName(), identity.qualifiedClassName(),
                identity.methodName(), identity.parameterTypes(), identity.displaySignature(), identity.canonicalForm());
    }

    private static String normalizeFramework(String framework) {
        if (framework == null || framework.isBlank()) {
            throw new IllegalArgumentException("framework must not be blank");
        }
        String normalized = framework.toLowerCase(Locale.ROOT);
        if (!List.of("spring", "feign", "dubbo", "interface").contains(normalized)) {
            throw new IllegalArgumentException("framework is not supported");
        }
        return normalized;
    }

    private static String httpValue(String framework, String value, boolean method) {
        if (!"spring".equals(framework) && !"feign".equals(framework)) return null;
        if (value == null || value.isBlank()) return null;
        if (!method) return value;
        String normalized = value.trim().toUpperCase(Locale.ROOT);
        return List.of("GET", "HEAD", "POST", "PUT", "DELETE", "CONNECT", "OPTIONS", "TRACE", "PATCH")
                .contains(normalized) ? normalized : null;
    }

    private static List<String> tags(String[] classTags, String[] methodTags) {
        LinkedHashSet<String> values = new LinkedHashSet<>();
        addTags(values, classTags);
        addTags(values, methodTags);
        return List.copyOf(values);
    }

    private static void addTags(LinkedHashSet<String> values, String[] tags) {
        if (tags == null) return;
        for (String tag : tags) {
            if (tag != null && !tag.isBlank()) values.add(tag.trim());
        }
    }

    private static List<ProjectDocumentData.HeaderData> headers(List<DocViewParamData> source) {
        if (source == null || source.isEmpty()) return List.of();
        if (source.size() > ProjectDocumentFormat.MAX_FIELDS_PER_API) {
            throw ProjectDocumentFormat.limitExceeded("API header count exceeds the item limit");
        }
        List<ProjectDocumentData.HeaderData> result = new ArrayList<>(source.size());
        for (DocViewParamData header : source) {
            result.add(new ProjectDocumentData.HeaderData(text(header.getName()), text(header.getDesc()),
                    header.getRequired(), nullableText(header.getExample())));
        }
        return List.copyOf(result);
    }

    private static List<ProjectDocumentData.FieldData> fields(List<DocViewParamData> source, ProjectionBudget budget) {
        if (source == null || source.isEmpty()) return List.of();
        if (source.size() > ProjectDocumentFormat.MAX_FIELDS_PER_API) {
            throw ProjectDocumentFormat.limitExceeded("API field list exceeds the item limit");
        }
        List<ProjectDocumentData.FieldData> result = new ArrayList<>(source.size());
        for (DocViewParamData field : source) result.add(field(field, 1, budget));
        return List.copyOf(result);
    }

    private static ProjectDocumentData.FieldData field(DocViewParamData source, int depth, ProjectionBudget budget) {
        if (depth > ProjectDocumentFormat.MAX_FIELD_DEPTH) {
            throw ProjectDocumentFormat.limitExceeded("API field depth exceeds the limit");
        }
        budget.add(1);
        List<ProjectDocumentData.FieldData> children = fields(source.getChildList(), depth + 1, budget);
        return new ProjectDocumentData.FieldData(text(source.getName()), text(source.getType()), text(source.getDesc()),
                source.getRequired(), nullableText(source.getExample()), nullableText(source.getSince()),
                nullableText(source.getVersion()), source.isCollection(), source.isMap(), children);
    }

    private static List<ProjectDocumentData.FieldData> fields(List<DocViewParamData> source,
                                                                int depth,
                                                                ProjectionBudget budget) {
        if (source == null || source.isEmpty()) return List.of();
        if (source.size() > ProjectDocumentFormat.MAX_FIELDS_PER_API) {
            throw ProjectDocumentFormat.limitExceeded("API field list exceeds the item limit");
        }
        List<ProjectDocumentData.FieldData> result = new ArrayList<>(source.size());
        for (DocViewParamData field : source) result.add(field(field, depth, budget));
        return List.copyOf(result);
    }

    private static ProjectDocumentData.BodyData body(Body source,
                                                    List<DocViewParamData> fields,
                                                    ProjectDocumentIdentity identity,
                                                    boolean methodParameters,
                                                    boolean request,
                                                    ProjectionBudget budget) {
        if (source == null) return null;
        List<ProjectDocumentData.FieldData> projectedFields = fields(fields, budget);
        String type = source.getQualifiedNameForClassType();
        if (methodParameters && request) {
            type = identity.displaySignature();
        } else if (type == null || type.isBlank()) {
            if (projectedFields.size() == 1) {
                type = projectedFields.get(0).type();
            } else if (!projectedFields.isEmpty()) {
                type = "object";
            } else {
                return null;
            }
        }
        return new ProjectDocumentData.BodyData(type, projectedFields);
    }

    private static String text(String value) {
        return value == null ? "" : value;
    }

    private static String nullableText(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    private record BodyDepth(Body body, int depth) {
    }

    private static final class ProjectionBudget {
        private int count;

        private void add(int amount) {
            if (amount < 0 || count > ProjectDocumentFormat.MAX_FIELDS_PER_API - amount) {
                throw ProjectDocumentFormat.limitExceeded("API field count exceeds the item limit");
            }
            count += amount;
        }
    }
}
