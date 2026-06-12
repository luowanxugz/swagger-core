package io.swagger.v3.jaxrs2.util;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class JaxRsAnnotationLoader {

    private static final String[] NAMESPACES = {"javax.ws.rs", "jakarta.ws.rs"};

    private static final Map<String, Class<? extends Annotation>> ANNOTATION_CACHE = new ConcurrentHashMap<>();
    private static final Map<String, Boolean> NAMESPACE_AVAILABLE = new ConcurrentHashMap<>();

    private static final String GET = "GET";
    private static final String POST = "POST";
    private static final String PUT = "PUT";
    private static final String DELETE = "DELETE";
    private static final String HEAD = "HEAD";
    private static final String OPTIONS = "OPTIONS";
    private static final String PATCH = "PATCH";
    private static final String PATH = "Path";
    private static final String CONSUMES = "Consumes";
    private static final String PRODUCES = "Produces";
    private static final String APPLICATION_PATH = "ApplicationPath";
    private static final String HTTP_METHOD = "HttpMethod";
    private static final String BEAN_PARAM = "BeanParam";
    private static final String COOKIE_PARAM = "CookieParam";
    private static final String HEADER_PARAM = "HeaderParam";
    private static final String PATH_PARAM = "PathParam";
    private static final String QUERY_PARAM = "QueryParam";
    private static final String MATRIX_PARAM = "MatrixParam";
    private static final String CONTEXT = "Context";

    private static final String GET_METHOD = "get";
    private static final String POST_METHOD = "post";
    private static final String PUT_METHOD = "put";
    private static final String DELETE_METHOD = "delete";
    private static final String HEAD_METHOD = "head";
    private static final String OPTIONS_METHOD = "options";
    private static final String PATCH_METHOD = "patch";

    public static String getOperationMethod(Method method) {
        if (isAnnotationPresent(method, GET)) {
            return GET_METHOD;
        } else if (isAnnotationPresent(method, POST)) {
            return POST_METHOD;
        } else if (isAnnotationPresent(method, PUT)) {
            return PUT_METHOD;
        } else if (isAnnotationPresent(method, DELETE)) {
            return DELETE_METHOD;
        } else if (isAnnotationPresent(method, OPTIONS)) {
            return OPTIONS_METHOD;
        } else if (isAnnotationPresent(method, HEAD)) {
            return HEAD_METHOD;
        } else if (isAnnotationPresent(method, PATCH)) {
            return PATCH_METHOD;
        }
        return null;
    }

    public static boolean isAnnotationPresent(AnnotatedElement element, String annotationSimpleName) {
        for (String namespace : NAMESPACES) {
            String fullName = namespace + "." + annotationSimpleName;
            Class<? extends Annotation> annotationClass = loadAnnotationClass(fullName);
            if (annotationClass != null && element.isAnnotationPresent(annotationClass)) {
                return true;
            }
        }
        return false;
    }

    @SuppressWarnings("unchecked")
    public static <A extends Annotation> A getAnnotation(AnnotatedElement element, String annotationSimpleName) {
        for (String namespace : NAMESPACES) {
            String fullName = namespace + "." + annotationSimpleName;
            Class<? extends Annotation> annotationClass = loadAnnotationClass(fullName);
            if (annotationClass != null) {
                A annotation = (A) element.getAnnotation(annotationClass);
                if (annotation != null) {
                    return annotation;
                }
            }
        }
        return null;
    }

    public static Class<? extends Annotation> loadAnnotationClass(String fullClassName) {
        Class<? extends Annotation> cached = ANNOTATION_CACHE.get(fullClassName);
        if (cached != null) {
            return cached;
        }
        String namespace = fullClassName.contains(".") ? fullClassName.substring(0, fullClassName.lastIndexOf('.')) : "";
        Boolean available = NAMESPACE_AVAILABLE.get(namespace);
        if (available != null && !available) {
            return null;
        }
        try {
            Class<?> clazz = Class.forName(fullClassName);
            if (clazz.isAnnotation()) {
                @SuppressWarnings("unchecked")
                Class<? extends Annotation> annotationClass = (Class<? extends Annotation>) clazz;
                ANNOTATION_CACHE.put(fullClassName, annotationClass);
                NAMESPACE_AVAILABLE.put(namespace, true);
                return annotationClass;
            }
        } catch (ClassNotFoundException | NoClassDefFoundError e) {
            NAMESPACE_AVAILABLE.put(namespace, false);
        }
        return null;
    }

    public static Class<? extends Annotation> getPathClass() {
        return loadAnnotationClass("javax.ws.rs.Path") != null
                ? loadAnnotationClass("javax.ws.rs.Path")
                : loadAnnotationClass("jakarta.ws.rs.Path");
    }

    public static Class<? extends Annotation> getConsumesClass() {
        return loadAnnotationClass("javax.ws.rs.Consumes") != null
                ? loadAnnotationClass("javax.ws.rs.Consumes")
                : loadAnnotationClass("jakarta.ws.rs.Consumes");
    }

    public static Class<? extends Annotation> getProducesClass() {
        return loadAnnotationClass("javax.ws.rs.Produces") != null
                ? loadAnnotationClass("javax.ws.rs.Produces")
                : loadAnnotationClass("jakarta.ws.rs.Produces");
    }

    public static Class<? extends Annotation> getApplicationPathClass() {
        return loadAnnotationClass("javax.ws.rs.ApplicationPath") != null
                ? loadAnnotationClass("javax.ws.rs.ApplicationPath")
                : loadAnnotationClass("jakarta.ws.rs.ApplicationPath");
    }

    public static Class<? extends Annotation> getHttpMethodClass() {
        return loadAnnotationClass("javax.ws.rs.HttpMethod") != null
                ? loadAnnotationClass("javax.ws.rs.HttpMethod")
                : loadAnnotationClass("jakarta.ws.rs.HttpMethod");
    }
}