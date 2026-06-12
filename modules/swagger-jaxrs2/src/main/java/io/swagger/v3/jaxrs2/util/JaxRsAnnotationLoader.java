package io.swagger.v3.jaxrs2.util;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;

public class JaxRsAnnotationLoader {

    public static boolean isAnnotationPresent(AnnotatedElement element, String annotationClassName) {
        return getAnnotation(element, annotationClassName) != null;
    }

    public static Annotation getAnnotation(AnnotatedElement element, String annotationClassName) {
        try {
            Class<? extends Annotation> annotationClass = (Class<? extends Annotation>) Class.forName(annotationClassName);
            return element.getAnnotation(annotationClass);
        } catch (ClassNotFoundException | ClassCastException e) {
            return null;
        }
    }

    public static <T> T getAnnotationValue(Annotation annotation, String propertyName, Class<T> returnType) {
        if (annotation == null) {
            return null;
        }
        try {
            Method method = annotation.getClass().getMethod(propertyName);
            Object value = method.invoke(annotation);
            if (returnType.isInstance(value)) {
                return returnType.cast(value);
            }
        } catch (Exception e) {
            // Ignore
        }
        return null;
    }

    public static String getOperationMethod(Method method) {
        if (isAnnotationPresent(method, "javax.ws.rs.GET") || isAnnotationPresent(method, "jakarta.ws.rs.GET")) {
            return "get";
        } else if (isAnnotationPresent(method, "javax.ws.rs.PUT") || isAnnotationPresent(method, "jakarta.ws.rs.PUT")) {
            return "put";
        } else if (isAnnotationPresent(method, "javax.ws.rs.POST") || isAnnotationPresent(method, "jakarta.ws.rs.POST")) {
            return "post";
        } else if (isAnnotationPresent(method, "javax.ws.rs.DELETE") || isAnnotationPresent(method, "jakarta.ws.rs.DELETE")) {
            return "delete";
        } else if (isAnnotationPresent(method, "javax.ws.rs.OPTIONS") || isAnnotationPresent(method, "jakarta.ws.rs.OPTIONS")) {
            return "options";
        } else if (isAnnotationPresent(method, "javax.ws.rs.HEAD") || isAnnotationPresent(method, "jakarta.ws.rs.HEAD")) {
            return "head";
        } else if (isAnnotationPresent(method, "javax.ws.rs.PATCH") || isAnnotationPresent(method, "jakarta.ws.rs.PATCH")) {
            return "patch";
        }

        Annotation httpMethod = getAnnotation(method, "javax.ws.rs.HttpMethod");
        if (httpMethod == null) {
            httpMethod = getAnnotation(method, "jakarta.ws.rs.HttpMethod");
        }
        if (httpMethod != null) {
            String value = getAnnotationValue(httpMethod, "value", String.class);
            if (value != null) {
                return value.toLowerCase();
            }
        }

        return null;
    }

    public static Annotation getPathAnnotation(AnnotatedElement element) {
        Annotation path = getAnnotation(element, "javax.ws.rs.Path");
        if (path == null) {
            path = getAnnotation(element, "jakarta.ws.rs.Path");
        }
        return path;
    }

    public static Annotation getConsumesAnnotation(AnnotatedElement element) {
        Annotation consumes = getAnnotation(element, "javax.ws.rs.Consumes");
        if (consumes == null) {
            consumes = getAnnotation(element, "jakarta.ws.rs.Consumes");
        }
        return consumes;
    }

    public static Annotation getProducesAnnotation(AnnotatedElement element) {
        Annotation produces = getAnnotation(element, "javax.ws.rs.Produces");
        if (produces == null) {
            produces = getAnnotation(element, "jakarta.ws.rs.Produces");
        }
        return produces;
    }
}
