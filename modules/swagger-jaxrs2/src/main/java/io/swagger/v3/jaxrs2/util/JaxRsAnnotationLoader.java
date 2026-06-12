package io.swagger.v3.jaxrs2.util;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class JaxRsAnnotationLoader {

    private static final String JAVAX_PREFIX = "javax" + ".ws.rs";
    private static final String JAKARTA_PREFIX = "jakarta" + ".ws.rs";

    private static final Map<String, Class<?>> classCache = new ConcurrentHashMap<>();

    private static final List<String> HTTP_METHOD_ANNOTATIONS = Arrays.asList(
            "GET", "PUT", "POST", "DELETE", "OPTIONS", "HEAD", "PATCH"
    );

    private static final List<String[]> HTTP_METHOD_MAPPINGS = Arrays.asList(
            new String[]{"GET", "get"},
            new String[]{"PUT", "put"},
            new String[]{"POST", "post"},
            new String[]{"DELETE", "delete"},
            new String[]{"OPTIONS", "options"},
            new String[]{"HEAD", "head"},
            new String[]{"PATCH", "patch"}
    );

    private JaxRsAnnotationLoader() {
    }

    public static Class<?> resolveClass(String simpleName) {
        String javaxName = JAVAX_PREFIX + "." + simpleName;
        String jakartaName = JAKARTA_PREFIX + "." + simpleName;
        Class<?> cls = loadClass(javaxName);
        if (cls != null) {
            return cls;
        }
        return loadClass(jakartaName);
    }

    public static String resolveClassName(String simpleName) {
        String javaxName = JAVAX_PREFIX + "." + simpleName;
        if (loadClass(javaxName) != null) {
            return javaxName;
        }
        String jakartaName = JAKARTA_PREFIX + "." + simpleName;
        if (loadClass(jakartaName) != null) {
            return jakartaName;
        }
        return null;
    }

    public static List<String> resolveClassNames(String simpleName) {
        return Arrays.asList(JAVAX_PREFIX + "." + simpleName, JAKARTA_PREFIX + "." + simpleName);
    }

    private static Class<?> loadClass(String className) {
        return classCache.computeIfAbsent(className, key -> {
            try {
                return Class.forName(key);
            } catch (ClassNotFoundException e) {
                return null;
            }
        });
    }

    @SuppressWarnings("unchecked")
    private static Class<? extends Annotation> loadAnnotationClass(String simpleName) {
        Class<?> cls = resolveClass(simpleName);
        if (cls != null && cls.isAnnotation()) {
            return (Class<? extends Annotation>) cls;
        }
        return null;
    }

    public static boolean isAnnotationPresent(AnnotatedElement element, String simpleName) {
        Class<? extends Annotation> annotationClass = loadAnnotationClass(simpleName);
        if (annotationClass != null) {
            return element.isAnnotationPresent(annotationClass);
        }
        return false;
    }

    public static Annotation getAnnotation(AnnotatedElement element, String simpleName) {
        Class<? extends Annotation> javaxClass = loadClassForNamespace(JAVAX_PREFIX + "." + simpleName);
        if (javaxClass != null) {
            Annotation ann = element.getAnnotation(javaxClass);
            if (ann != null) {
                return ann;
            }
        }
        Class<? extends Annotation> jakartaClass = loadClassForNamespace(JAKARTA_PREFIX + "." + simpleName);
        if (jakartaClass != null) {
            return element.getAnnotation(jakartaClass);
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private static Class<? extends Annotation> loadClassForNamespace(String className) {
        Class<?> cls = loadClass(className);
        if (cls != null && cls.isAnnotation()) {
            return (Class<? extends Annotation>) cls;
        }
        return null;
    }

    public static String getAnnotationValue(Annotation annotation, String methodName) {
        if (annotation == null) {
            return null;
        }
        try {
            Method method = annotation.annotationType().getMethod(methodName);
            Object value = method.invoke(annotation);
            if (value instanceof String) {
                return (String) value;
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    public static String[] getAnnotationValues(Annotation annotation, String methodName) {
        if (annotation == null) {
            return null;
        }
        try {
            Method method = annotation.annotationType().getMethod(methodName);
            Object value = method.invoke(annotation);
            if (value instanceof String[]) {
                return (String[]) value;
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    public static String getOperationMethod(Method method) {
        for (String[] mapping : HTTP_METHOD_MAPPINGS) {
            if (isAnnotationPresent(method, mapping[0])) {
                return mapping[1];
            }
        }
        Annotation httpMethod = getAnnotation(method, "HttpMethod");
        if (httpMethod != null) {
            String value = getAnnotationValue(httpMethod, "value");
            if (value != null) {
                return value.toLowerCase();
            }
        }
        return null;
    }

    public static String getHttpMethodFromCustomAnnotations(Method method) {
        for (Annotation methodAnnotation : method.getAnnotations()) {
            Annotation httpMethod = methodAnnotation.annotationType().getAnnotation(
                    loadAnnotationClass("HttpMethod"));
            if (httpMethod != null) {
                String value = getAnnotationValue(httpMethod, "value");
                if (value != null) {
                    return value.toLowerCase();
                }
            }
        }
        return null;
    }

    public static boolean isContext(List<Annotation> annotations) {
        for (Annotation annotation : annotations) {
            if (isAnnotationPresent(annotation.annotationType(), "Context")
                    || annotation.annotationType().getName().equals(JAVAX_PREFIX + ".core.Context")
                    || annotation.annotationType().getName().equals(JAKARTA_PREFIX + ".core.Context")) {
                return true;
            }
        }
        return false;
    }

    public static boolean isAnnotationInstanceOf(Annotation annotation, String simpleName) {
        if (annotation == null) {
            return false;
        }
        String annotationClassName = annotation.annotationType().getName();
        return annotationClassName.equals(JAVAX_PREFIX + "." + simpleName)
                || annotationClassName.equals(JAKARTA_PREFIX + "." + simpleName);
    }

    public static boolean shouldIgnoreClass(String className) {
        return className.startsWith(JAVAX_PREFIX + ".") || className.startsWith(JAKARTA_PREFIX + ".");
    }

    public static boolean isApplicationClass(Class<?> cls) {
        Class<?> appClass = loadClass(JAVAX_PREFIX + ".core.Application");
        if (appClass == null) {
            appClass = loadClass(JAKARTA_PREFIX + ".core.Application");
        }
        return appClass != null && appClass.isAssignableFrom(cls);
    }

    public static String getMediaTypeValue(String constantName) {
        String value = resolveConstant(JAVAX_PREFIX + ".core.MediaType", constantName);
        if (value != null) {
            return value;
        }
        return resolveConstant(JAKARTA_PREFIX + ".core.MediaType", constantName);
    }

    public static String getMediaTypeApplicationFormUrlencoded() {
        String value = resolveConstant(JAVAX_PREFIX + ".core.MediaType", "APPLICATION_FORM_URLENCODED");
        if (value != null) {
            return value;
        }
        return resolveConstant(JAKARTA_PREFIX + ".core.MediaType", "APPLICATION_FORM_URLENCODED");
    }

    public static String getMediaTypeMultipartFormData() {
        String value = resolveConstant(JAVAX_PREFIX + ".core.MediaType", "MULTIPART_FORM_DATA");
        if (value != null) {
            return value;
        }
        return resolveConstant(JAKARTA_PREFIX + ".core.MediaType", "MULTIPART_FORM_DATA");
    }

    private static String resolveConstant(String className, String fieldName) {
        try {
            Class<?> cls = Class.forName(className);
            java.lang.reflect.Field field = cls.getField(fieldName);
            return (String) field.get(null);
        } catch (Exception e) {
            return null;
        }
    }
}
