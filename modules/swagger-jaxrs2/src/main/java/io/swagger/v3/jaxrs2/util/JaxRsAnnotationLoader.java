package io.swagger.v3.jaxrs2.util;

import io.swagger.v3.core.util.ReflectionUtils;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

/**
 * Utility class that provides dynamic loading of JAX-RS annotations for both
 * the legacy {@code javax.ws.rs.*} namespace (JAX-RS 2.x / Jakarta EE 8 and earlier)
 * and the modern {@code jakarta.ws.rs.*} namespace (Jakarta REST 3.0+ / Jakarta EE 9+).
 *
 * <p>This class is designed to be a drop-in replacement for hard-coded references to
 * annotation classes such as {@code javax.ws.rs.GET.class}, {@code javax.ws.rs.Path.class},
 * etc. By using reflection to look up the annotation classes by fully-qualified name,
 * the swagger-jaxrs2 module can generate OpenAPI models in environments using either
 * the javax or jakarta API without requiring separate artifacts.</p>
 *
 * <p>For example, instead of writing {@code method.isAnnotationPresent(GET.class)},
 * callers should use {@code JaxRsAnnotationLoader.hasPathAnnotation(method)} or the
 * more general {@code JaxRsAnnotationLoader.isAnnotationPresent(method, "GET")}.</p>
 *
 * <p>Annotation classes are cached after the first successful resolution to avoid
 * repeated {@link Class#forName} overhead on repeated invocations.</p>
 */
public final class JaxRsAnnotationLoader {

    private static final String JAVAX_PREFIX = "javax.ws.rs.";
    private static final String JAKARTA_PREFIX = "jakarta.ws.rs.";

    // Common short names used throughout swagger-jaxrs2
    public static final String ANNOTATION_GET = "GET";
    public static final String ANNOTATION_POST = "POST";
    public static final String ANNOTATION_PUT = "PUT";
    public static final String ANNOTATION_DELETE = "DELETE";
    public static final String ANNOTATION_HEAD = "HEAD";
    public static final String ANNOTATION_OPTIONS = "OPTIONS";
    public static final String ANNOTATION_PATCH = "PATCH";
    public static final String ANNOTATION_PATH = "Path";
    public static final String ANNOTATION_CONSUMES = "Consumes";
    public static final String ANNOTATION_PRODUCES = "Produces";
    public static final String ANNOTATION_HTTP_METHOD = "HttpMethod";
    public static final String ANNOTATION_APPLICATION_PATH = "ApplicationPath";
    public static final String ANNOTATION_BEAN_PARAM = "BeanParam";
    public static final String ANNOTATION_QUERY_PARAM = "QueryParam";
    public static final String ANNOTATION_PATH_PARAM = "PathParam";
    public static final String ANNOTATION_HEADER_PARAM = "HeaderParam";
    public static final String ANNOTATION_COOKIE_PARAM = "CookieParam";
    public static final String ANNOTATION_MATRIX_PARAM = "MatrixParam";
    public static final String ANNOTATION_FORM_PARAM = "FormParam";
    public static final String ANNOTATION_CONTEXT = "Context";

    private static final Map<String, Class<? extends Annotation>> ANNOTATION_CLASS_CACHE = new HashMap<>();

    static {
        // Prime the cache with the most commonly accessed annotation classes so that
        // subsequent look-ups during scanning are as cheap as a map read.
        resolveCached(ANNOTATION_GET);
        resolveCached(ANNOTATION_POST);
        resolveCached(ANNOTATION_PUT);
        resolveCached(ANNOTATION_DELETE);
        resolveCached(ANNOTATION_HEAD);
        resolveCached(ANNOTATION_OPTIONS);
        resolveCached(ANNOTATION_PATCH);
        resolveCached(ANNOTATION_PATH);
        resolveCached(ANNOTATION_CONSUMES);
        resolveCached(ANNOTATION_PRODUCES);
        resolveCached(ANNOTATION_HTTP_METHOD);
        resolveCached(ANNOTATION_APPLICATION_PATH);
        resolveCached(ANNOTATION_BEAN_PARAM);
        resolveCached(ANNOTATION_QUERY_PARAM);
        resolveCached(ANNOTATION_PATH_PARAM);
        resolveCached(ANNOTATION_HEADER_PARAM);
        resolveCached(ANNOTATION_COOKIE_PARAM);
        resolveCached(ANNOTATION_MATRIX_PARAM);
        resolveCached(ANNOTATION_FORM_PARAM);
        resolveCached(ANNOTATION_CONTEXT);
    }

    private JaxRsAnnotationLoader() {
    }

    /**
     * Attempts to resolve the Class object for a JAX-RS annotation by trying both the
     * {@code javax.ws.rs.} and {@code jakarta.ws.rs.} namespace prefixes. If neither is
     * present on the classpath, returns {@code null} rather than throwing.
     *
     * @param simpleName the simple class name of the annotation (e.g. {@code "GET"}, {@code "Path"})
     * @return the resolved annotation Class, or {@code null} if no matching class is available
     */
    @SuppressWarnings("unchecked")
    public static Class<? extends Annotation> getAnnotationClass(String simpleName) {
        Class<? extends Annotation> cached = resolveCached(simpleName);
        if (cached != null) {
            return cached;
        }
        Class<? extends Annotation> found = null;
        try {
            found = (Class<? extends Annotation>) Class.forName(JAVAX_PREFIX + simpleName);
        } catch (ClassNotFoundException javaxNotFound) {
            // Try jakarta namespace instead
            try {
                found = (Class<? extends Annotation>) Class.forName(JAKARTA_PREFIX + simpleName);
            } catch (ClassNotFoundException jakartaNotFound) {
                // Neither implementation is on the classpath; leave found as null.
            }
        } catch (Throwable ignored) {
            // Defensive: ClassFormatError, LinkageError, etc. should not crash the loader.
        }
        synchronized (ANNOTATION_CLASS_CACHE) {
            ANNOTATION_CLASS_CACHE.put(simpleName, found);
        }
        return found;
    }

    /**
     * Attempts to resolve a specific fully-qualified annotation class name. This
     * helper is useful when the caller knows exactly which namespace variant it wants.
     *
     * @param fullyQualifiedName the fully-qualified annotation class name
     * @return the Class object if it exists on the classpath, otherwise {@code null}
     */
    @SuppressWarnings("unchecked")
    public static Class<? extends Annotation> resolveByFullyQualifiedName(String fullyQualifiedName) {
        try {
            return (Class<? extends Annotation>) Class.forName(fullyQualifiedName);
        } catch (ClassNotFoundException | Throwable ignored) {
            return null;
        }
    }

    /**
     * Returns the Class object of the JAX-RS {@code HttpMethod} meta-annotation
     * (either {@code javax.ws.rs.HttpMethod} or {@code jakarta.ws.rs.HttpMethod}),
     * whichever is available on the classpath.
     */
    public static Class<? extends Annotation> getHttpMethodMetaAnnotation() {
        return getAnnotationClass(ANNOTATION_HTTP_METHOD);
    }

    // ---------------------------------------------------------------------
    // Presence checks on Method / Class / Annotation objects
    // ---------------------------------------------------------------------

    /**
     * Determines whether the supplied method is annotated with the named JAX-RS
     * annotation under either namespace.
     *
     * @param method     the method to inspect; must not be {@code null}
     * @param simpleName the simple class name of the annotation
     * @return {@code true} if the method is annotated with the requested JAX-RS annotation
     */
    public static boolean isAnnotationPresent(Method method, String simpleName) {
        if (method == null) {
            return false;
        }
        Class<? extends Annotation> annotationClass = getAnnotationClass(simpleName);
        if (annotationClass == null) {
            return false;
        }
        return method.getAnnotation(annotationClass) != null;
    }

    /**
     * Determines whether the supplied class is annotated with the named JAX-RS
     * annotation under either namespace.
     */
    public static boolean isAnnotationPresent(Class<?> clazz, String simpleName) {
        if (clazz == null) {
            return false;
        }
        Class<? extends Annotation> annotationClass = getAnnotationClass(simpleName);
        if (annotationClass == null) {
            return false;
        }
        return clazz.getAnnotation(annotationClass) != null;
    }

    /**
     * Determines whether the supplied annotation instance is a JAX-RS annotation
     * with the given simple name (either javax or jakarta variant).
     */
    public static boolean isAnnotationType(Annotation annotation, String simpleName) {
        if (annotation == null) {
            return false;
        }
        Class<? extends Annotation> annotationClass = getAnnotationClass(simpleName);
        if (annotationClass == null) {
            return false;
        }
        return annotationClass.isInstance(annotation);
    }

    /**
     * Determines whether the supplied annotation's type extends or equals a JAX-RS
     * annotation with the given simple name. Convenience for parameter annotations.
     */
    public static boolean isAnnotationType(Class<? extends Annotation> annotationType, String simpleName) {
        if (annotationType == null) {
            return false;
        }
        Class<? extends Annotation> resolvedClass = getAnnotationClass(simpleName);
        if (resolvedClass == null) {
            return false;
        }
        return resolvedClass.isAssignableFrom(annotationType);
    }

    // ---------------------------------------------------------------------
    // Annotation retrieval (typed values with reflection-based value access)
    // ---------------------------------------------------------------------

    public static <A extends Annotation> A getAnnotation(Method method, String simpleName) {
        return getAnnotationInternal(method, simpleName);
    }

    public static <A extends Annotation> A getAnnotation(Class<?> clazz, String simpleName) {
        return getAnnotationInternal(clazz, simpleName);
    }

    /**
     * Retrieves an annotation instance using the same recursive look-up logic as
     * {@link ReflectionUtils#getAnnotation(Method, Class)}, but against the
     * dynamically-resolved JAX-RS annotation class.
     */
    @SuppressWarnings("unchecked")
    private static <A extends Annotation> A getAnnotationInternal(Object target, String simpleName) {
        Class<? extends Annotation> annotationClass = getAnnotationClass(simpleName);
        if (annotationClass == null) {
            return null;
        }
        if (target instanceof Method) {
            return (A) ReflectionUtils.getAnnotation((Method) target, annotationClass);
        }
        if (target instanceof Class<?>) {
            return (A) ReflectionUtils.getAnnotation((Class<?>) target, annotationClass);
        }
        return null;
    }

    // ---------------------------------------------------------------------
    // High-level helpers used by ReaderUtils / Reader / DefaultParameterExtension
    // ---------------------------------------------------------------------

    /**
     * Returns the HTTP method verb for a given {@code javax / jakarta .ws.rs.HttpMethod}
     * meta-annotation (e.g. {@code "GET"}, {@code "POST"}, etc.).
     */
    public static String extractHttpMethodValue(Annotation methodAnnotation) {
        if (methodAnnotation == null) {
            return null;
        }
        Class<? extends Annotation> httpMethodClass = getHttpMethodMetaAnnotation();
        if (httpMethodClass == null) {
            return null;
        }
        Annotation httpMeta = methodAnnotation.annotationType().getAnnotation(httpMethodClass);
        if (httpMeta == null) {
            return null;
        }
        try {
            Method valueMethod = httpMethodClass.getDeclaredMethod("value");
            Object value = valueMethod.invoke(httpMeta);
            return value == null ? null : value.toString();
        } catch (Exception ignored) {
            return null;
        }
    }

    /**
     * Extracts the HTTP method ({@code "get"}, {@code "post"}, etc.) for a method
     * by inspecting the standard JAX-RS verb annotations as well as any custom
     * annotation meta-annotated with {@code @HttpMethod}. This mirrors the logic
     * previously hard-coded in {@code ReaderUtils.extractOperationMethod}, so that
     * it works transparently on both javax and jakarta runtimes.
     *
     * @param method the method to inspect
     * @return the lowercase HTTP verb, or {@code null} if no JAX-RS verb annotation is present
     */
    public static String getOperationMethod(Method method) {
        if (method == null) {
            return null;
        }
        if (isAnnotationPresent(method, ANNOTATION_GET)) {
            return "get";
        }
        if (isAnnotationPresent(method, ANNOTATION_POST)) {
            return "post";
        }
        if (isAnnotationPresent(method, ANNOTATION_PUT)) {
            return "put";
        }
        if (isAnnotationPresent(method, ANNOTATION_DELETE)) {
            return "delete";
        }
        if (isAnnotationPresent(method, ANNOTATION_HEAD)) {
            return "head";
        }
        if (isAnnotationPresent(method, ANNOTATION_OPTIONS)) {
            return "options";
        }
        if (isAnnotationPresent(method, ANNOTATION_PATCH)) {
            return "patch";
        }
        return getHttpMethodFromCustomAnnotations(method);
    }

    /**
     * Checks the method's annotations for any custom annotation that is itself
     * meta-annotated with {@code @HttpMethod} (either namespace) and returns the
     * declared HTTP method verb, or {@code null} if no such annotation is found.
     */
    public static String getHttpMethodFromCustomAnnotations(Method method) {
        if (method == null) {
            return null;
        }
        for (Annotation methodAnnotation : method.getAnnotations()) {
            String verb = extractHttpMethodValue(methodAnnotation);
            if (verb != null && !verb.isEmpty()) {
                return verb.toLowerCase();
            }
        }
        return null;
    }

    public static boolean hasPathAnnotation(Method method) {
        return isAnnotationPresent(method, ANNOTATION_PATH);
    }

    public static boolean hasPathAnnotation(Class<?> clazz) {
        return isAnnotationPresent(clazz, ANNOTATION_PATH);
    }

    public static Annotation getPathAnnotation(Class<?> clazz) {
        return getAnnotation(clazz, ANNOTATION_PATH);
    }

    public static Annotation getPathAnnotation(Method method) {
        return getAnnotation(method, ANNOTATION_PATH);
    }

    public static Annotation getConsumesAnnotation(Class<?> clazz) {
        return getAnnotation(clazz, ANNOTATION_CONSUMES);
    }

    public static Annotation getProducesAnnotation(Class<?> clazz) {
        return getAnnotation(clazz, ANNOTATION_PRODUCES);
    }

    public static Annotation getConsumesAnnotation(Method method) {
        return getAnnotation(method, ANNOTATION_CONSUMES);
    }

    public static Annotation getProducesAnnotation(Method method) {
        return getAnnotation(method, ANNOTATION_PRODUCES);
    }

    /**
     * Given a JAX-RS {@code @Path}, {@code @Consumes}, or {@code @Produces} annotation,
     * returns its {@code value()} array by reflection. The annotation may be from
     * either the {@code javax.ws.rs} or {@code jakarta.ws.rs} namespace.
     */
    public static String[] getAnnotationValue(Annotation annotation) {
        if (annotation == null) {
            return null;
        }
        try {
            Method valueMethod = annotation.annotationType().getDeclaredMethod("value");
            Object raw = valueMethod.invoke(annotation);
            if (raw instanceof String[]) {
                return (String[]) raw;
            }
            if (raw instanceof String) {
                return new String[]{(String) raw};
            }
        } catch (Exception ignored) {
            // Intentionally swallow reflection failures; callers check for null.
        }
        return new String[0];
    }

    /**
     * Given a JAX-RS parameter annotation such as {@code @PathParam("name")},
     * returns the string value of the annotation via reflection. Works for both
     * javax and jakarta variants.
     */
    public static String getParamAnnotationValue(Annotation annotation) {
        if (annotation == null) {
            return null;
        }
        try {
            Method valueMethod = annotation.annotationType().getDeclaredMethod("value");
            Object raw = valueMethod.invoke(annotation);
            return raw == null ? null : raw.toString();
        } catch (Exception ignored) {
            return null;
        }
    }

    /**
     * Indicates whether the supplied JAX-RS annotation simple name is one of the
     * supported parameter-binding annotations such as {@code QueryParam}.
     */
    public static boolean isParameterAnnotation(String simpleName) {
        if (simpleName == null) {
            return false;
        }
        switch (simpleName) {
            case ANNOTATION_QUERY_PARAM:
            case ANNOTATION_PATH_PARAM:
            case ANNOTATION_HEADER_PARAM:
            case ANNOTATION_COOKIE_PARAM:
            case ANNOTATION_MATRIX_PARAM:
            case ANNOTATION_FORM_PARAM:
            case ANNOTATION_BEAN_PARAM:
            case ANNOTATION_CONTEXT:
                return true;
            default:
                return false;
        }
    }

    private static Class<? extends Annotation> resolveCached(String simpleName) {
        synchronized (ANNOTATION_CLASS_CACHE) {
            if (ANNOTATION_CLASS_CACHE.containsKey(simpleName)) {
                return ANNOTATION_CLASS_CACHE.get(simpleName);
            }
            return null;
        }
    }
}
