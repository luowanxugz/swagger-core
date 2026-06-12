package io.swagger.v3.jaxrs2.util;

import com.fasterxml.jackson.annotation.JsonView;
import io.swagger.v3.core.util.ParameterProcessor;
import io.swagger.v3.core.util.ReflectionUtils;
import io.swagger.v3.jaxrs2.ext.OpenAPIExtension;
import io.swagger.v3.jaxrs2.ext.OpenAPIExtensions;
import io.swagger.v3.oas.integration.api.OpenAPIConfiguration;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.parameters.Parameter;
import org.apache.commons.lang3.StringUtils;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;

public class ReaderUtils {
    private static final String GET_METHOD = "get";
    private static final String POST_METHOD = "post";
    private static final String PUT_METHOD = "put";
    private static final String DELETE_METHOD = "delete";
    private static final String HEAD_METHOD = "head";
    private static final String OPTIONS_METHOD = "options";
    private static final String PATH_DELIMITER = "/";
    private static final String JAVAX_CONTEXT = "javax.ws.rs.core.Context";
    private static final String JAKARTA_CONTEXT = "jakarta.ws.rs.core.Context";

    public static List<Parameter> collectConstructorParameters(Class<?> cls, Components components, javax.ws.rs.Consumes classConsumes, JsonView jsonViewAnnotation) {
        return collectConstructorParameters(cls, components, classConsumes, jsonViewAnnotation, null);
    }

    public static List<Parameter> collectConstructorParameters(Class<?> cls, Components components, javax.ws.rs.Consumes classConsumes, JsonView jsonViewAnnotation, Schema.SchemaResolution schemaResolution) {
        return collectConstructorParameters(cls, components, classConsumes, jsonViewAnnotation, schemaResolution, false);
    }

    public static List<Parameter> collectConstructorParameters(Class<?> cls, Components components, javax.ws.rs.Consumes classConsumes, JsonView jsonViewAnnotation, Schema.SchemaResolution schemaResolution, boolean openapi31) {
        if (cls.isLocalClass() || (cls.isMemberClass() && !Modifier.isStatic(cls.getModifiers()))) {
            return Collections.emptyList();
        }

        List<Parameter> selected = Collections.emptyList();
        int maxParamsCount = 0;

        for (Constructor<?> constructor : cls.getDeclaredConstructors()) {
            if (!ReflectionUtils.isConstructorCompatible(constructor)
                    && !ReflectionUtils.isInject(Arrays.asList(constructor.getDeclaredAnnotations()))) {
                continue;
            }

            final Type[] genericParameterTypes = constructor.getGenericParameterTypes();
            final Annotation[][] annotations = constructor.getParameterAnnotations();

            int paramsCount = 0;
            final List<Parameter> parameters = new ArrayList<>();
            for (int i = 0; i < genericParameterTypes.length; i++) {
                final List<Annotation> tmpAnnotations = Arrays.asList(annotations[i]);
                if (isContext(tmpAnnotations)) {
                    paramsCount++;
                } else {
                    final Type genericParameterType = genericParameterTypes[i];
                    final List<Parameter> tmpParameters = collectParameters(genericParameterType, tmpAnnotations, components, classConsumes, jsonViewAnnotation);
                    if (!tmpParameters.isEmpty()) {
                        for (Parameter tmpParameter : tmpParameters) {
                            Parameter processedParameter = ParameterProcessor.applyAnnotations(
                                    tmpParameter,
                                    genericParameterType,
                                    tmpAnnotations,
                                    components,
                                    classConsumes == null ? new String[0] : classConsumes.value(),
                                    null,
                                    jsonViewAnnotation,
                                    openapi31,
                                    schemaResolution);
                            if (processedParameter != null) {
                                parameters.add(processedParameter);
                            }
                        }
                        paramsCount++;
                    }
                }
            }

            if (paramsCount >= maxParamsCount) {
                maxParamsCount = paramsCount;
                selected = parameters;
            }
        }

        return selected;
    }

    public static List<Parameter> collectFieldParameters(Class<?> cls, Components components, javax.ws.rs.Consumes classConsumes, JsonView jsonViewAnnotation) {
        final List<Parameter> parameters = new ArrayList<>();
        for (Field field : ReflectionUtils.getDeclaredFields(cls)) {
            final List<Annotation> annotations = Arrays.asList(field.getAnnotations());
            final Type genericType = field.getGenericType();
            parameters.addAll(collectParameters(genericType, annotations, components, classConsumes, jsonViewAnnotation));
        }
        return parameters;
    }

    private static List<Parameter> collectParameters(Type type, List<Annotation> annotations, Components components, javax.ws.rs.Consumes classConsumes, JsonView jsonViewAnnotation) {
        final Iterator<OpenAPIExtension> chain = OpenAPIExtensions.chain();
        return chain.hasNext() ? chain.next().extractParameters(annotations, type, new HashSet<>(), components, classConsumes, null, false, jsonViewAnnotation, chain).parameters :
                Collections.emptyList();
    }

    private static boolean isContext(List<Annotation> annotations) {
        for (Annotation annotation : annotations) {
            String annotationName = annotation.annotationType().getName();
            if (JAVAX_CONTEXT.equals(annotationName) || JAKARTA_CONTEXT.equals(annotationName)) {
                return true;
            }
        }
        return false;
    }

    public static Optional<List<String>> getStringListFromStringArray(String[] array) {
        if (array == null) {
            return Optional.empty();
        }
        List<String> list = new ArrayList<>();
        boolean isEmpty = true;
        for (String value : array) {
            if (StringUtils.isNotBlank(value)) {
                isEmpty = false;
            }
            list.add(value);
        }
        if (isEmpty) {
            return Optional.empty();
        }
        return Optional.of(list);
    }

    public static boolean isIgnored(String path, OpenAPIConfiguration config) {
        if (config.getIgnoredRoutes() == null) {
            return false;
        }
        for (String item : config.getIgnoredRoutes()) {
            final int length = item.length();
            if (path.startsWith(item) && (path.length() == length || path.startsWith(PATH_DELIMITER, length))) {
                return true;
            }
        }
        return false;
    }

    public static String getPath(String classLevelPath, String methodLevelPath, String parentPath, boolean isSubresource) {
        if (classLevelPath == null && methodLevelPath == null && StringUtils.isEmpty(parentPath)) {
            return null;
        }
        StringBuilder b = new StringBuilder();
        appendPathComponent(parentPath, b);
        if (classLevelPath != null && !isSubresource) {
            appendPathComponent(classLevelPath, b);
        }
        if (methodLevelPath != null) {
            appendPathComponent(methodLevelPath, b);
        }
        return b.length() == 0 ? "/" : b.toString();
    }

    private static void appendPathComponent(String component, StringBuilder to) {
        if (component == null || component.isEmpty() || "/".equals(component)) {
            return;
        }
        if (!component.startsWith("/") && (to.length() == 0 || '/' != to.charAt(to.length() - 1))) {
            to.append("/");
        }
        if (component.endsWith("/")) {
            to.append(component, 0, component.length() - 1);
        } else {
            to.append(component);
        }
    }

    public static String extractOperationMethod(Method method, Iterator<OpenAPIExtension> chain) {
        String httpMethod = JaxRsAnnotationLoader.getOperationMethod(method);
        if (GET_METHOD.equals(httpMethod)) {
            return GET_METHOD;
        } else if (PUT_METHOD.equals(httpMethod)) {
            return PUT_METHOD;
        } else if (POST_METHOD.equals(httpMethod)) {
            return POST_METHOD;
        } else if (DELETE_METHOD.equals(httpMethod)) {
            return DELETE_METHOD;
        } else if (OPTIONS_METHOD.equals(httpMethod)) {
            return OPTIONS_METHOD;
        } else if (HEAD_METHOD.equals(httpMethod)) {
            return HEAD_METHOD;
        } else if (!StringUtils.isEmpty(httpMethod)) {
            return httpMethod;
        } else if (chain != null && chain.hasNext()) {
            return chain.next().extractOperationMethod(method, chain);
        } else {
            return null;
        }
    }

    public static String getHttpMethodFromCustomAnnotations(Method method) {
        return JaxRsAnnotationLoader.getHttpMethodFromCustomAnnotations(method);
    }
}
