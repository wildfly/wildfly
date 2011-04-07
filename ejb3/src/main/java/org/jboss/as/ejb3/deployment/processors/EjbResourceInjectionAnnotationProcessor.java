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

package org.jboss.as.ejb3.deployment.processors;

import org.jboss.as.ee.component.AbstractComponentConfigProcessor;
import org.jboss.as.ee.component.AbstractComponentDescription;
import org.jboss.as.ee.component.BindingDescription;
import org.jboss.as.ee.component.InjectionTargetDescription;
import org.jboss.as.ee.component.InterceptorDescription;
import org.jboss.as.ee.component.LazyBindingSourceDescription;
import org.jboss.as.ee.component.LookupBindingSourceDescription;
import org.jboss.as.ee.component.ServiceBindingSourceDescription;
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
import org.jboss.logging.Logger;
import org.jboss.msc.service.ServiceName;

import javax.ejb.EJB;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Deployment processor responsible for processing @EJB annotations within components.  Each @EJB annotation will be registered
 * as an injection binding for the component.
 *
 * @author John Bailey
 */
public class EjbResourceInjectionAnnotationProcessor extends AbstractComponentConfigProcessor {

    private static final Logger logger = Logger.getLogger(EjbResourceInjectionAnnotationProcessor.class);

    private static final DotName EJB_ANNOTATION_NAME = DotName.createSimple(EJB.class.getName());

    protected void processComponentConfig(final DeploymentUnit deploymentUnit, final DeploymentPhaseContext phaseContext, final CompositeIndex index, final AbstractComponentDescription description) throws DeploymentUnitProcessingException {
        final ClassInfo classInfo = index.getClassByName(DotName.createSimple(description.getComponentClassName()));
        if (classInfo == null) {
            return; // We can't continue without the annotation index info.
        }

        description.addAnnotationBindings(getEjbInjectionConfigurations(index, classInfo, deploymentUnit, description));
        final Collection<InterceptorDescription> interceptorConfigurations = description.getAllInterceptors().values();
        for (InterceptorDescription interceptorConfiguration : interceptorConfigurations) {
            final ClassInfo interceptorClassInfo = index.getClassByName(DotName.createSimple(interceptorConfiguration.getInterceptorClassName()));
            if (interceptorClassInfo == null) {
                continue;
            }
            description.addAnnotationBindings(getEjbInjectionConfigurations(index, interceptorClassInfo, deploymentUnit, description));
        }
    }

    private List<BindingDescription> getEjbInjectionConfigurations(final CompositeIndex index, final ClassInfo classInfo, final DeploymentUnit deploymentUnit, final AbstractComponentDescription componentDescription) {
        final List<BindingDescription> configurations = new ArrayList<BindingDescription>();

        final Map<DotName, List<AnnotationInstance>> classAnnotations = classInfo.annotations();
        if (classAnnotations != null) {
            final List<AnnotationInstance> ejbAnnotations = classAnnotations.get(EJB_ANNOTATION_NAME);
            if (ejbAnnotations != null) {
                for (AnnotationInstance annotation : ejbAnnotations) {
                    configurations.add(getEjbInjectionConfiguration(annotation, deploymentUnit,componentDescription));
                }
            }
        }
        // Process the super class for @EJB annotations
        DotName superName = classInfo.superName();
        if (superName != null && !superName.toString().equals(Object.class.getName())) {
            ClassInfo superClass = index.getClassByName(superName);
            if (superClass != null) {
                configurations.addAll(this.getEjbInjectionConfigurations(index, superClass, deploymentUnit,componentDescription));
            }
        }
        return configurations;
    }

    private BindingDescription getEjbInjectionConfiguration(final AnnotationInstance annotation, final DeploymentUnit deploymentUnit, final AbstractComponentDescription componentDescription) {
        final AnnotationTarget annotationTarget = annotation.target();

        final AnnotationValue nameValue = annotation.value("name");
        final String name = nameValue != null ? nameValue.asString() : null;

        final AnnotationValue beanNameValue = annotation.value("beanName");
        final String beanName = beanNameValue != null ? beanNameValue.asString() : null;

        final AnnotationValue beanInterfaceValue = annotation.value("beanInterface");
        final String beanInterface = beanInterfaceValue != null ? beanInterfaceValue.asClass().name().toString() : null;

        final AnnotationValue descriptionValue = annotation.value("description");
        final String description = descriptionValue != null ? descriptionValue.asString() : null;

        final AnnotationValue lookupValue = annotation.value("lookup");
        final String lookup = lookupValue != null ? lookupValue.asString() : null;

        if (!isEmpty(lookup) && !isEmpty(beanName)) {
            logger.debug("Both beanName = " + beanName + " and lookup = " + lookup + " have been specified in @EJB annotation." +
                    " lookup will be given preference");
        }

        final BindingDescription bindingDescription;
        if (annotationTarget instanceof FieldInfo) {
            bindingDescription = processFieldInjection(FieldInfo.class.cast(annotationTarget), name, beanInterface, componentDescription);
        } else if (annotationTarget instanceof MethodInfo) {
            bindingDescription = processMethodInjection(MethodInfo.class.cast(annotationTarget), name, beanInterface, componentDescription);
        } else if (annotationTarget instanceof ClassInfo) {
            bindingDescription = processClassInjection(name, beanInterface, componentDescription);
        } else {
            bindingDescription = null;
        }
        if (bindingDescription != null) {
            if (!isEmpty(description)) {
                bindingDescription.setDescription(description);
            }
            // give preference to "lookup" before "beanName"
            if (!isEmpty(lookup)) {
                bindingDescription.setReferenceSourceDescription(new LookupBindingSourceDescription(lookup,componentDescription));
            } else if (!isEmpty(beanName)) {
                final ServiceName beanServiceName = deploymentUnit.getServiceName()
                        .append("component").append(beanName).append("VIEW").append(bindingDescription.getBindingType());
                bindingDescription.setReferenceSourceDescription(new ServiceBindingSourceDescription(beanServiceName));
            } else {
                bindingDescription.setReferenceSourceDescription(new LazyBindingSourceDescription());
            }
        }
        return bindingDescription;
    }

    private BindingDescription processFieldInjection(final FieldInfo fieldInfo, final String name, final String beanInterface, final AbstractComponentDescription componentDescription) {
        final String fieldName = fieldInfo.name();
        final String injectionType = isEmpty(beanInterface) || beanInterface.equals(Object.class.getName()) ? fieldInfo.type().name().toString() : beanInterface;

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
        targetDescription.setDeclaredValueClassName(fieldInfo.type().name().toString());
        bindingDescription.getInjectionTargetDescriptions().add(targetDescription);
        return bindingDescription;
    }

    private BindingDescription processMethodInjection(final MethodInfo methodInfo, final String name, final String beanInterface, final AbstractComponentDescription componentDescription) {
        final String methodName = methodInfo.name();
        if (!methodName.startsWith("set") || methodInfo.args().length != 1) {
            throw new IllegalArgumentException("@EJB injection target is invalid.  Only setter methods are allowed: " + methodInfo);
        }

        final String contextNameSuffix = methodName.substring(3, 4).toLowerCase() + methodName.substring(4);
        final String localContextName;
        if (isEmpty(name)) {
            localContextName = methodInfo.declaringClass().name().toString() + "/" + contextNameSuffix;
        } else {
            localContextName = name;
        }
        final String injectionType = isEmpty(beanInterface) || beanInterface.equals(Object.class.getName()) ? methodInfo.args()[0].name().toString() : beanInterface;
        final BindingDescription bindingDescription = createBindingDescription(localContextName, injectionType, componentDescription);

        final InjectionTargetDescription targetDescription = new InjectionTargetDescription();
        targetDescription.setName(methodName);
        targetDescription.setClassName(methodInfo.declaringClass().name().toString());
        targetDescription.setType(InjectionTargetDescription.Type.METHOD);
        targetDescription.setDeclaredValueClassName(methodInfo.args()[0].name().toString());
        bindingDescription.getInjectionTargetDescriptions().add(targetDescription);
        return bindingDescription;
    }

    private BindingDescription processClassInjection(final String name, final String beanInterface, final AbstractComponentDescription componentDescription) {
        if (isEmpty(name)) {
            throw new IllegalArgumentException("Class level @EJB annotations must provide a name.");
        }
        if (isEmpty(beanInterface) || beanInterface.equals(Object.class.getName())) {
            throw new IllegalArgumentException("Class level @EJB annotations must provide a 'beanInterface'.");
        }
        return createBindingDescription(name, beanInterface, componentDescription);
    }

    private BindingDescription createBindingDescription(final String name, final String beanInterface,final AbstractComponentDescription componentDescription) {
        final BindingDescription bindingDescription = new BindingDescription(name,componentDescription);
        bindingDescription.setDependency(true);
        bindingDescription.setBindingType(beanInterface);
        return bindingDescription;
    }

    private boolean isEmpty(final String string) {
        return string == null || string.isEmpty();
    }
}
