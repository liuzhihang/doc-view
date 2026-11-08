package com.liuzhihang.doc.view.service.http;

import com.google.gson.JsonParser;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.CommonClassNames;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiAnnotationMemberValue;
import com.intellij.psi.PsiArrayInitializerMemberValue;
import com.intellij.psi.PsiArrayType;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiClassType;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiExpression;
import com.intellij.psi.PsiJavaCodeReferenceElement;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiModifier;
import com.intellij.psi.PsiModifierList;
import com.intellij.psi.PsiModifierListOwner;
import com.intellij.psi.PsiParameter;
import com.intellij.psi.PsiPrimitiveType;
import com.intellij.psi.PsiReferenceExpression;
import com.intellij.psi.PsiType;
import com.intellij.psi.util.PsiUtil;
import com.intellij.psi.util.InheritanceUtil;
import com.liuzhihang.doc.view.constant.FieldTypeConstant;
import com.liuzhihang.doc.view.utils.ParamPsiUtils;
import com.liuzhihang.doc.view.utils.SpringPsiUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Builds a single editable HTTP Client request from a Spring or Feign method. */
public class HttpRequestBuilder {

    private static final String REST_CONTROLLER = "org.springframework.web.bind.annotation.RestController";
    private static final String CONTROLLER = "org.springframework.stereotype.Controller";
    private static final String FEIGN_CLIENT = "org.springframework.cloud.openfeign.FeignClient";
    private static final String REQUEST_MAPPING = "org.springframework.web.bind.annotation.RequestMapping";
    private static final String REQUEST_PARAM = "org.springframework.web.bind.annotation.RequestParam";
    private static final String PATH_VARIABLE = "org.springframework.web.bind.annotation.PathVariable";
    private static final String REQUEST_HEADER = "org.springframework.web.bind.annotation.RequestHeader";
    private static final String REQUEST_BODY = "org.springframework.web.bind.annotation.RequestBody";
    private static final String REQUEST_PART = "org.springframework.web.bind.annotation.RequestPart";
    private static final String MODEL_ATTRIBUTE = "org.springframework.web.bind.annotation.ModelAttribute";
    private static final String COOKIE_VALUE = "org.springframework.web.bind.annotation.CookieValue";
    private static final String OPEN_API_PARAMETER = "io.swagger.v3.oas.annotations.Parameter";
    private static final Set<String> CANDIDATE_FRAMEWORK_ANNOTATIONS = Set.of(
            "RestController", "Controller", "FeignClient");
    private static final Set<String> CANDIDATE_MAPPING_ANNOTATIONS = Set.of(
            "RequestMapping", "GetMapping", "PostMapping", "PutMapping", "DeleteMapping", "PatchMapping");

    private static final Map<String, String> FIXED_MAPPING_METHODS = Map.of(
            "org.springframework.web.bind.annotation.GetMapping", "GET",
            "org.springframework.web.bind.annotation.PostMapping", "POST",
            "org.springframework.web.bind.annotation.PutMapping", "PUT",
            "org.springframework.web.bind.annotation.DeleteMapping", "DELETE",
            "org.springframework.web.bind.annotation.PatchMapping", "PATCH");
    private static final Set<String> MAPPING_ANNOTATIONS;
    private static final Set<String> SUPPORTED_SCALAR_TYPES = Set.of(
            "java.lang.String",
            "boolean", "byte", "short", "int", "long", "float", "double",
            "java.lang.Boolean", "java.lang.Byte", "java.lang.Short", "java.lang.Integer",
            "java.lang.Long", "java.lang.Float", "java.lang.Double");
    private static final Set<String> AUTHENTICATION_HEADERS = Set.of(
            "authorization", "proxy-authorization", "x-api-key", "api-key", "apikey");
    private static final Pattern PATH_VARIABLE_PATTERN = Pattern.compile("\\{([^{}]+)}");
    private static final Pattern HTTP_CLIENT_VARIABLE_PATTERN = Pattern.compile("\\{\\{[^{}\\s]+}}");
    private static final Pattern SAFE_HEADER_NAME = Pattern.compile("[!#$%&'*+.^_`|~0-9A-Za-z-]+");
    private static final String SPRING_DEFAULT_VALUE_SENTINEL =
            "\n\t\t\n\t\t\n\uE000\uE001\uE002\n\t\t\t\t\n";
    private static final int MAX_ANCESTORS = 64;

    static {
        LinkedHashSet<String> names = new LinkedHashSet<>(FIXED_MAPPING_METHODS.keySet());
        names.add(REQUEST_MAPPING);
        MAPPING_ANNOTATIONS = Set.copyOf(names);
    }

    /**
     * A cheap candidate check for action visibility. It examines only the method, its owner,
     * and their direct annotations; full mapping and parameter validation belongs to {@link #build(PsiMethod)}.
     */
    public static boolean isCandidate(@Nullable PsiMethod method) {
        if (method == null || !method.isValid() || method.isConstructor()
                || method.hasModifierProperty(PsiModifier.STATIC)) {
            return false;
        }
        PsiClass owner = method.getContainingClass();
        if (owner == null || !owner.isValid()
                || !hasDirectAnnotationSimpleName(owner, CANDIDATE_FRAMEWORK_ANNOTATIONS)) {
            return false;
        }
        return hasDirectAnnotationSimpleName(method, CANDIDATE_MAPPING_ANNOTATIONS);
    }

    /** Builds one immutable request text snapshot from the selected method's current PSI. */
    @NotNull
    public HttpRequestSnapshot build(@NotNull PsiMethod method) throws HttpRequestException {
        if (method == null || !method.isValid() || method.isConstructor()) {
            throw failure(HttpRequestException.INVALID_SOURCE);
        }
        PsiClass owner = method.getContainingClass();
        if (owner == null || !owner.isValid()) {
            throw failure(HttpRequestException.INVALID_SOURCE);
        }
        if (!isSupportedFrameworkClass(owner) || method.hasModifierProperty(PsiModifier.STATIC)) {
            throw failure(HttpRequestException.UNSUPPORTED_METHOD);
        }

        try {
            Endpoint endpoint = parseEndpoint(method, owner);
            ParsedParameters parameters = parseParameters(method);
            RequestModel request = createRequest(endpoint, parameters);
            return new HttpRequestSnapshot(render(request));
        } catch (ProcessCanceledException exception) {
            throw exception;
        } catch (HttpRequestException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            // Do not expose PSI text, endpoint values, or a nested message that might contain secrets.
            throw failure(HttpRequestException.BUILD_FAILED);
        }
    }

    private static boolean isSupportedFrameworkClass(PsiClass psiClass) {
        return hasAnnotation(psiClass, REST_CONTROLLER)
                || hasAnnotation(psiClass, CONTROLLER)
                || hasAnnotation(psiClass, FEIGN_CLIENT);
    }

    private static Endpoint parseEndpoint(PsiMethod method, PsiClass owner) throws HttpRequestException {
        if (hasInheritedMapping(method, owner) || hasComposedMapping(method) || hasComposedMapping(owner)) {
            throw failure(HttpRequestException.UNSUPPORTED_MAPPING);
        }

        List<PsiAnnotation> methodMappings = findDirectMappingAnnotations(method);
        if (methodMappings.size() != 1) {
            throw failure(HttpRequestException.UNSUPPORTED_MAPPING);
        }
        PsiAnnotation classRequestMapping = findAnnotation(owner, REQUEST_MAPPING);
        if (hasClassMethodMapping(owner)) {
            throw failure(HttpRequestException.UNSUPPORTED_MAPPING);
        }

        MappingInfo classMapping = classRequestMapping == null ? MappingInfo.empty() : parseMapping(classRequestMapping);
        MappingInfo methodMapping = parseMapping(methodMappings.get(0));
        String methodName = resolveMethod(classMapping.methods(), methodMapping.methods());
        String feignPath = feignClientPath(owner);
        String classMappingPath = singlePath(classMapping.paths());
        String methodPath = singlePath(methodMapping.paths());
        validateStaticPath(classMappingPath);
        validateStaticPath(methodPath);
        String classPath = joinPath(feignPath, classMappingPath);
        String path = normalizePath(joinPath(classPath, methodPath));
        validateStaticPath(path);

        List<String> effectiveConsumes = methodMapping.consumes().isEmpty()
                ? classMapping.consumes() : methodMapping.consumes();
        if (effectiveConsumes.size() > 1) {
            throw failure(HttpRequestException.UNSUPPORTED_MEDIA_TYPE);
        }
        String consumes = effectiveConsumes.isEmpty() ? "" : normalizeMediaType(effectiveConsumes.get(0));
        return new Endpoint(methodName, path, consumes, pathVariableNames(path));
    }

    private static String feignClientPath(PsiClass owner) throws HttpRequestException {
        PsiAnnotation feignClient = findAnnotation(owner, FEIGN_CLIENT);
        if (feignClient == null) {
            return "";
        }
        PsiAnnotationMemberValue declaredPath = feignClient.findDeclaredAttributeValue("path");
        if (declaredPath == null) {
            return "";
        }
        Object constant = constantValue(declaredPath);
        if (!(constant instanceof String path)) {
            throw failure(HttpRequestException.UNSUPPORTED_MAPPING);
        }
        if (isBlank(path)) {
            return "";
        }
        validateStaticPath(path);
        if (PATH_VARIABLE_PATTERN.matcher(path).find()) {
            throw failure(HttpRequestException.UNSUPPORTED_MAPPING);
        }
        return path;
    }

    private static boolean hasClassMethodMapping(PsiClass owner) {
        PsiModifierList modifierList = owner.getModifierList();
        if (modifierList == null) {
            return false;
        }
        for (PsiAnnotation annotation : modifierList.getAnnotations()) {
            if (MAPPING_ANNOTATIONS.contains(annotation.getQualifiedName())
                    && !REQUEST_MAPPING.equals(annotation.getQualifiedName())) {
                return true;
            }
        }
        return false;
    }

    private static MappingInfo parseMapping(PsiAnnotation annotation) throws HttpRequestException {
        List<String> rawValuePaths = stringValues(annotation, "value");
        List<String> rawPathPaths = stringValues(annotation, "path");
        if (rawValuePaths.size() > 1 || rawPathPaths.size() > 1) {
            throw failure(HttpRequestException.UNSUPPORTED_MAPPING);
        }
        List<String> valuePaths = nonBlankValues(rawValuePaths);
        List<String> pathPaths = nonBlankValues(rawPathPaths);
        if (!valuePaths.isEmpty() && !pathPaths.isEmpty() && !valuePaths.equals(pathPaths)) {
            throw failure(HttpRequestException.UNSUPPORTED_MAPPING);
        }
        List<String> paths = pathPaths.isEmpty() ? valuePaths : pathPaths;
        if (paths.size() > 1) {
            throw failure(HttpRequestException.UNSUPPORTED_MAPPING);
        }

        if (!nonBlankValues(stringValues(annotation, "params")).isEmpty()
                || !nonBlankValues(stringValues(annotation, "headers")).isEmpty()) {
            throw failure(HttpRequestException.UNSUPPORTED_MAPPING);
        }

        List<String> consumes = stringValues(annotation, "consumes");
        if (consumes.size() > 1) {
            throw failure(HttpRequestException.UNSUPPORTED_MEDIA_TYPE);
        }

        String annotationName = annotation.getQualifiedName();
        List<String> methods;
        String fixedMethod = FIXED_MAPPING_METHODS.get(annotationName);
        if (fixedMethod != null) {
            methods = List.of(fixedMethod);
        } else {
            methods = enumNames(annotation, "method");
        }
        return new MappingInfo(paths, methods, nonBlankValues(consumes));
    }

    private static String resolveMethod(List<String> classMethods, List<String> methodMethods)
            throws HttpRequestException {
        if (classMethods.size() > 1 || methodMethods.size() > 1) {
            throw failure(HttpRequestException.UNSUPPORTED_MAPPING);
        }
        List<String> effectiveMethods = methodMethods.isEmpty() ? classMethods : methodMethods;
        if (effectiveMethods.size() != 1) {
            throw failure(HttpRequestException.UNSUPPORTED_MAPPING);
        }
        String resolved = effectiveMethods.get(0).toUpperCase(Locale.ROOT);
        if (!Set.of("GET", "POST", "PUT", "DELETE", "PATCH", "HEAD", "OPTIONS", "TRACE").contains(resolved)) {
            throw failure(HttpRequestException.UNSUPPORTED_MAPPING);
        }
        if (!methodMethods.isEmpty() && !classMethods.isEmpty()) {
            Set<String> allowed = new HashSet<>();
            for (String classMethod : classMethods) {
                allowed.add(classMethod.toUpperCase(Locale.ROOT));
            }
            if (!allowed.contains(resolved)) {
                throw failure(HttpRequestException.UNSUPPORTED_MAPPING);
            }
        }
        return resolved;
    }

    private static String singlePath(List<String> paths) throws HttpRequestException {
        if (paths.isEmpty()) {
            return "";
        }
        if (paths.size() != 1) {
            throw failure(HttpRequestException.UNSUPPORTED_MAPPING);
        }
        return paths.get(0);
    }

    private static String joinPath(String classPath, String methodPath) {
        String left = classPath == null ? "" : classPath.trim();
        String right = methodPath == null ? "" : methodPath.trim();
        if (left.isEmpty()) {
            return right;
        }
        if (right.isEmpty()) {
            return left;
        }
        return stripTrailingSlashes(left) + "/" + stripLeadingSlashes(right);
    }

    private static String normalizePath(String path) {
        if (path == null || path.isBlank()) {
            return "/";
        }
        return "/" + stripLeadingSlashes(path.trim());
    }

    private static String stripLeadingSlashes(String value) {
        int index = 0;
        while (index < value.length() && value.charAt(index) == '/') {
            index++;
        }
        return value.substring(index);
    }

    private static String stripTrailingSlashes(String value) {
        int end = value.length();
        while (end > 0 && value.charAt(end - 1) == '/') {
            end--;
        }
        return value.substring(0, end);
    }

    private static void validateStaticPath(String path) throws HttpRequestException {
        if (path.contains("${") || path.contains("#{") || path.contains("*") || path.contains("?")) {
            throw failure(HttpRequestException.UNSUPPORTED_MAPPING);
        }
        for (int index = 0; index < path.length(); index++) {
            char character = path.charAt(index);
            if (Character.isWhitespace(character) || Character.isISOControl(character) || character == '#') {
                throw failure(HttpRequestException.UNSUPPORTED_MAPPING);
            }
        }
        Matcher matcher = PATH_VARIABLE_PATTERN.matcher(path);
        String remainder = matcher.replaceAll("");
        if (remainder.contains("{") || remainder.contains("}")) {
            throw failure(HttpRequestException.UNSUPPORTED_MAPPING);
        }
        matcher.reset();
        while (matcher.find()) {
            String name = matcher.group(1);
            if (!name.matches("[A-Za-z_$][A-Za-z0-9_$.\\-]*")) {
                throw failure(HttpRequestException.UNSUPPORTED_MAPPING);
            }
        }
    }

    private static List<String> pathVariableNames(String path) {
        List<String> names = new ArrayList<>();
        Matcher matcher = PATH_VARIABLE_PATTERN.matcher(path);
        while (matcher.find()) {
            names.add(matcher.group(1));
        }
        return names;
    }

    private static String normalizeMediaType(String mediaType) {
        return mediaType == null ? "" : mediaType.trim().toLowerCase(Locale.ROOT);
    }

    private static ParsedParameters parseParameters(PsiMethod method) throws HttpRequestException {
        List<Binding> bindings = new ArrayList<>();
        PsiParameter bodyParameter = null;
        for (PsiParameter parameter : method.getParameterList().getParameters()) {
            ProgressManager.checkCanceled();
            if (!parameter.isValid()) {
                throw failure(HttpRequestException.INVALID_SOURCE);
            }
            if (hasAnnotation(parameter, REQUEST_PART) || hasAnnotation(parameter, MODEL_ATTRIBUTE)
                    || hasAnnotation(parameter, COOKIE_VALUE)) {
                throw failure(HttpRequestException.UNSUPPORTED_PARAMETER);
            }
            if (hasAnnotation(parameter, REQUEST_BODY)) {
                if (bodyParameter != null) {
                    throw failure(HttpRequestException.UNSUPPORTED_PARAMETER);
                }
                if (hasAnnotation(parameter, REQUEST_PARAM) || hasAnnotation(parameter, PATH_VARIABLE)
                        || hasAnnotation(parameter, REQUEST_HEADER)) {
                    throw failure(HttpRequestException.UNSUPPORTED_PARAMETER);
                }
                bodyParameter = parameter;
                continue;
            }

            int bindingCount = (hasAnnotation(parameter, REQUEST_PARAM) ? 1 : 0)
                    + (hasAnnotation(parameter, PATH_VARIABLE) ? 1 : 0)
                    + (hasAnnotation(parameter, REQUEST_HEADER) ? 1 : 0);
            if (bindingCount != 1) {
                throw failure(HttpRequestException.UNSUPPORTED_PARAMETER);
            }

            PsiAnnotation bindingAnnotation;
            BindingKind kind;
            if ((bindingAnnotation = findAnnotation(parameter, PATH_VARIABLE)) != null) {
                kind = BindingKind.PATH;
            } else if ((bindingAnnotation = findAnnotation(parameter, REQUEST_PARAM)) != null) {
                kind = BindingKind.REQUEST_PARAM;
            } else {
                bindingAnnotation = findAnnotation(parameter, REQUEST_HEADER);
                kind = BindingKind.HEADER;
            }
            if (!isSupportedScalar(parameter.getType())) {
                throw failure(HttpRequestException.UNSUPPORTED_PARAMETER);
            }
            String name = bindingName(bindingAnnotation, parameter);
            String example = openApiExample(parameter);
            String defaultValue = effectiveDefaultValue(bindingAnnotation, kind);
            boolean required = booleanAttribute(bindingAnnotation, "required", true);
            if (defaultValue != null && kind != BindingKind.PATH) {
                required = false;
            }
            bindings.add(new Binding(kind, name, example, defaultValue, required));
        }

        if (bodyParameter != null && !isSupportedBodyType(bodyParameter.getType())) {
            throw failure(HttpRequestException.UNSUPPORTED_PARAMETER);
        }
        return new ParsedParameters(bindings, bodyParameter);
    }

    private static String bindingName(PsiAnnotation annotation, PsiParameter parameter) throws HttpRequestException {
        String name = declaredString(annotation, "name");
        String value = declaredString(annotation, "value");
        boolean hasName = !isBlank(name);
        boolean hasValue = !isBlank(value);
        if (hasName && hasValue && !name.equals(value)) {
            throw failure(HttpRequestException.UNSUPPORTED_PARAMETER);
        }
        String bindingName = hasName ? name : hasValue ? value : parameter.getName();
        if (isBlank(bindingName) || bindingName.indexOf('\r') >= 0 || bindingName.indexOf('\n') >= 0) {
            throw failure(HttpRequestException.UNSUPPORTED_PARAMETER);
        }
        return bindingName;
    }

    private static String openApiExample(PsiParameter parameter) {
        PsiAnnotation annotation = findAnnotation(parameter, OPEN_API_PARAMETER);
        String example = annotation == null ? null : stringAttribute(annotation, "example");
        return isBlank(example) ? null : example;
    }

    private static String effectiveDefaultValue(PsiAnnotation annotation, BindingKind kind) {
        if (kind == BindingKind.PATH) {
            return null;
        }
        String value = stringAttribute(annotation, "defaultValue");
        return value == null || SPRING_DEFAULT_VALUE_SENTINEL.equals(value) ? null : value;
    }

    private static boolean booleanAttribute(PsiAnnotation annotation, String name, boolean defaultValue)
            throws HttpRequestException {
        Object value = constantValue(annotation.findAttributeValue(name));
        if (value == null) {
            return defaultValue;
        }
        if (Boolean.TRUE.equals(value) || "true".equalsIgnoreCase(value.toString())) {
            return true;
        }
        if (Boolean.FALSE.equals(value) || "false".equalsIgnoreCase(value.toString())) {
            return false;
        }
        throw failure(HttpRequestException.UNSUPPORTED_PARAMETER);
    }

    private static boolean isSupportedScalar(PsiType type) {
        if (type == null || !type.isValid()) {
            return false;
        }
        String canonicalText = type.getCanonicalText();
        return SUPPORTED_SCALAR_TYPES.contains(canonicalText);
    }

    private static boolean isSupportedBodyType(PsiType type) {
        if (type == null || !type.isValid()) {
            return false;
        }
        String canonicalText = type.getCanonicalText();
        if ("void".equals(canonicalText)) {
            return false;
        }
        if (type instanceof PsiPrimitiveType || isKnownBodyScalar(type)) {
            return true;
        }
        if (InheritanceUtil.isInheritor(type, "java.util.Optional")
                || InheritanceUtil.isInheritor(type, CommonClassNames.JAVA_UTIL_MAP)
                || "java.io.InputStream".equals(canonicalText)
                || InheritanceUtil.isInheritor(type, "java.io.InputStream")
                || "org.springframework.core.io.InputStreamSource".equals(canonicalText)
                || InheritanceUtil.isInheritor(type, "org.springframework.core.io.InputStreamSource")
                || "org.springframework.web.multipart.MultipartFile".equals(canonicalText)
                || InheritanceUtil.isInheritor(type, "org.springframework.web.multipart.MultipartFile")
                || "java.io.File".equals(canonicalText)) {
            return false;
        }
        if (type instanceof PsiArrayType arrayType) {
            return isSupportedBodyType(arrayType.getComponentType());
        }
        if (InheritanceUtil.isInheritor(type, CommonClassNames.JAVA_UTIL_COLLECTION)) {
            if (!(type instanceof PsiClassType classType)) {
                return false;
            }
            PsiType[] arguments = classType.getParameters();
            return arguments.length == 1 && isSupportedBodyType(arguments[0]);
        }
        PsiClass resolved = PsiUtil.resolveClassInType(type);
        return resolved != null && !resolved.isAnnotationType() && !resolved.isEnum()
                && !resolved.isInterface() && !CommonClassNames.JAVA_LANG_OBJECT.equals(resolved.getQualifiedName())
                && (resolved.getQualifiedName() == null || !resolved.getQualifiedName().startsWith("java."));
    }

    private static boolean isKnownBodyScalar(PsiType type) {
        String canonicalText = type.getCanonicalText();
        return SUPPORTED_SCALAR_TYPES.contains(canonicalText)
                || Set.of("java.math.BigDecimal", "java.util.Date", "java.time.LocalDate",
                        "java.time.LocalTime", "java.time.LocalDateTime").contains(canonicalText);
    }

    private static RequestModel createRequest(Endpoint endpoint, ParsedParameters parameters)
            throws HttpRequestException {
        List<Binding> pathBindings = new ArrayList<>();
        List<Binding> requestBindings = new ArrayList<>();
        List<Binding> headerBindings = new ArrayList<>();
        Set<String> pathNames = new LinkedHashSet<>();
        Set<String> requestNames = new LinkedHashSet<>();
        Set<String> headerNames = new LinkedHashSet<>();
        for (Binding binding : parameters.bindings()) {
            ProgressManager.checkCanceled();
            switch (binding.kind()) {
                case PATH -> {
                    if (!pathNames.add(binding.name())) {
                        throw failure(HttpRequestException.PATH_PARAMETER_MISMATCH);
                    }
                    pathBindings.add(binding);
                }
                case REQUEST_PARAM -> {
                    if (!requestNames.add(binding.name())) {
                        throw failure(HttpRequestException.UNSUPPORTED_PARAMETER);
                    }
                    requestBindings.add(binding);
                }
                case HEADER -> {
                    String lowerName = binding.name().toLowerCase(Locale.ROOT);
                    if (!headerNames.add(lowerName)) {
                        if ("content-type".equals(lowerName)) {
                            throw failure(HttpRequestException.CONTENT_TYPE_CONFLICT);
                        }
                        throw failure(HttpRequestException.UNSUPPORTED_PARAMETER);
                    }
                    headerBindings.add(binding);
                }
            }
        }

        validatePathBindings(endpoint, pathBindings);
        String bodyContentType = resolveBodyContentType(endpoint, parameters, requestBindings);
        List<OutputParameter> query = new ArrayList<>();
        List<OutputParameter> form = new ArrayList<>();
        List<OutputHeader> headers = new ArrayList<>();
        VariableNames variableNames = new VariableNames();

        for (Binding binding : requestBindings) {
            boolean formBinding = "application/x-www-form-urlencoded".equals(bodyContentType);
            ResolvedValue value = resolveValue(binding, formBinding ? "form" : "query", variableNames, false);
            if (value == null) {
                continue;
            }
            OutputParameter output = new OutputParameter(binding.name(), value.value());
            (formBinding ? form : query).add(output);
        }

        for (Binding binding : headerBindings) {
            if (!SAFE_HEADER_NAME.matcher(binding.name()).matches()) {
                throw failure(HttpRequestException.UNSAFE_HEADER);
            }
            boolean contentTypeHeader = "content-type".equals(binding.name().toLowerCase(Locale.ROOT));
            ResolvedValue explicitValue = explicitHeaderValue(binding);
            if (explicitValue != null && containsLineBreak(explicitValue.value())) {
                throw failure(HttpRequestException.UNSAFE_HEADER);
            }
            if (contentTypeHeader && bodyContentType != null) {
                if (explicitValue != null && !bodyContentType.equalsIgnoreCase(explicitValue.value().trim())) {
                    throw failure(HttpRequestException.CONTENT_TYPE_CONFLICT);
                }
                headers.add(new OutputHeader("Content-Type", bodyContentType));
                continue;
            }

            ResolvedValue value = resolveHeaderValue(binding, variableNames);
            if (value == null) {
                continue;
            }
            if (containsLineBreak(binding.name()) || containsLineBreak(value.value())) {
                throw failure(HttpRequestException.UNSAFE_HEADER);
            }
            headers.add(new OutputHeader(binding.name(), value.value()));
        }
        if (bodyContentType != null && headerBindings.stream().noneMatch(
                binding -> "content-type".equals(binding.name().toLowerCase(Locale.ROOT)))) {
            headers.add(new OutputHeader("Content-Type", bodyContentType));
        }

        String path = substitutePath(endpoint.path(), pathBindings, variableNames);
        String formBody = "application/x-www-form-urlencoded".equals(bodyContentType)
                ? renderParameters(form, true) : null;
        String body = parameters.bodyParameter() == null ? formBody : bodyJson(parameters.bodyParameter());
        if (parameters.bodyParameter() != null && !form.isEmpty()) {
            throw failure(HttpRequestException.UNSUPPORTED_PARAMETER);
        }
        if (bodyContentType != null && body == null) {
            throw failure(HttpRequestException.UNSUPPORTED_MEDIA_TYPE);
        }
        return new RequestModel(endpoint.method(), path, query, headers, body);
    }

    private static void validatePathBindings(Endpoint endpoint, List<Binding> pathBindings)
            throws HttpRequestException {
        Set<String> bindingNames = new LinkedHashSet<>();
        for (Binding binding : pathBindings) {
            bindingNames.add(binding.name());
        }
        Set<String> placeholderNames = new LinkedHashSet<>(endpoint.pathVariables());
        if (!placeholderNames.equals(bindingNames)) {
            throw failure(HttpRequestException.PATH_PARAMETER_MISMATCH);
        }
    }

    @Nullable
    private static String resolveBodyContentType(Endpoint endpoint, ParsedParameters parameters,
                                                 List<Binding> requestBindings) throws HttpRequestException {
        boolean hasJsonBody = parameters.bodyParameter() != null;
        boolean formMediaType = "application/x-www-form-urlencoded".equals(endpoint.consumes());
        if (endpoint.consumes().isBlank()) {
            if (hasJsonBody) {
                return "application/json";
            }
            return null;
        }
        if ("application/json".equals(endpoint.consumes())) {
            if (!hasJsonBody) {
                throw failure(HttpRequestException.UNSUPPORTED_MEDIA_TYPE);
            }
            return "application/json";
        }
        if (formMediaType) {
            if (hasJsonBody || !"POST".equals(endpoint.method()) || requestBindings.isEmpty()) {
                throw failure(HttpRequestException.UNSUPPORTED_MEDIA_TYPE);
            }
            return "application/x-www-form-urlencoded";
        }
        throw failure(HttpRequestException.UNSUPPORTED_MEDIA_TYPE);
    }

    private static String bodyJson(PsiParameter bodyParameter) throws HttpRequestException {
        try {
            PsiType type = bodyParameter.getType();
            String json;
            if (type instanceof PsiArrayType) {
                // The existing examples expand arrays only when they are DTO fields. An empty array is
                // a valid, editable body when the same bounded helper has no top-level array entry point.
                json = "[]";
            } else if (isKnownBodyScalar(type)) {
                json = SpringPsiUtils.reqBodyJson(bodyParameter).strip();
            } else if (type instanceof PsiClassType classType
                    && (InheritanceUtil.isInheritor(type, CommonClassNames.JAVA_UTIL_COLLECTION)
                    || classType.getParameters().length > 0)) {
                json = ParamPsiUtils.getRespBodyJson(type).strip();
            } else {
                PsiClass resolved = PsiUtil.resolveClassInType(type);
                if (resolved != null && FieldTypeConstant.FIELD_TYPE.containsKey(resolved.getName())
                        && !isKnownBodyScalar(type)) {
                    json = ParamPsiUtils.getRespBodyJson(type).strip();
                } else {
                    json = SpringPsiUtils.reqBodyJson(bodyParameter).strip();
                }
            }
            JsonParser.parseString(json);
            return json;
        } catch (ProcessCanceledException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw failure(HttpRequestException.UNSUPPORTED_PARAMETER);
        }
    }

    @Nullable
    private static ResolvedValue explicitHeaderValue(Binding binding) {
        if (!isBlank(binding.example())) {
            return new ResolvedValue(binding.example());
        }
        if (binding.defaultValue() != null) {
            return new ResolvedValue(binding.defaultValue());
        }
        return null;
    }

    @Nullable
    private static ResolvedValue resolveHeaderValue(Binding binding, VariableNames variableNames)
            throws HttpRequestException {
        ResolvedValue explicit = explicitHeaderValue(binding);
        boolean secret = AUTHENTICATION_HEADERS.contains(binding.name().toLowerCase(Locale.ROOT));
        if (explicit != null && secret) {
            return new ResolvedValue(variableNames.next("header", binding.name()));
        }
        if (explicit != null) {
            return explicit;
        }
        if (binding.required()) {
            return new ResolvedValue(variableNames.next("header", binding.name()));
        }
        return null;
    }

    @Nullable
    private static ResolvedValue resolveValue(Binding binding, String location, VariableNames variableNames,
                                              boolean forcePresent) throws HttpRequestException {
        if (!isBlank(binding.example())) {
            return new ResolvedValue(binding.example());
        }
        if (binding.defaultValue() != null) {
            return new ResolvedValue(binding.defaultValue());
        }
        if (forcePresent || binding.required()) {
            return new ResolvedValue(variableNames.next(location, binding.name()));
        }
        return null;
    }

    private static String substitutePath(String path, List<Binding> pathBindings, VariableNames variableNames)
            throws HttpRequestException {
        Map<String, ResolvedValue> pathValues = new LinkedHashMap<>();
        for (Binding binding : pathBindings) {
            pathValues.put(binding.name(), resolveValue(binding, "path", variableNames, true));
        }
        Matcher matcher = PATH_VARIABLE_PATTERN.matcher(path);
        StringBuffer result = new StringBuffer();
        while (matcher.find()) {
            ResolvedValue value = pathValues.get(matcher.group(1));
            if (value == null) {
                throw failure(HttpRequestException.PATH_PARAMETER_MISMATCH);
            }
            matcher.appendReplacement(result, Matcher.quoteReplacement(encodeWithVariables(value.value(), Component.PATH)));
        }
        matcher.appendTail(result);
        String unresolvedPath = HTTP_CLIENT_VARIABLE_PATTERN.matcher(result).replaceAll("");
        if (unresolvedPath.indexOf("{") >= 0 || unresolvedPath.indexOf("}") >= 0) {
            throw failure(HttpRequestException.PATH_PARAMETER_MISMATCH);
        }
        return result.toString();
    }

    private static String render(RequestModel request) {
        StringBuilder text = new StringBuilder();
        text.append(request.method()).append(" {{host}}").append(request.path());
        if (!request.query().isEmpty()) {
            text.append('?').append(renderParameters(request.query(), false));
        }
        text.append('\n');
        for (OutputHeader header : request.headers()) {
            text.append(header.name()).append(": ").append(header.value()).append('\n');
        }
        if (request.body() != null) {
            text.append('\n').append(request.body());
        }
        return text.toString();
    }

    private static String renderParameters(List<OutputParameter> parameters, boolean form) {
        StringBuilder text = new StringBuilder();
        for (OutputParameter parameter : parameters) {
            if (!text.isEmpty()) {
                text.append('&');
            }
            Component component = form ? Component.FORM : Component.QUERY;
            text.append(encodeComponent(parameter.name(), component))
                    .append('=')
                    .append(encodeWithVariables(parameter.value(), component));
        }
        return text.toString();
    }

    private static String encodeWithVariables(String value, Component component) {
        Matcher matcher = HTTP_CLIENT_VARIABLE_PATTERN.matcher(value);
        StringBuilder encoded = new StringBuilder(value.length());
        int offset = 0;
        while (matcher.find()) {
            encoded.append(encodeComponent(value.substring(offset, matcher.start()), component));
            encoded.append(matcher.group());
            offset = matcher.end();
        }
        encoded.append(encodeComponent(value.substring(offset), component));
        return encoded.toString();
    }

    private static String encodeComponent(String value, Component component) {
        if (value.isEmpty()) {
            return "";
        }
        StringBuilder encoded = new StringBuilder(value.length());
        for (byte valueByte : value.getBytes(StandardCharsets.UTF_8)) {
            int unsigned = valueByte & 0xff;
            if (isUnreserved(unsigned)) {
                encoded.append((char) unsigned);
            } else if (component == Component.FORM && unsigned == ' ') {
                encoded.append('+');
            } else {
                encoded.append('%');
                encoded.append(Character.toUpperCase(Character.forDigit((unsigned >>> 4) & 0xf, 16)));
                encoded.append(Character.toUpperCase(Character.forDigit(unsigned & 0xf, 16)));
            }
        }
        return encoded.toString();
    }

    private static boolean isUnreserved(int value) {
        return value >= 'a' && value <= 'z' || value >= 'A' && value <= 'Z'
                || value >= '0' && value <= '9' || value == '-' || value == '.' || value == '_' || value == '~';
    }

    private static List<PsiAnnotation> findDirectMappingAnnotations(PsiModifierListOwner owner) {
        List<PsiAnnotation> result = new ArrayList<>();
        PsiModifierList modifierList = owner.getModifierList();
        if (modifierList == null) {
            return result;
        }
        for (PsiAnnotation annotation : modifierList.getAnnotations()) {
            if (MAPPING_ANNOTATIONS.contains(annotation.getQualifiedName())) {
                result.add(annotation);
            }
        }
        return result;
    }

    /**
     * Checks only annotation syntax. This deliberately avoids qualified-name resolution so action
     * update remains safe while indexes are unavailable; {@link #build(PsiMethod)} validates the
     * annotation's resolved framework identity before producing a request.
     */
    private static boolean hasDirectAnnotationSimpleName(PsiModifierListOwner owner, Set<String> simpleNames) {
        PsiModifierList modifierList = owner.getModifierList();
        if (modifierList == null) {
            return false;
        }
        for (PsiAnnotation annotation : modifierList.getAnnotations()) {
            PsiJavaCodeReferenceElement nameReference = annotation.getNameReferenceElement();
            if (nameReference == null) {
                continue;
            }
            String annotationName = nameReference.getText();
            int separator = annotationName.lastIndexOf('.');
            String simpleName = separator < 0 ? annotationName : annotationName.substring(separator + 1);
            if (simpleNames.contains(simpleName)) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasInheritedMapping(PsiMethod method, PsiClass owner) {
        for (PsiMethod superMethod : method.findSuperMethods()) {
            if (!findDirectMappingAnnotations(superMethod).isEmpty() || hasComposedMapping(superMethod)) {
                return true;
            }
        }
        Deque<PsiClass> pending = new ArrayDeque<>();
        Set<PsiClass> visited = new HashSet<>();
        for (PsiClass superClass : owner.getSupers()) {
            pending.add(superClass);
        }
        int count = 0;
        while (!pending.isEmpty() && count++ < MAX_ANCESTORS) {
            PsiClass ancestor = pending.removeFirst();
            if (!visited.add(ancestor)) {
                continue;
            }
            if (findAnnotation(ancestor, REQUEST_MAPPING) != null || hasComposedMapping(ancestor)) {
                return true;
            }
            for (PsiClass superClass : ancestor.getSupers()) {
                pending.addLast(superClass);
            }
        }
        return false;
    }

    private static boolean hasComposedMapping(PsiModifierListOwner owner) {
        PsiModifierList modifierList = owner.getModifierList();
        if (modifierList == null) {
            return false;
        }
        for (PsiAnnotation annotation : modifierList.getAnnotations()) {
            ProgressManager.checkCanceled();
            if (MAPPING_ANNOTATIONS.contains(annotation.getQualifiedName())) {
                continue;
            }
            PsiJavaCodeReferenceElement nameReference = annotation.getNameReferenceElement();
            PsiElement resolved = nameReference == null ? null : nameReference.resolve();
            if (resolved instanceof PsiClass annotationType && annotationType.isAnnotationType()
                    && hasMappingMetaAnnotation(annotationType, new HashSet<>(), 0)) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasMappingMetaAnnotation(PsiClass annotationType, Set<PsiClass> visited, int depth) {
        if (depth >= 8 || !visited.add(annotationType)) {
            return false;
        }
        PsiModifierList modifierList = annotationType.getModifierList();
        if (modifierList == null) {
            return false;
        }
        for (PsiAnnotation annotation : modifierList.getAnnotations()) {
            if (MAPPING_ANNOTATIONS.contains(annotation.getQualifiedName())) {
                return true;
            }
            PsiJavaCodeReferenceElement nameReference = annotation.getNameReferenceElement();
            PsiElement resolved = nameReference == null ? null : nameReference.resolve();
            if (resolved instanceof PsiClass nested && nested.isAnnotationType()
                    && hasMappingMetaAnnotation(nested, visited, depth + 1)) {
                return true;
            }
        }
        return false;
    }

    private static List<String> enumNames(PsiAnnotation annotation, String attributeName) throws HttpRequestException {
        PsiAnnotationMemberValue value = annotation.findAttributeValue(attributeName);
        if (value == null) {
            return List.of();
        }
        List<PsiAnnotationMemberValue> elements = value instanceof PsiArrayInitializerMemberValue array
                ? List.of(array.getInitializers()) : List.of(value);
        List<String> names = new ArrayList<>();
        for (PsiAnnotationMemberValue element : elements) {
            if (element instanceof PsiReferenceExpression reference) {
                String name = reference.getReferenceName();
                if (name != null) {
                    names.add(name);
                    continue;
                }
            }
            Object constant = constantValue(element);
            if (constant instanceof String string) {
                names.add(string);
                continue;
            }
            String expressionText = element.getText();
            int separator = expressionText.lastIndexOf('.');
            if (separator >= 0 && separator + 1 < expressionText.length()) {
                names.add(expressionText.substring(separator + 1));
            } else {
                throw failure(HttpRequestException.UNSUPPORTED_MAPPING);
            }
        }
        return names;
    }

    private static List<String> stringValues(PsiAnnotation annotation, String attributeName)
            throws HttpRequestException {
        PsiAnnotationMemberValue value = annotation.findAttributeValue(attributeName);
        if (value == null) {
            return List.of();
        }
        List<PsiAnnotationMemberValue> elements = value instanceof PsiArrayInitializerMemberValue array
                ? List.of(array.getInitializers()) : List.of(value);
        List<String> values = new ArrayList<>();
        for (PsiAnnotationMemberValue element : elements) {
            Object constant = constantValue(element);
            if (!(constant instanceof String string)) {
                throw failure(HttpRequestException.UNSUPPORTED_MAPPING);
            }
            values.add(string);
        }
        return values;
    }

    private static String stringAttribute(PsiAnnotation annotation, String attributeName) {
        PsiAnnotationMemberValue value = annotation.findAttributeValue(attributeName);
        Object constant = constantValue(value);
        return constant instanceof String string ? string : null;
    }

    private static String declaredString(PsiAnnotation annotation, String attributeName) {
        PsiAnnotationMemberValue value = annotation.findDeclaredAttributeValue(attributeName);
        Object constant = constantValue(value);
        return constant instanceof String string ? string : null;
    }

    @Nullable
    private static Object constantValue(@Nullable PsiAnnotationMemberValue value) {
        if (!(value instanceof PsiExpression expression)) {
            return null;
        }
        return JavaPsiFacade.getInstance(value.getProject()).getConstantEvaluationHelper()
                .computeConstantExpression(expression);
    }

    private static boolean hasAnnotation(PsiModifierListOwner owner, String qualifiedName) {
        return findAnnotation(owner, qualifiedName) != null;
    }

    @Nullable
    private static PsiAnnotation findAnnotation(PsiModifierListOwner owner, String qualifiedName) {
        PsiModifierList modifierList = owner.getModifierList();
        return modifierList == null ? null : modifierList.findAnnotation(qualifiedName);
    }

    private static List<String> nonBlankValues(List<String> values) {
        List<String> result = new ArrayList<>(values.size());
        for (String value : values) {
            if (!isBlank(value)) {
                result.add(value);
            }
        }
        return result;
    }

    private static boolean containsLineBreak(String value) {
        return value.indexOf('\r') >= 0 || value.indexOf('\n') >= 0;
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private static HttpRequestException failure(String reasonKey) {
        return new HttpRequestException(reasonKey);
    }

    private enum BindingKind { PATH, REQUEST_PARAM, HEADER }

    private enum Component { PATH, QUERY, FORM }

    private record MappingInfo(List<String> paths, List<String> methods, List<String> consumes) {
        private static MappingInfo empty() {
            return new MappingInfo(List.of(), List.of(), List.of());
        }
    }

    private record Endpoint(String method, String path, String consumes, List<String> pathVariables) { }

    private record Binding(BindingKind kind, String name, String example, String defaultValue, boolean required) { }

    private record ParsedParameters(List<Binding> bindings, PsiParameter bodyParameter) { }

    private record ResolvedValue(String value) { }

    private record OutputParameter(String name, String value) { }

    private record OutputHeader(String name, String value) { }

    private record RequestModel(String method, String path, List<OutputParameter> query,
                                List<OutputHeader> headers, String body) { }

    private static final class VariableNames {
        private final Map<String, Integer> occurrences = new LinkedHashMap<>();
        private final Set<String> used = new HashSet<>();

        private String next(String location, String sourceName) {
            String normalized = sourceName.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_]+", "_");
            normalized = normalized.replaceAll("^_+|_+$", "");
            if (normalized.isEmpty()) {
                normalized = "value";
            }
            String stem = location + "_" + normalized;
            int occurrence = occurrences.getOrDefault(stem, 0);
            String candidate;
            do {
                occurrence++;
                candidate = "{{" + stem + (occurrence == 1 ? "" : "_" + occurrence) + "}}";
            } while (!used.add(candidate));
            occurrences.put(stem, occurrence);
            return candidate;
        }
    }
}
