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

package org.jboss.as.ee.component;

import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.annotation.CompositeIndex;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.FieldInfo;
import org.jboss.jandex.MethodInfo;

import javax.annotation.Resource;
import javax.annotation.Resources;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Deployment processor responsible for analyzing each attached {@link AbstractComponentDescription} instance to configure
 * required resource injection configurations.
 *
 * @author John Bailey
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public class ResourceInjectionAnnotationParsingProcessor extends AbstractComponentConfigProcessor {
    private static final DotName RESOURCE_ANNOTATION_NAME = DotName.createSimple(Resource.class.getName());
    private static final DotName RESOURCES_ANNOTATION_NAME = DotName.createSimple(Resources.class.getName());
    private static final Map<DotName,String> FIXED_LOCATIONS;

    static {
        final Map<DotName,String> locations = new HashMap<DotName,String>();
        locations.put(DotName.createSimple("javax.transaction.UserTransaction"), "java:comp/UserTransaction");
        locations.put(DotName.createSimple("javax.transaction.TransactionSynchronizationRegistry"), "java:comp/TransactionSynchronizationRegistry");
        locations.put(DotName.createSimple("javax.enterprise.inject.spi.BeanManager"), "java:comp/BeanManager");
        FIXED_LOCATIONS = Collections.unmodifiableMap(locations);
    }


    /** {@inheritDoc} **/
    protected void processComponentConfig(final DeploymentUnit deploymentUnit, final DeploymentPhaseContext phaseContext, final CompositeIndex index, final AbstractComponentDescription description) {
        final ClassInfo classInfo = index.getClassByName(DotName.createSimple(description.getComponentClassName()));
        if(classInfo == null) {
            return; // We can't continue without the annotation index info.
        }
        description.getBindings().addAll(getResourceConfigurations(classInfo));
        final Collection<InterceptorDescription> interceptorConfigurations = description.getAllInterceptors().values();
        for (InterceptorDescription interceptorConfiguration : interceptorConfigurations) {
            final ClassInfo interceptorClassInfo = index.getClassByName(DotName.createSimple(interceptorConfiguration.getInterceptorClassName()));
            if(interceptorClassInfo == null) {
                continue;
            }
            interceptorConfiguration.getBindings().addAll(getResourceConfigurations(interceptorClassInfo));
        }
    }

    private List<BindingDescription> getResourceConfigurations(final ClassInfo classInfo) {
        final List<BindingDescription> configurations = new ArrayList<BindingDescription>();

        final Map<DotName, List<AnnotationInstance>> classAnnotations = classInfo.annotations();
        if (classAnnotations != null) {
            final List<AnnotationInstance> resourceAnnotations = classAnnotations.get(RESOURCE_ANNOTATION_NAME);
            if (resourceAnnotations != null) for (AnnotationInstance annotation : resourceAnnotations) {
                configurations.add(getResourceConfiguration(annotation));
            }
            configurations.addAll(processClassResources(classAnnotations));
        }

        return configurations;
    }

    private BindingDescription getResourceConfiguration(final AnnotationInstance annotation) {
        final AnnotationTarget annotationTarget = annotation.target();
        final BindingDescription resourceConfiguration;
        if (annotationTarget instanceof FieldInfo) {
            resourceConfiguration = processFieldResource(annotation, FieldInfo.class.cast(annotationTarget));
        } else if (annotationTarget instanceof MethodInfo) {
            resourceConfiguration = processMethodResource(annotation, MethodInfo.class.cast(annotationTarget));
        } else if (annotationTarget instanceof ClassInfo) {
            resourceConfiguration = processClassResource(annotation);
        } else {
            resourceConfiguration = null;
        }
        return resourceConfiguration;
    }

    private BindingDescription processFieldResource(final AnnotationInstance annotation, final FieldInfo fieldInfo) {
        final String fieldName = fieldInfo.name();
        final AnnotationValue declaredNameValue = annotation.value("name");
        final String declaredName = declaredNameValue != null ? declaredNameValue.asString() : null;

        final AnnotationValue declaredTypeValue = annotation.value("type");
        final DotName declaredType = declaredTypeValue != null ? declaredTypeValue.asClass().name() : null;
        final DotName injectionType = declaredType == null || declaredType.toString().equals(Object.class.getName()) ? fieldInfo.type().name() : declaredType;

        BindingDescription bindingDescription = new BindingDescription();
        final String localContextName;
        if (declaredName == null || declaredName.isEmpty()) {
            localContextName = fieldInfo.declaringClass().name().toString() + "/" + fieldName;
        } else {
            localContextName = declaredName;
        }
        bindingDescription.setBindingName(localContextName);
        bindingDescription.setDependency(true);
        final String injectionTypeName = injectionType.toString();
        bindingDescription.setBindingType(injectionTypeName);
        final AnnotationValue description = annotation.value("description");
        if (description != null) bindingDescription.setDescription(description.asString());

        if(FIXED_LOCATIONS.containsKey(fieldInfo.type().name())) {
            bindingDescription.setReferenceSourceDescription(new LookupBindingSourceDescription(FIXED_LOCATIONS.get(fieldInfo.type().name())));
        }
        final AnnotationValue lookupValue = annotation.value("lookup");
        if (lookupValue != null) {
            bindingDescription.setReferenceSourceDescription(new LookupBindingSourceDescription(lookupValue.asString()));
        }
        final InjectionTargetDescription targetDescription = new InjectionTargetDescription();
        targetDescription.setName(fieldName);
        targetDescription.setClassName(fieldInfo.declaringClass().name().toString());
        targetDescription.setType(InjectionTargetDescription.Type.FIELD);
        targetDescription.setValueClassName(injectionTypeName);
        bindingDescription.getInjectionTargetDescriptions().add(targetDescription);
        return bindingDescription;
    }

    private BindingDescription processMethodResource(final AnnotationInstance annotation, final MethodInfo methodInfo) {
        final String methodName = methodInfo.name();
        if (!methodName.startsWith("set") || methodInfo.args().length != 1) {
            throw new IllegalArgumentException("@Resource injection target is invalid.  Only setter methods are allowed: " + methodInfo);
        }

        final String contextNameSuffix = methodName.substring(3, 4).toLowerCase() + methodName.substring(4);
        final AnnotationValue declaredNameValue = annotation.value("name");
        final String declaredName = declaredNameValue != null ? declaredNameValue.asString() : null;
        final String localContextName;
        if (declaredName == null || declaredName.isEmpty()) {
            localContextName = methodInfo.declaringClass().name().toString() + "/" + contextNameSuffix;
        } else {
            localContextName = declaredName;
        }
        final AnnotationValue declaredTypeValue = annotation.value("type");
        final DotName declaredType = declaredTypeValue != null ? declaredTypeValue.asClass().name() : null;
        final DotName injectionType = declaredType == null || declaredType.toString().equals(Object.class.getName()) ? methodInfo.args()[0].name() : declaredType;
        final BindingDescription bindingDescription = new BindingDescription();
        bindingDescription.setDependency(true);
        bindingDescription.setBindingName(localContextName);
        final String injectionTypeName = injectionType.toString();
        bindingDescription.setBindingType(injectionTypeName);
        final AnnotationValue description = annotation.value("description");
        if (description != null) bindingDescription.setDescription(description.asString());

        if(FIXED_LOCATIONS.containsKey(methodInfo.args()[0].name())) {
            bindingDescription.setReferenceSourceDescription(new LookupBindingSourceDescription(FIXED_LOCATIONS.get(methodInfo.args()[0].name())));
        }
        //there is nothing in the spec that says the user cannot override 'lookup; for things like UserTransaction
        final AnnotationValue lookupValue = annotation.value("lookup");
        if (lookupValue != null) {
            bindingDescription.setReferenceSourceDescription(new LookupBindingSourceDescription(lookupValue.asString()));
        }
        final InjectionTargetDescription targetDescription = new InjectionTargetDescription();
        targetDescription.setName(methodName);
        targetDescription.setClassName(methodInfo.declaringClass().name().toString());
        targetDescription.setType(InjectionTargetDescription.Type.METHOD);
        targetDescription.setValueClassName(injectionTypeName);
        bindingDescription.getInjectionTargetDescriptions().add(targetDescription);
        return bindingDescription;
    }

    private BindingDescription processClassResource(final AnnotationInstance annotation) {
        final AnnotationValue nameValue = annotation.value("name");
        if (nameValue == null || nameValue.asString().isEmpty()) {
            throw new IllegalArgumentException("Class level @Resource annotations must provide a name.");
        }
        final String name = nameValue.asString();

        final AnnotationValue typeValue = annotation.value("type");
        if (typeValue == null || typeValue.asClass().name().toString().equals(Object.class.getName())) {
            throw new IllegalArgumentException("Class level @Resource annotations must provide a type.");
        }
        final String type = typeValue.asClass().name().toString();
        final BindingDescription bindingDescription = new BindingDescription();
        bindingDescription.setDependency(true);
        bindingDescription.setBindingName(name);
        bindingDescription.setBindingType(type);
        final AnnotationValue description = annotation.value("description");
        if (description != null) bindingDescription.setDescription(description.asString());
        final AnnotationValue lookupValue = annotation.value("lookup");
        if (lookupValue != null) {
            bindingDescription.setReferenceSourceDescription(new LookupBindingSourceDescription(lookupValue.asString()));
        }
        return bindingDescription;
    }

    private List<BindingDescription> processClassResources(final Map<DotName, List<AnnotationInstance>> classAnnotations) {
        final List<AnnotationInstance> resourcesAnnotations = classAnnotations.get(RESOURCES_ANNOTATION_NAME);
        if (resourcesAnnotations == null || resourcesAnnotations.isEmpty()) {
            return Collections.emptyList();
        }

        final AnnotationInstance resourcesInstance = resourcesAnnotations.get(0);
        final AnnotationInstance[] resourceAnnotations = resourcesInstance.value().asNestedArray();

        final List<BindingDescription> resourceConfigurations = new ArrayList<BindingDescription>(resourceAnnotations.length);
        for (AnnotationInstance resource : resourceAnnotations) {
            resourceConfigurations.add(processClassResource(resource));
        }
        return resourceConfigurations;
    }
}
