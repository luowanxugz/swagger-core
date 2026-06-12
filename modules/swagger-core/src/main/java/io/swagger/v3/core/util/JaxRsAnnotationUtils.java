package io.swagger.v3.core.util;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class JaxRsAnnotationUtils {

    private static final String JAVAX_PREFIX = "javax.ws.rs";
    private static final String JAKARTA_PREFIX = "jakarta.ws.rs";
    private static final String VALUE_METHOD = "value";

    private static final Map<String, String> JAVAX_TO_JAKARTA = new HashMap<>();
    private static final Map<String, String> JAKARTA_TO_JAVAX = new HashMap<>();

    static {
        String[][] pairs = {
                {"javax.ws.rs.GET", "jakarta.ws.rs.GET"},
                {"javax.ws.rs.PUT", "jakarta.ws.rs.PUT"},
                {"javax.ws.rs.POST", "jakarta.ws.rs.POST"},
                {"javax.ws.rs.DELETE", "jakarta.ws.rs.DELETE"},
                {"javax.ws.rs.OPTIONS", "jakarta.ws.rs.OPTIONS"},
                {"javax.ws.rs.HEAD", "jakarta.ws.rs.HEAD"},
                {"javax.ws.rs.PATCH", "jakarta.ws.rs.PATCH"},
                {"javax.ws.rs.Path", "jakarta.ws.rs.Path"},
                {"javax.ws.rs.Produces", "jakarta.ws.rs.Produces"},
                {"javax.ws.rs.Consumes", "jakarta.ws.rs.Consumes"},
                {"javax.ws.rs.ApplicationPath", "jakarta.ws.rs.ApplicationPath"},
                {"javax.ws.rs.HttpMethod", "jakarta.ws.rs.HttpMethod"},
                {"javax.ws.rs.FormParam", "jakarta.ws.rs.FormParam"},
                {"javax.ws.rs.PathParam", "jakarta.ws.rs.PathParam"},
                {"javax.ws.rs.QueryParam", "jakarta.ws.rs.QueryParam"},
                {"javax.ws.rs.HeaderParam", "jakarta.ws.rs.HeaderParam"},
                {"javax.ws.rs.CookieParam", "jakarta.ws.rs.CookieParam"},
                {"javax.ws.rs.MatrixParam", "jakarta.ws.rs.MatrixParam"},
                {"javax.ws.rs.BeanParam", "jakarta.ws.rs.BeanParam"},
                {"javax.ws.rs.core.Context", "jakarta.ws.rs.core.Context"},
                {"javax.ws.rs.DefaultValue", "jakarta.ws.rs.DefaultValue"},
                {"javax.ws.rs.core.MediaType", "jakarta.ws.rs.core.MediaType"},
                {"javax.ws.rs.core.Application", "jakarta.ws.rs.core.Application"},
                {"javax.ws.rs.Response", "jakarta.ws.rs.Response"},
        };
        for (String[] pair : pairs) {
            JAVAX_TO_JAKARTA.put(pair[0], pair[1]);
            JAKARTA_TO_JAVAX.put(pair[1], pair[0]);
        }
    }

    public static boolean isJaxRsAnnotation(Annotation annotation, String jaxRsSimpleName) {
        if (annotation == null) {
            return false;
        }
        String annotationName = annotation.annotationType().getName();
        String javaxName = JAVAX_PREFIX + "." + jaxRsSimpleName;
        String jakartaName = JAKARTA_PREFIX + "." + jaxRsSimpleName;
        return annotationName.equals(javaxName) || annotationName.equals(jakartaName);
    }

    public static boolean isJaxRsAnnotation(Annotation annotation, String jaxRsSimpleName, String subPackage) {
        if (annotation == null) {
            return false;
        }
        String annotationName = annotation.annotationType().getName();
        String javaxName = JAVAX_PREFIX + "." + subPackage + "." + jaxRsSimpleName;
        String jakartaName = JAKARTA_PREFIX + "." + subPackage + "." + jaxRsSimpleName;
        return annotationName.equals(javaxName) || annotationName.equals(jakartaName);
    }

    public static boolean matchesJaxRsName(String annotationTypeName, String jaxRsSimpleName) {
        if (annotationTypeName == null) {
            return false;
        }
        String javaxName = JAVAX_PREFIX + "." + jaxRsSimpleName;
        String jakartaName = JAKARTA_PREFIX + "." + jaxRsSimpleName;
        return annotationTypeName.equals(javaxName) || annotationTypeName.equals(jakartaName);
    }

    public static boolean matchesJaxRsName(String annotationTypeName, String jaxRsSimpleName, String subPackage) {
        if (annotationTypeName == null) {
            return false;
        }
        String javaxName = JAVAX_PREFIX + "." + subPackage + "." + jaxRsSimpleName;
        String jakartaName = JAKARTA_PREFIX + "." + subPackage + "." + jaxRsSimpleName;
        return annotationTypeName.equals(javaxName) || annotationTypeName.equals(jakartaName);
    }

    public static boolean hasAnnotation(AnnotatedElement element, String jaxRsSimpleName) {
        for (Annotation annotation : element.getAnnotations()) {
            if (isJaxRsAnnotation(annotation, jaxRsSimpleName)) {
                return true;
            }
        }
        return false;
    }

    public static Annotation getAnnotation(AnnotatedElement element, String jaxRsSimpleName) {
        for (Annotation annotation : element.getAnnotations()) {
            if (isJaxRsAnnotation(annotation, jaxRsSimpleName)) {
                return annotation;
            }
        }
        return null;
    }

    public static Annotation getAnnotation(Annotation[] annotations, String jaxRsSimpleName) {
        if (annotations == null) {
            return null;
        }
        for (Annotation annotation : annotations) {
            if (isJaxRsAnnotation(annotation, jaxRsSimpleName)) {
                return annotation;
            }
        }
        return null;
    }

    public static String getAnnotationValue(Annotation annotation) {
        if (annotation == null) {
            return null;
        }
        try {
            Method valueMethod = annotation.annotationType().getMethod(VALUE_METHOD);
            return (String) valueMethod.invoke(annotation);
        } catch (Exception e) {
            return null;
        }
    }

    public static String[] getAnnotationValues(Annotation annotation) {
        if (annotation == null) {
            return new String[0];
        }
        try {
            Method valueMethod = annotation.annotationType().getMethod(VALUE_METHOD);
            return (String[]) valueMethod.invoke(annotation);
        } catch (Exception e) {
            return new String[0];
        }
    }

    public static Class<?> loadAnnotationClass(String className) {
        try {
            return Class.forName(className);
        } catch (ClassNotFoundException e) {
            return null;
        }
    }

    public static Class<?> loadJaxRsClass(String jaxRsSimpleName) {
        String jakartaName = JAKARTA_PREFIX + "." + jaxRsSimpleName;
        String javaxName = JAVAX_PREFIX + "." + jaxRsSimpleName;
        Class<?> cls = loadAnnotationClass(jakartaName);
        if (cls != null) {
            return cls;
        }
        return loadAnnotationClass(javaxName);
    }

    public static boolean isJaxRsClass(String className) {
        if (className == null) {
            return false;
        }
        return className.startsWith(JAVAX_PREFIX + ".") || className.startsWith(JAKARTA_PREFIX + ".");
    }

    public static String getCounterpartName(String annotationName) {
        if (annotationName == null) {
            return null;
        }
        if (annotationName.startsWith(JAVAX_PREFIX)) {
            return JAVAX_TO_JAKARTA.get(annotationName);
        }
        if (annotationName.startsWith(JAKARTA_PREFIX)) {
            return JAKARTA_TO_JAVAX.get(annotationName);
        }
        return null;
    }

    public static String extractHttpMethod(Method method) {
        String[] httpMethodAnnotations = {"GET", "PUT", "POST", "DELETE", "OPTIONS", "HEAD", "PATCH"};
        String[] methodNames = {"get", "put", "post", "delete", "options", "head", "patch"};
        for (int i = 0; i < httpMethodAnnotations.length; i++) {
            if (hasAnnotation(method, httpMethodAnnotations[i])) {
                return methodNames[i];
            }
        }
        return null;
    }

    private static volatile Class<?> applicationClass;

    public static Class<?> getApplicationClass() {
        if (applicationClass == null) {
            synchronized (JaxRsAnnotationUtils.class) {
                if (applicationClass == null) {
                    Class<?> cls = loadAnnotationClass("jakarta.ws.rs.core.Application");
                    if (cls == null) {
                        cls = loadAnnotationClass("javax.ws.rs.core.Application");
                    }
                    applicationClass = cls;
                }
            }
        }
        return applicationClass;
    }

    public static boolean isApplicationClass(Class<?> cls) {
        Class<?> appClass = getApplicationClass();
        return appClass != null && appClass.isAssignableFrom(cls);
    }

    public static boolean isApplicationSuperclass(Class<?> cls) {
        Class<?> appClass = getApplicationClass();
        return appClass != null && cls.equals(appClass);
    }

    public static Annotation getReflectionUtilsAnnotation(Class<?> cls, String jaxRsSimpleName) {
        Annotation annotation = JaxRsAnnotationUtils.getAnnotation(cls, jaxRsSimpleName);
        if (annotation == null) {
            Class<?> counterpartClass = loadJaxRsCounterpartClass(jaxRsSimpleName);
            if (counterpartClass != null) {
                annotation = cls.getAnnotation((Class<? extends Annotation>) counterpartClass);
            }
        }
        return annotation;
    }

    @SuppressWarnings("unchecked")
    public static <A extends Annotation> A getReflectionUtilsAnnotation(Method method, String jaxRsSimpleName, Class<A> fallbackClass) {
        Annotation annotation = JaxRsAnnotationUtils.getAnnotation(method, jaxRsSimpleName);
        if (annotation != null && fallbackClass.isInstance(annotation)) {
            return (A) annotation;
        }
        return null;
    }

    private static Class<?> loadJaxRsCounterpartClass(String jaxRsSimpleName) {
        String javaxName = JAVAX_PREFIX + "." + jaxRsSimpleName;
        String jakartaName = JAKARTA_PREFIX + "." + jaxRsSimpleName;
        Class<?> cls = loadAnnotationClass(jakartaName);
        if (cls == null) {
            cls = loadAnnotationClass(javaxName);
        }
        return cls;
    }

    public static String getMediaTypeApplicationFormUrlencoded() {
        Class<?> mediaTypeClass = loadAnnotationClass("jakarta.ws.rs.core.MediaType");
        if (mediaTypeClass == null) {
            mediaTypeClass = loadAnnotationClass("javax.ws.rs.core.MediaType");
        }
        if (mediaTypeClass != null) {
            try {
                java.lang.reflect.Field field = mediaTypeClass.getField("APPLICATION_FORM_URLENCODED");
                return (String) field.get(null);
            } catch (Exception e) {
                return "application/x-www-form-urlencoded";
            }
        }
        return "application/x-www-form-urlencoded";
    }

    public static String getMediaTypeMultipartFormData() {
        Class<?> mediaTypeClass = loadAnnotationClass("jakarta.ws.rs.core.MediaType");
        if (mediaTypeClass == null) {
            mediaTypeClass = loadAnnotationClass("javax.ws.rs.core.MediaType");
        }
        if (mediaTypeClass != null) {
            try {
                java.lang.reflect.Field field = mediaTypeClass.getField("MULTIPART_FORM_DATA");
                return (String) field.get(null);
            } catch (Exception e) {
                return "multipart/form-data";
            }
        }
        return "multipart/form-data";
    }
}
