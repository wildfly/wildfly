/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.jboss.as.ee.container.processor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import javax.annotation.Resource;
import javax.annotation.Resources;
import org.jboss.as.ee.container.ComponentConfiguration;
import org.jboss.as.ee.container.injection.ResourceInjectionConfiguration;
import org.jboss.as.ee.container.interceptor.MethodInterceptorConfiguration;
import org.jboss.as.ee.container.service.Attachments;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.server.deployment.annotation.CompositeIndex;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.FieldInfo;
import org.jboss.jandex.MethodInfo;

/**
 * Deployment processor responsible for analyzing each attached {@link org.jboss.as.ee.container.ComponentConfiguration} instance to configure
 * required resource injection configurations.
 *
 * @author John Bailey
 */
public class ResourceInjectionAnnotationParsingProcessor implements DeploymentUnitProcessor {
    private static final DotName RESOURCE_ANNOTATION_NAME = DotName.createSimple(Resource.class.getName());
    private static final DotName RESOURCES_ANNOTATION_NAME = DotName.createSimple(Resources.class.getName());

    public void deploy(DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        final List<ComponentConfiguration> containerConfigs = deploymentUnit.getAttachment(Attachments.BEAN_CONTAINER_CONFIGS);
        if (containerConfigs == null || containerConfigs.isEmpty()) {
            return;
        }

        final CompositeIndex index = deploymentUnit.getAttachment(org.jboss.as.server.deployment.Attachments.COMPOSITE_ANNOTATION_INDEX);
        if (index == null) {
            return;
        }

        for (ComponentConfiguration containerConfig : containerConfigs) {
            final ClassInfo classInfo = index.getClassByName(DotName.createSimple(containerConfig.getBeanClass()));
            containerConfig.addResourceInjectionConfigs(getResourceConfigurations(classInfo));

            final List<MethodInterceptorConfiguration> interceptorConfigurations = containerConfig.getMethodInterceptorConfigs();
            for(MethodInterceptorConfiguration interceptorConfiguration : interceptorConfigurations) {
                final ClassInfo interceptorClassInfo = index.getClassByName(DotName.createSimple(interceptorConfiguration.getInterceptorClassName()));
                interceptorConfiguration.addResourceInjectionConfigs(getResourceConfigurations(interceptorClassInfo));
            }
        }
    }

    public void undeploy(DeploymentUnit context) {
    }

    public static List<ResourceInjectionConfiguration> getResourceConfigurations(final ClassInfo classInfo) {
        final List<ResourceInjectionConfiguration> configurations = new ArrayList<ResourceInjectionConfiguration>();

        final Map<DotName, List<AnnotationInstance>> classAnnotations = classInfo.annotations();
        if (classAnnotations != null) {
            final List<AnnotationInstance> resourceAnnotations = classAnnotations.get(RESOURCE_ANNOTATION_NAME);
            if (resourceAnnotations != null) for (AnnotationInstance annotation : resourceAnnotations) {
                configurations.add(getResourceConfiguration(classInfo, annotation));
            }

            configurations.addAll(processClassResources(classInfo, classAnnotations));
        }

        return configurations;
    }

    public static ResourceInjectionConfiguration getResourceConfiguration(final ClassInfo classInfo, final AnnotationInstance annotation) {
        final AnnotationTarget annotationTarget = annotation.target();
        final ResourceInjectionConfiguration resourceConfiguration;
        if (annotationTarget instanceof FieldInfo) {
            resourceConfiguration = processFieldResource(annotation, FieldInfo.class.cast(annotationTarget));
        } else if (annotationTarget instanceof MethodInfo) {
            resourceConfiguration = processMethodResource(annotation, MethodInfo.class.cast(annotationTarget));
        } else if (annotationTarget instanceof ClassInfo) {
            resourceConfiguration = processClassResource(annotation, classInfo);
        } else {
            resourceConfiguration = null;
        }
        return resourceConfiguration;
    }

    private static ResourceInjectionConfiguration processFieldResource(final AnnotationInstance annotation, final FieldInfo fieldInfo) {
        final String fieldName = fieldInfo.name();
        final AnnotationValue declaredNameValue = annotation.value("name");
        final String declaredName = declaredNameValue != null ? declaredNameValue.asString() : null;
        final String localContextName = declaredName == null || declaredName.isEmpty() ? fieldName : declaredName;

        final AnnotationValue declaredTypeValue = annotation.value("type");
        final DotName declaredType = declaredTypeValue != null ? declaredTypeValue.asClass().name() : null;
        final DotName injectionType = declaredType == null || declaredType.toString().equals(Object.class.getName()) ? fieldInfo.type().name() : declaredType;
        return new ResourceInjectionConfiguration(fieldName, ResourceInjectionConfiguration.TargetType.FIELD, injectionType.toString(), localContextName, getTargetContextName(annotation, fieldName, injectionType.toString()));
    }

    private static ResourceInjectionConfiguration processMethodResource(final AnnotationInstance annotation, final MethodInfo methodInfo) {
        final String methodName = methodInfo.name();
        if (!methodName.startsWith("set") || methodInfo.args().length != 1) {
            throw new IllegalArgumentException("@Resource injection target is invalid.  Only setter methods are allowed: " + methodInfo);
        }

        final String contextNameSuffix = methodName.substring(3, 4).toLowerCase() + methodName.substring(4);
        final AnnotationValue declaredNameValue = annotation.value("name");
        final String declaredName = declaredNameValue != null ? declaredNameValue.asString() : null;
        final String localContextName = declaredName == null || declaredName.isEmpty() ? contextNameSuffix : declaredName;

        final AnnotationValue declaredTypeValue = annotation.value("type");
        final DotName declaredType = declaredTypeValue != null ? declaredTypeValue.asClass().name() : null;
        final DotName injectionType = declaredType == null || declaredType.toString().equals(Object.class.getName()) ? methodInfo.args()[0].name() : declaredType;

        return new ResourceInjectionConfiguration(methodName, ResourceInjectionConfiguration.TargetType.METHOD, injectionType.toString(), localContextName, getTargetContextName(annotation, contextNameSuffix, injectionType.toString()));
    }

    private static ResourceInjectionConfiguration processClassResource(final AnnotationInstance annotation, final ClassInfo classInfo) {
        final AnnotationValue nameValue = annotation.value("name");
        if (nameValue == null || nameValue.asString().isEmpty()) {
            throw new IllegalArgumentException("Class level @Resource annotations must provide a name.");
        }
        final String name = nameValue.asString();

        final AnnotationValue mappedNameValue = annotation.value("mappedName");
        if (mappedNameValue == null || mappedNameValue.asString().isEmpty()) {
            throw new IllegalArgumentException("Class level @Resource annotations must provide a mapped name.");
        }
        final String mappedName = mappedNameValue.asString();

        final AnnotationValue typeValue = annotation.value("type");
        if (typeValue == null || typeValue.asClass().name().toString().equals(Object.class.getName())) {
            throw new IllegalArgumentException("Class level @Resource annotations must provide a type.");
        }
        final String type = typeValue.asClass().name().toString();
        return new ResourceInjectionConfiguration(classInfo.name().toString(), ResourceInjectionConfiguration.TargetType.CLASS, type, name, mappedName);
    }

    private static List<ResourceInjectionConfiguration> processClassResources(final ClassInfo classInfo, final Map<DotName, List<AnnotationInstance>> classAnnotations) {
        final List<AnnotationInstance> resourcesAnnotations = classAnnotations.get(RESOURCES_ANNOTATION_NAME);
        if (resourcesAnnotations == null || resourcesAnnotations.isEmpty()) {
            return Collections.emptyList();
        }

        final AnnotationInstance resourcesInstance = resourcesAnnotations.get(0);
        final AnnotationInstance[] resourceAnnotations = resourcesInstance.value().asNestedArray();

        final List<ResourceInjectionConfiguration> resourceConfigurations = new ArrayList<ResourceInjectionConfiguration>(resourceAnnotations.length);
        for (AnnotationInstance resource : resourceAnnotations) {
            resourceConfigurations.add(processClassResource(resource, classInfo));
        }
        return resourceConfigurations;
    }

    private static String getTargetContextName(final AnnotationInstance resource, final String contextNameSuffix, final String injectionType) {
        final AnnotationValue mappedNameValue = resource.value("mappedName");
        String mappedName = mappedNameValue != null ? mappedNameValue.asString() : null;
        if (mappedName != null && !mappedName.isEmpty()) {
            return mappedName;
        }
        else if (isEnvironmentEntryType(injectionType)) {
            return contextNameSuffix;
        }
        return null;
    }

    private static boolean isEnvironmentEntryType(final String type) {
        return type.equals(String.class.getName())
                || type.equals(Character.class.getName())
                || type.equals(Byte.class.getName())
                || type.equals(Short.class.getName())
                || type.equals(Integer.class.getName())
                || type.equals(Long.class.getName())
                || type.equals(Boolean.class.getName())
                || type.equals(Double.class.getName())
                || type.equals(Float.class.getName());
        // TODO: Add primitive types
    }
}
