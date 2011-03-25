/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
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
    private static final Map<String,String> FIXED_LOCATIONS;

    static {
        final Map<String,String> locations = new HashMap<String,String>();
        locations.put("javax.transaction.UserTransaction", "java:comp/UserTransaction");
        locations.put("javax.transaction.TransactionSynchronizationRegistry", "java:comp/TransactionSynchronizationRegistry");
        locations.put("javax.enterprise.inject.spi.BeanManager", "java:comp/BeanManager");
        locations.put("javax.validation.Validator", "java:comp/Validator");
        locations.put("javax.validation.ValidationFactory", "java:comp/ValidationFactory");
        locations.put("javax.ejb.EJBContext", "java:comp/EJBContext");
        locations.put("javax.ejb.SessionContext", "java:comp/EJBContext");
        locations.put("org.omg.CORBA.ORB", "java:comp/ORB");
        FIXED_LOCATIONS = Collections.unmodifiableMap(locations);
    }


    /** {@inheritDoc} **/
    protected void processComponentConfig(final DeploymentUnit deploymentUnit, final DeploymentPhaseContext phaseContext, final CompositeIndex index, final AbstractComponentDescription componentDescription) throws DeploymentUnitProcessingException {
        final ClassInfo classInfo = index.getClassByName(DotName.createSimple(componentDescription.getComponentClassName()));
        if(classInfo == null) {
            return; // We can't continue without the annotation index info.
        }

        componentDescription.addAnnotationBindings(getResourceConfigurations(classInfo, componentDescription));
        final Collection<InterceptorDescription> interceptorConfigurations = componentDescription.getAllInterceptors().values();
        for (InterceptorDescription interceptorConfiguration : interceptorConfigurations) {
            final ClassInfo interceptorClassInfo = index.getClassByName(DotName.createSimple(interceptorConfiguration.getInterceptorClassName()));
            if(interceptorClassInfo == null) {
                continue;
            }
            componentDescription.addAnnotationBindings(getResourceConfigurations(interceptorClassInfo, componentDescription));
        }
    }

    private List<BindingDescription> getResourceConfigurations(final ClassInfo classInfo,AbstractComponentDescription componentDescription) {
        final List<BindingDescription> configurations = new ArrayList<BindingDescription>();

        final Map<DotName, List<AnnotationInstance>> classAnnotations = classInfo.annotations();
        if (classAnnotations != null) {
            final List<AnnotationInstance> resourceAnnotations = classAnnotations.get(RESOURCE_ANNOTATION_NAME);
            if (resourceAnnotations != null) for (AnnotationInstance annotation : resourceAnnotations) {
                configurations.add(getResourceConfiguration(annotation,componentDescription));
            }
            configurations.addAll(processClassResources(classAnnotations,componentDescription));
        }

        return configurations;
    }

    private BindingDescription getResourceConfiguration(final AnnotationInstance annotation,AbstractComponentDescription componentDescription) {
        final AnnotationTarget annotationTarget = annotation.target();
        final BindingDescription resourceConfiguration;

        final AnnotationValue nameValue = annotation.value("name");
        final String name = nameValue != null ? nameValue.asString() : null;
        final AnnotationValue typeValue = annotation.value("type");
        final String type = typeValue != null ? typeValue.asClass().name().toString() : null;

        if (annotationTarget instanceof FieldInfo) {
            resourceConfiguration = processFieldResource(FieldInfo.class.cast(annotationTarget), name, type, componentDescription);
        } else if (annotationTarget instanceof MethodInfo) {
            resourceConfiguration = processMethodResource(MethodInfo.class.cast(annotationTarget), name, type, componentDescription);
        } else if (annotationTarget instanceof ClassInfo) {
            resourceConfiguration = processClassResource(name, type, componentDescription);
        } else {
            resourceConfiguration = null;
        }

        if(resourceConfiguration != null) {
            final AnnotationValue description = annotation.value("description");
            if (description != null) {
                resourceConfiguration.setDescription(description.asString());
            }

            final String bindingType = resourceConfiguration.getBindingType();
            final AnnotationValue lookupValue = annotation.value("lookup");
            if (lookupValue != null) {
                resourceConfiguration.setReferenceSourceDescription(new LookupBindingSourceDescription(lookupValue.asString(),componentDescription));
            } else if(FIXED_LOCATIONS.containsKey(bindingType)) {
                resourceConfiguration.setReferenceSourceDescription(new LookupBindingSourceDescription(FIXED_LOCATIONS.get(bindingType),componentDescription));
            } else {
                resourceConfiguration.setReferenceSourceDescription(new LazyBindingSourceDescription());
            }
        }
        return resourceConfiguration;
    }

    private BindingDescription processFieldResource(final FieldInfo fieldInfo, final String name, final String type, final AbstractComponentDescription componentDescription) {
        final String fieldName = fieldInfo.name();
        final String injectionType = isEmpty(type) || type.equals(Object.class.getName()) ? fieldInfo.type().name().toString() : type;
        final String localContextName;
        if (isEmpty(name)) {
            localContextName = fieldInfo.declaringClass().name().toString() + "/" + fieldName;
        } else {
            localContextName = name;
        }
        final BindingDescription bindingDescription = createBindingDescription(localContextName, injectionType, componentDescription);

        final InjectionTargetDescription targetDescription = new InjectionTargetDescription();
        targetDescription.setName(fieldName);
        targetDescription.setClassName(fieldInfo.declaringClass().name().toString());
        targetDescription.setType(InjectionTargetDescription.Type.FIELD);
        targetDescription.setValueClassName(injectionType);
        bindingDescription.getInjectionTargetDescriptions().add(targetDescription);
        return bindingDescription;
    }

    private BindingDescription processMethodResource(final MethodInfo methodInfo, final String name, final String type, final AbstractComponentDescription componentDescription) {
        final String methodName = methodInfo.name();
        if (!methodName.startsWith("set") || methodInfo.args().length != 1) {
            throw new IllegalArgumentException("@Resource injection target is invalid.  Only setter methods are allowed: " + methodInfo);
        }

        final String contextNameSuffix = methodName.substring(3, 4).toLowerCase() + methodName.substring(4);
        final String localContextName;
        if (name == null || name.isEmpty()) {
            localContextName = methodInfo.declaringClass().name().toString() + "/" + contextNameSuffix;
        } else {
            localContextName = name;
        }

        final String injectionType = isEmpty(type) || type.equals(Object.class.getName()) ? methodInfo.args()[0].name().toString() : type;
        final BindingDescription bindingDescription = createBindingDescription(localContextName, injectionType, componentDescription);

        final InjectionTargetDescription targetDescription = new InjectionTargetDescription();
        targetDescription.setName(methodName);
        targetDescription.setClassName(methodInfo.declaringClass().name().toString());
        targetDescription.setType(InjectionTargetDescription.Type.METHOD);
        targetDescription.setValueClassName(injectionType);
        bindingDescription.getInjectionTargetDescriptions().add(targetDescription);
        return bindingDescription;
    }

    private BindingDescription processClassResource(final String name, final String type, final AbstractComponentDescription componentDescription) {
        if (isEmpty(name)) {
            throw new IllegalArgumentException("Class level @Resource annotations must provide a name.");
        }
        if (isEmpty(type)|| type.equals(Object.class.getName())) {
            throw new IllegalArgumentException("Class level @Resource annotations must provide a type.");
        }
        return createBindingDescription(name, type, componentDescription);
    }

    private List<BindingDescription> processClassResources(final Map<DotName, List<AnnotationInstance>> classAnnotations,AbstractComponentDescription abstractComponentDescription) {
        final List<AnnotationInstance> resourcesAnnotations = classAnnotations.get(RESOURCES_ANNOTATION_NAME);
        if (resourcesAnnotations == null || resourcesAnnotations.isEmpty()) {
            return Collections.emptyList();
        }

        final AnnotationInstance resourcesInstance = resourcesAnnotations.get(0);
        final AnnotationInstance[] resourceAnnotations = resourcesInstance.value().asNestedArray();

        final List<BindingDescription> resourceConfigurations = new ArrayList<BindingDescription>(resourceAnnotations.length);
        for (AnnotationInstance resource : resourceAnnotations) {
            resourceConfigurations.add(getResourceConfiguration(resource,abstractComponentDescription));
        }
        return resourceConfigurations;
    }

    private BindingDescription createBindingDescription(final String name, final String beanInterface, final AbstractComponentDescription componentDescription) {
        final BindingDescription bindingDescription = new BindingDescription(name, componentDescription);
        bindingDescription.setDependency(true);
        bindingDescription.setBindingType(beanInterface);
        return bindingDescription;
    }


    private boolean isEmpty(final String string) {
        return string == null || string.isEmpty();
    }
}
