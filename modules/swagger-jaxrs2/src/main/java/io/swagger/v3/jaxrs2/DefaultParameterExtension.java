package io.swagger.v3.jaxrs2;

import com.fasterxml.jackson.annotation.JsonView;
import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.introspect.AnnotatedField;
import com.fasterxml.jackson.databind.introspect.AnnotatedMethod;
import com.fasterxml.jackson.databind.introspect.AnnotationMap;
import com.fasterxml.jackson.databind.introspect.BeanPropertyDefinition;

import io.swagger.v3.core.util.Json;
import io.swagger.v3.core.util.ParameterProcessor;
import io.swagger.v3.jaxrs2.ext.AbstractOpenAPIExtension;
import io.swagger.v3.jaxrs2.ext.OpenAPIExtension;
import io.swagger.v3.jaxrs2.ext.OpenAPIExtensions;
import io.swagger.v3.jaxrs2.util.JaxRsAnnotationLoader;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.parameters.Parameter;
import org.apache.commons.lang3.StringUtils;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public class DefaultParameterExtension extends AbstractOpenAPIExtension {
    private static final String QUERY_PARAM = "query";
    private static final String HEADER_PARAM = "header";
    private static final String COOKIE_PARAM = "cookie";
    private static final String PATH_PARAM = "path";

    final ObjectMapper mapper = Json.mapper();

    @Override
    public ResolvedParameter extractParameters(List<Annotation> annotations,
                                               Type type,
                                               Set<Type> typesToSkip,
                                               Components components,
                                               Annotation classConsumes,
                                               Annotation methodConsumes,
                                               boolean includeRequestBody,
                                               JsonView jsonViewAnnotation,
                                               Iterator<OpenAPIExtension> chain) {
        if (shouldIgnoreType(type, typesToSkip)) {
            return new ResolvedParameter();
        }


        Parameter parameter = null;
        for (Annotation annotation : annotations) {
            if (JaxRsAnnotationLoader.isAnnotationInstanceOf(annotation, "QueryParam")) {
                Parameter qp = new Parameter();
                qp.setIn(QUERY_PARAM);
                qp.setName(JaxRsAnnotationLoader.getAnnotationValue(annotation, "value"));
                parameter = qp;
            } else if (JaxRsAnnotationLoader.isAnnotationInstanceOf(annotation, "PathParam")) {
                Parameter pp = new Parameter();
                pp.setIn(PATH_PARAM);
                pp.setName(JaxRsAnnotationLoader.getAnnotationValue(annotation, "value"));
                parameter = pp;
            } else if (JaxRsAnnotationLoader.isAnnotationInstanceOf(annotation, "MatrixParam")) {
                Parameter pp = new Parameter();
                pp.setIn(PATH_PARAM);
                pp.setStyle(Parameter.StyleEnum.MATRIX);
                pp.setName(JaxRsAnnotationLoader.getAnnotationValue(annotation, "value"));
                parameter = pp;
            } else if (JaxRsAnnotationLoader.isAnnotationInstanceOf(annotation, "HeaderParam")) {
                Parameter pp = new Parameter();
                pp.setIn(HEADER_PARAM);
                pp.setName(JaxRsAnnotationLoader.getAnnotationValue(annotation, "value"));
                parameter = pp;
            } else if (JaxRsAnnotationLoader.isAnnotationInstanceOf(annotation, "CookieParam")) {
                Parameter pp = new Parameter();
                pp.setIn(COOKIE_PARAM);
                pp.setName(JaxRsAnnotationLoader.getAnnotationValue(annotation, "value"));
                parameter = pp;
            } else if (annotation instanceof io.swagger.v3.oas.annotations.Parameter) {
                if (((io.swagger.v3.oas.annotations.Parameter) annotation).hidden()) {
                    return new ResolvedParameter();
                }
                if (parameter == null) {
                    parameter = new Parameter();
                }
                if (StringUtils.isNotBlank(((io.swagger.v3.oas.annotations.Parameter) annotation).ref())) {
                    parameter.$ref(((io.swagger.v3.oas.annotations.Parameter) annotation).ref());
                }
            } else {
                List<Parameter> formParameters = new ArrayList<>();
                List<Parameter> parameters = new ArrayList<>();
                if (handleAdditionalAnnotation(parameters, formParameters, annotation, type, typesToSkip, classConsumes, methodConsumes, components, includeRequestBody, jsonViewAnnotation)) {
                    ResolvedParameter extractParametersResult = new ResolvedParameter();
                    extractParametersResult.parameters.addAll(parameters);
                    extractParametersResult.formParameters.addAll(formParameters);
                    return extractParametersResult;
                }
            }
        }
        List<Parameter> parameters = new ArrayList<>();
        ResolvedParameter extractParametersResult = new ResolvedParameter();

        if (parameter != null && (StringUtils.isNotBlank(parameter.getIn()) || StringUtils.isNotBlank(parameter.get$ref()))) {
            parameters.add(parameter);
        } else if (includeRequestBody) {
            Parameter unknownParameter = ParameterProcessor.applyAnnotations(
                    null,
                    type,
                    annotations,
                    components,
                    classConsumes == null ? new String[0] : JaxRsAnnotationLoader.getAnnotationValues(classConsumes, "value"),
                    methodConsumes == null ? new String[0] : JaxRsAnnotationLoader.getAnnotationValues(methodConsumes, "value"), jsonViewAnnotation, configuration);
            if (unknownParameter != null) {
                if (StringUtils.isNotBlank(unknownParameter.getIn()) && !"form".equals(unknownParameter.getIn())) {
                    extractParametersResult.parameters.add(unknownParameter);
                } else if ("form".equals(unknownParameter.getIn())) {
                    unknownParameter.setIn(null);
                    extractParametersResult.formParameters.add(unknownParameter);
                } else {
                    extractParametersResult.requestBody = unknownParameter;
                }
            }
        }
        for (Parameter p : parameters) {
            Parameter processedParameter = ParameterProcessor.applyAnnotations(
                    p,
                    type,
                    annotations,
                    components,
                    classConsumes == null ? new String[0] : JaxRsAnnotationLoader.getAnnotationValues(classConsumes, "value"),
                    methodConsumes == null ? new String[0] : JaxRsAnnotationLoader.getAnnotationValues(methodConsumes, "value"),
                    jsonViewAnnotation,
                    openapi31,
                    this.schemaResolution);
            if (processedParameter != null) {
                extractParametersResult.parameters.add(processedParameter);
            }
        }
        return extractParametersResult;
    }

    private boolean handleAdditionalAnnotation(List<Parameter> parameters, List<Parameter> formParameters, Annotation annotation,
                                               final Type type, Set<Type> typesToSkip, Annotation classConsumes,
                                               Annotation methodConsumes, Components components, boolean includeRequestBody, JsonView jsonViewAnnotation) {
        boolean processed = false;
        if (JaxRsAnnotationLoader.isAnnotationInstanceOf(annotation, "BeanParam")) {
            final BeanDescription beanDesc = mapper.getSerializationConfig().introspect(constructType(type));
            final List<BeanPropertyDefinition> properties = beanDesc.findProperties();

            for (final BeanPropertyDefinition propDef : properties) {
                final AnnotatedField field = propDef.getField();
                final AnnotatedMethod setter = propDef.getSetter();
                final AnnotatedMethod getter = propDef.getGetter();
                final List<Annotation> paramAnnotations = new ArrayList<>();
                final Iterator<OpenAPIExtension> extensions = OpenAPIExtensions.chain();
                Type paramType = null;

                if (field != null) {
                    paramType = field.getType();
                    AnnotationMap annotationMap = field.getAllAnnotations();
                    if (annotationMap != null) {
                        for (final Annotation fieldAnnotation : annotationMap.annotations()) {
                            if (!paramAnnotations.contains(fieldAnnotation)) {
                                paramAnnotations.add(fieldAnnotation);
                            }
                        }
                    }
                }

                if (setter != null) {
                    if (paramType == null) {
                        paramType = setter.getParameterType(0);
                    }
                    AnnotationMap annotationMap = setter.getAllAnnotations();
                    if (annotationMap != null) {
                        for (final Annotation fieldAnnotation : annotationMap.annotations()) {
                            if (!paramAnnotations.contains(fieldAnnotation)) {
                                paramAnnotations.add(fieldAnnotation);
                            }
                        }
                    }
                }

                if (getter != null) {
                    if (paramType == null) {
                        paramType = getter.getType();
                    }
                    AnnotationMap annotationMap = getter.getAllAnnotations();
                    if (annotationMap != null) {
                        for (final Annotation fieldAnnotation : annotationMap.annotations()) {
                            if (!paramAnnotations.contains(fieldAnnotation)) {
                                paramAnnotations.add(fieldAnnotation);
                            }
                        }
                    }
                }

                if (paramType == null) {
                    continue;
                }

                boolean hidden  = false;
                for (Annotation a : paramAnnotations) {
                    if (a instanceof io.swagger.v3.oas.annotations.media.Schema) {
                        if (((io.swagger.v3.oas.annotations.media.Schema) a).hidden()) {
                            hidden = true;
                            break;
                        };
                    } else if (a instanceof Hidden) {
                        hidden = true;
                        break;
                    }
                }
                if (hidden) {
                    continue;
                }
                ResolvedParameter resolvedParameter = extensions.next().extractParameters(
                        paramAnnotations,
                        paramType,
                        typesToSkip,
                        components,
                        classConsumes,
                        methodConsumes,
                        includeRequestBody,
                        jsonViewAnnotation,
                        extensions);

                List<Parameter> extractedParameters =
                        resolvedParameter.parameters;

                for (Parameter p : extractedParameters) {
                    if (p != null) {
                        parameters.add(p);
                    }
                }

                List<Parameter> extractedFormParameters =
                        resolvedParameter.formParameters;

                for (Parameter p : extractedFormParameters) {
                    if (p != null) {
                        formParameters.add(p);
                    }
                }

                processed = true;
            }
        }
        return processed;
    }

    @Override
    protected boolean shouldIgnoreClass(Class<?> cls) {
        return JaxRsAnnotationLoader.shouldIgnoreClass(cls.getName());
    }

}
