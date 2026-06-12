package io.swagger.v3.jaxrs2.util;

import io.swagger.v3.core.util.ReflectionUtils;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public final class JaxRsAnnotationLoader {
    private static final String JAVAX_WS_RS_PREFIX = "javax.ws.rs.";
    private static final String JAKARTA_WS_RS_PREFIX = "jakarta.ws.rs.";
    private static final List<String> PATH_ANNOTATION_NAMES = Arrays.asList(
            JAVAX_WS_RS_PREFIX + "Path",
            JAKARTA_WS_RS_PREFIX + "Path"
    );
    private static final List<String> APPLICATION_PATH_ANNOTATION_NAMES = Arrays.asList(
            JAVAX_WS_RS_PREFIX + "ApplicationPath",
            JAKARTA_WS_RS_PREFIX + "ApplicationPath"
    );
    private static final List<String> HTTP_METHOD_ANNOTATION_NAMES = Arrays.asList(
            JAVAX_WS_RS_PREFIX + "HttpMethod",
            JAKARTA_WS_RS_PREFIX + "HttpMethod"
    );
    private static final List<JaxRsHttpMethod> HTTP_METHODS = Arrays.asList(
            new JaxRsHttpMethod("GET", "get"),
            new JaxRsHttpMethod("PUT", "put"),
            new JaxRsHttpMethod("POST", "post"),
            new JaxRsHttpMethod("DELETE", "delete"),
            new JaxRsHttpMethod("OPTIONS", "options"),
            new JaxRsHttpMethod("HEAD", "head"),
            new JaxRsHttpMethod("PATCH", "patch")
    );

    private JaxRsAnnotationLoader() {
    }

    public static String getOperationMethod(Method method) {
        for (JaxRsHttpMethod httpMethod : HTTP_METHODS) {
            if (isAnyAnnotationPresent(method, httpMethod.getAnnotationClassNames())) {
                return httpMethod.value;
            }
        }

        String customHttpMethod = getMethodAnnotationValue(method, HTTP_METHOD_ANNOTATION_NAMES, true);
        if (customHttpMethod != null) {
            return customHttpMethod.toLowerCase(Locale.ROOT);
        }
        return null;
    }

    public static String getHttpMethodFromCustomAnnotations(Method method) {
        String customHttpMethod = getMethodAnnotationValue(method, HTTP_METHOD_ANNOTATION_NAMES, false);
        if (customHttpMethod != null) {
            return customHttpMethod.toLowerCase(Locale.ROOT);
        }
        return null;
    }

    public static String getPathValue(Method method) {
        return getMethodAnnotationValue(method, PATH_ANNOTATION_NAMES, true);
    }

    public static String getPathValue(Class<?> cls) {
        return getClassAnnotationValue(cls, PATH_ANNOTATION_NAMES);
    }

    public static boolean hasPathAnnotation(Method method) {
        return getPathValue(method) != null;
    }

    public static String getApplicationPathValue(Class<?> cls) {
        return getClassAnnotationValue(cls, APPLICATION_PATH_ANNOTATION_NAMES);
    }

    public static Set<String> getPathAnnotationNames() {
        return new LinkedHashSet<>(PATH_ANNOTATION_NAMES);
    }

    public static Set<String> getApplicationPathAnnotationNames() {
        return new LinkedHashSet<>(APPLICATION_PATH_ANNOTATION_NAMES);
    }

    public static boolean isAnnotationPresent(AnnotatedElement element, String annotationClassName) {
        return findDirectOrMetaAnnotation(element.getAnnotations(), Collections.singleton(annotationClassName)) != null;
    }

    private static boolean isAnyAnnotationPresent(AnnotatedElement element, Collection<String> annotationClassNames) {
        return findDirectOrMetaAnnotation(element.getAnnotations(), annotationClassNames) != null;
    }

    private static String getMethodAnnotationValue(Method method, Collection<String> annotationClassNames, boolean includeOverrides) {
        Annotation annotation = findDirectOrMetaAnnotation(method.getAnnotations(), annotationClassNames);
        if (annotation != null) {
            return getAnnotationValue(annotation);
        }
        if (includeOverrides) {
            Method overriddenMethod = ReflectionUtils.getOverriddenMethod(method);
            if (overriddenMethod != null) {
                return getMethodAnnotationValue(overriddenMethod, annotationClassNames, true);
            }
        }
        return null;
    }

    private static String getClassAnnotationValue(Class<?> cls, Collection<String> annotationClassNames) {
        Annotation annotation = findDirectOrMetaAnnotation(cls.getAnnotations(), annotationClassNames);
        if (annotation != null) {
            return getAnnotationValue(annotation);
        }

        Class<?> superClass = cls.getSuperclass();
        if (superClass != null && !Object.class.equals(superClass)) {
            String superClassValue = getClassAnnotationValue(superClass, annotationClassNames);
            if (superClassValue != null) {
                return superClassValue;
            }
        }

        for (Class<?> implementedInterface : cls.getInterfaces()) {
            String interfaceValue = getClassAnnotationValue(implementedInterface, annotationClassNames);
            if (interfaceValue != null) {
                return interfaceValue;
            }
        }

        return null;
    }

    private static Annotation findDirectOrMetaAnnotation(Annotation[] annotations, Collection<String> annotationClassNames) {
        for (Annotation annotation : annotations) {
            if (annotationClassNames.contains(annotation.annotationType().getName())) {
                return annotation;
            }
        }
        for (Annotation annotation : annotations) {
            for (Annotation metaAnnotation : annotation.annotationType().getAnnotations()) {
                if (annotationClassNames.contains(metaAnnotation.annotationType().getName())) {
                    return metaAnnotation;
                }
            }
        }
        return null;
    }

    private static String getAnnotationValue(Annotation annotation) {
        try {
            Object value = annotation.annotationType().getMethod("value").invoke(annotation);
            return value == null ? null : value.toString();
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            return null;
        }
    }

    private static final class JaxRsHttpMethod {
        private final String annotationName;
        private final String value;

        private JaxRsHttpMethod(String annotationName, String value) {
            this.annotationName = annotationName;
            this.value = value;
        }

        private List<String> getAnnotationClassNames() {
            return Arrays.asList(
                    JAVAX_WS_RS_PREFIX + annotationName,
                    JAKARTA_WS_RS_PREFIX + annotationName
            );
        }
    }
}
