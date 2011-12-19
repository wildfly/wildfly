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

import java.util.List;

import javax.ejb.EJB;
import javax.ejb.EJBs;

import org.jboss.as.ee.component.Attachments;
import org.jboss.as.ee.component.BindingConfiguration;
import org.jboss.as.ee.component.EEModuleClassDescription;
import org.jboss.as.ee.component.EEModuleDescription;
import org.jboss.as.ee.component.FieldInjectionTarget;
import org.jboss.as.ee.component.InjectionSource;
import org.jboss.as.ee.component.InjectionTarget;
import org.jboss.as.ee.component.LookupInjectionSource;
import org.jboss.as.ee.component.MethodInjectionTarget;
import org.jboss.as.ee.component.ResourceInjectionConfiguration;
import org.jboss.as.ejb3.deployment.EjbDeploymentAttachmentKeys;
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
import org.jboss.logging.Logger;

/**
 * Deployment processor responsible for processing @EJB annotations within components.  Each @EJB annotation will be registered
 * as an injection binding for the component.
 *
 * @author John Bailey
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
public class EjbResourceInjectionAnnotationProcessor implements DeploymentUnitProcessor {
    private static final DotName EJB_ANNOTATION_NAME = DotName.createSimple(EJB.class.getName());
    private static final DotName EJBS_ANNOTATION_NAME = DotName.createSimple(EJBs.class.getName());

    private static final Logger logger = Logger.getLogger(EjbResourceInjectionAnnotationProcessor.class);

    public void deploy(DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();

        final EEModuleDescription moduleDescription = deploymentUnit.getAttachment(Attachments.EE_MODULE_DESCRIPTION);
        final CompositeIndex index = deploymentUnit.getAttachment(org.jboss.as.server.deployment.Attachments.COMPOSITE_ANNOTATION_INDEX);
        final List<AnnotationInstance> resourceAnnotations = index.getAnnotations(EJB_ANNOTATION_NAME);

        for (AnnotationInstance annotation : resourceAnnotations) {
            final AnnotationTarget annotationTarget = annotation.target();
            final EJBResourceWrapper annotationWrapper = new EJBResourceWrapper(annotation);
            if (annotationTarget instanceof FieldInfo) {
                processField(deploymentUnit, annotationWrapper, (FieldInfo) annotationTarget, moduleDescription);
            } else if (annotationTarget instanceof MethodInfo) {
                processMethod(deploymentUnit, annotationWrapper, (MethodInfo) annotationTarget, moduleDescription);
            } else if (annotationTarget instanceof ClassInfo) {
                processClass(deploymentUnit, annotationWrapper, (ClassInfo) annotationTarget, moduleDescription);
            }
        }
        final List<AnnotationInstance> ejbsAnnotations = index.getAnnotations(EJBS_ANNOTATION_NAME);
        for (AnnotationInstance annotation : ejbsAnnotations) {
            final AnnotationTarget annotationTarget = annotation.target();
            if (annotationTarget instanceof ClassInfo) {
                final AnnotationValue annotationValue = annotation.value();
                final AnnotationInstance[] ejbAnnotations = annotationValue.asNestedArray();
                for (AnnotationInstance ejbAnnotation : ejbAnnotations) {
                    final EJBResourceWrapper annotationWrapper = new EJBResourceWrapper(ejbAnnotation);
                    processClass(deploymentUnit, annotationWrapper, (ClassInfo) annotationTarget, moduleDescription);
                }
            } else {
                throw new DeploymentUnitProcessingException("EJBs annotation can only be placed on classes " + annotation.target());
            }
        }
    }

    public void undeploy(DeploymentUnit context) {
    }

    private void processField(final DeploymentUnit deploymentUnit, final EJBResourceWrapper annotation, final FieldInfo fieldInfo, final EEModuleDescription eeModuleDescription) {
        final String fieldName = fieldInfo.name();
        final String fieldType = fieldInfo.type().name().toString();
        final InjectionTarget targetDescription = new FieldInjectionTarget(fieldInfo.declaringClass().name().toString(), fieldName, fieldType);
        final String localContextName = isEmpty(annotation.name()) ? fieldInfo.declaringClass().name().toString() + "/" + fieldInfo.name() : annotation.name();
        final String beanInterfaceType = isEmpty(annotation.beanInterface()) || annotation.beanInterface().equals(Object.class.getName()) ? fieldType : annotation.beanInterface();
        process(deploymentUnit, beanInterfaceType, annotation.beanName(), annotation.lookup(), fieldInfo.declaringClass(), targetDescription, localContextName, eeModuleDescription);
    }

    private void processMethod(final DeploymentUnit deploymentUnit, final EJBResourceWrapper annotation, final MethodInfo methodInfo, final EEModuleDescription eeModuleDescription) {
        final String methodName = methodInfo.name();
        if (!methodName.startsWith("set") || methodInfo.args().length != 1) {
            throw new IllegalArgumentException("@EJB injection target is invalid.  Only setter methods are allowed: " + methodInfo);
        }
        final String methodParamType = methodInfo.args()[0].name().toString();
        final InjectionTarget targetDescription = new MethodInjectionTarget(methodInfo.declaringClass().name().toString(), methodName, methodParamType);

        final String localContextName = isEmpty(annotation.name()) ? methodInfo.declaringClass().name().toString() + "/" + methodName.substring(3, 4).toLowerCase() + methodName.substring(4) : annotation.name();
        final String beanInterfaceType = isEmpty(annotation.beanInterface()) || annotation.beanInterface().equals(Object.class.getName()) ? methodParamType : annotation.beanInterface();
        process(deploymentUnit, beanInterfaceType, annotation.beanName(), annotation.lookup(), methodInfo.declaringClass(), targetDescription, localContextName, eeModuleDescription);
    }

    private void processClass(final DeploymentUnit deploymentUnit, final EJBResourceWrapper annotation, final ClassInfo classInfo, final EEModuleDescription eeModuleDescription) throws DeploymentUnitProcessingException {
        if (isEmpty(annotation.name())) {
            throw new DeploymentUnitProcessingException("@EJB attribute 'name' is required fo class level annotations. Class: " + classInfo.name());
        }
        if (isEmpty(annotation.beanInterface())) {
            throw new DeploymentUnitProcessingException("@EJB attribute 'beanInterface' is required fo class level annotations. Class: " + classInfo.name());
        }
        process(deploymentUnit, annotation.beanInterface(), annotation.beanName(), annotation.lookup(), classInfo, null, annotation.name(), eeModuleDescription);
    }

    private void process(final DeploymentUnit deploymentUnit, final String beanInterface, final String beanName, final String lookup, final ClassInfo classInfo, final InjectionTarget targetDescription, final String localContextName, final EEModuleDescription eeModuleDescription) {

        if (!isEmpty(lookup) && !isEmpty(beanName)) {
            logger.debug("Both beanName = " + beanName + " and lookup = " + lookup + " have been specified in @EJB annotation." +
                    " lookup will be given preference. Class: " + classInfo.name());
        }

        final EEModuleClassDescription classDescription = eeModuleDescription.addOrGetLocalClassDescription(classInfo.name().toString());

        final InjectionSource valueSource;
        EjbInjectionSource ejbInjectionSource = null;
        //give preference to lookup
        if (!isEmpty(lookup)) {
            if (!lookup.startsWith("java:")) {
                valueSource = new EjbLookupInjectionSource(lookup, targetDescription.getDeclaredValueClassName());
            } else {
                valueSource = new LookupInjectionSource(lookup);
            }
        } else if (!isEmpty(beanName)) {
            valueSource = ejbInjectionSource = new EjbInjectionSource(beanName, beanInterface, localContextName, deploymentUnit);
        } else {
            valueSource = ejbInjectionSource = new EjbInjectionSource(beanInterface, localContextName, deploymentUnit);
        }
        if (ejbInjectionSource != null) {
            deploymentUnit.addToAttachmentList(EjbDeploymentAttachmentKeys.EJB_INJECTIONS, ejbInjectionSource);
        }
        // our injection comes from the local lookup, no matter what.
        final ResourceInjectionConfiguration injectionConfiguration = targetDescription != null ?
                new ResourceInjectionConfiguration(targetDescription, new LookupInjectionSource(localContextName)) : null;

        // Create the binding from whence our injection comes.
        final BindingConfiguration bindingConfiguration = new BindingConfiguration(localContextName, valueSource);

        classDescription.getBindingConfigurations().add(bindingConfiguration);
        if (injectionConfiguration != null) {
            classDescription.addResourceInjection(injectionConfiguration);
        }
    }

    private boolean isEmpty(final String string) {
        return string == null || string.isEmpty();
    }

    private class EJBResourceWrapper {
        private final String name;
        private final String beanInterface;
        private final String beanName;
        private final String lookup;
        private final String description;

        private EJBResourceWrapper(final AnnotationInstance annotation) {
            name = stringValueOrNull(annotation, "name");
            beanInterface = classValueOrNull(annotation, "beanInterface");
            beanName = stringValueOrNull(annotation, "beanName");
            String lookupValue = stringValueOrNull(annotation, "lookup");
            // if "lookup" isn't specified, then fallback on "mappedName". We treat "mappedName" the same as "lookup"
            if (isEmpty(lookupValue)) {
                lookupValue = stringValueOrNull(annotation, "mappedName");
            }
            this.lookup = lookupValue;
            description = stringValueOrNull(annotation, "description");
        }

        private String name() {
            return name;
        }

        private String beanInterface() {
            return beanInterface;
        }

        private String beanName() {
            return beanName;
        }

        private String lookup() {
            return lookup;
        }

        private String description() {
            return description;
        }

        private String stringValueOrNull(final AnnotationInstance annotation, final String attribute) {
            final AnnotationValue value = annotation.value(attribute);
            return value != null ? value.asString() : null;
        }

        private String classValueOrNull(final AnnotationInstance annotation, final String attribute) {
            final AnnotationValue value = annotation.value(attribute);
            return value != null ? value.asClass().name().toString() : null;
        }
    }
}
