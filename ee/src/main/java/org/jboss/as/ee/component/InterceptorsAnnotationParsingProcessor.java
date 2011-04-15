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
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.server.deployment.annotation.CompositeIndex;
import org.jboss.invocation.proxy.MethodIdentifier;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.Type;

import javax.interceptor.ExcludeClassInterceptors;
import javax.interceptor.ExcludeDefaultInterceptors;
import javax.interceptor.Interceptors;
import java.util.List;

/**
 * Deployment processor responsible for analyzing the annotation index to find all @Interceptors, @ExcludeDefaultInterceptors and
 *
 * @author John Bailey
 */
public class InterceptorsAnnotationParsingProcessor implements DeploymentUnitProcessor {
    private static final DotName INTERCEPTORS_ANNOTATION_NAME = DotName.createSimple(Interceptors.class.getName());
    private static final DotName EXCLUDE_DEFAULT_ANNOTATION_NAME = DotName.createSimple(ExcludeDefaultInterceptors.class.getName());
    private static final DotName EXCLUDE_CLASS_ANNOTATION_NAME = DotName.createSimple(ExcludeClassInterceptors.class.getName());

    public void deploy(DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        final EEModuleDescription eeModuleDescription = deploymentUnit.getAttachment(Attachments.EE_MODULE_DESCRIPTION);
        final CompositeIndex index = deploymentUnit.getAttachment(org.jboss.as.server.deployment.Attachments.COMPOSITE_ANNOTATION_INDEX);

        final List<AnnotationInstance> interceptors = index.getAnnotations(INTERCEPTORS_ANNOTATION_NAME);
        for (AnnotationInstance annotation : interceptors) {
            processInterceptors(eeModuleDescription, annotation);
        }

        final List<AnnotationInstance> excludeDefaults = index.getAnnotations(EXCLUDE_DEFAULT_ANNOTATION_NAME);
        for (AnnotationInstance annotation : excludeDefaults) {
            processExcludeDefault(eeModuleDescription, annotation);
        }

        final List<AnnotationInstance> excludeClasses = index.getAnnotations(EXCLUDE_CLASS_ANNOTATION_NAME);
        for (AnnotationInstance annotation : excludeClasses) {
            processExcludeClass(eeModuleDescription, annotation);
        }
    }

    private void processInterceptors(final EEModuleDescription eeModuleDescription, final AnnotationInstance annotation) throws DeploymentUnitProcessingException {
        final AnnotationTarget target = annotation.target();
        if (target instanceof MethodInfo) {
            processMethodInterceptor(eeModuleDescription, MethodInfo.class.cast(target), annotation);
        } else if (target instanceof ClassInfo) {
            processClassInterceptor(eeModuleDescription, ClassInfo.class.cast(target), annotation);
        } else {
            throw new DeploymentUnitProcessingException("@Interceptors annotation is only allowed on methods and classes");
        }
    }

    private void processMethodInterceptor(final EEModuleDescription eeModuleDescription, final MethodInfo methodInfo, final AnnotationInstance annotation) {
        final ComponentDescription componentDescription = eeModuleDescription.getComponentByClassName(methodInfo.declaringClass().name().toString());
        if (componentDescription == null) {
            return; // We ignore non-components
        }

        final AnnotationValue value = annotation.value();
        if (value != null) for (Type interceptorClass : value.asClassArray()) {
            componentDescription.addMethodInterceptor(methodIdentifierFromMethodInfo(methodInfo), new InterceptorDescription(interceptorClass.name().toString()));
        }
    }

    private void processClassInterceptor(final EEModuleDescription eeModuleDescription, final ClassInfo classInfo, final AnnotationInstance annotation) {
        final ComponentDescription componentDescription = eeModuleDescription.getComponentByClassName(classInfo.name().toString());
        if (componentDescription == null) {
            return; // We ignore non-components
        }

        final AnnotationValue value = annotation.value();
        if (value != null) for (Type interceptorClass : value.asClassArray()) {
            componentDescription.addClassInterceptor(new InterceptorDescription(interceptorClass.name().toString()));
        }
    }

    private void processExcludeDefault(final EEModuleDescription eeModuleDescription, final AnnotationInstance annotation) throws DeploymentUnitProcessingException {
        final AnnotationTarget target = annotation.target();
        if (target instanceof MethodInfo) {
            processMethodExcludeDefault(eeModuleDescription, MethodInfo.class.cast(target));
        } else if (target instanceof ClassInfo) {
            processClassExcludeDefault(eeModuleDescription, ClassInfo.class.cast(target));
        } else {
            throw new DeploymentUnitProcessingException("@ExcludeDefaultInterceptors annotation is only allowed on methods and classes");
        }
    }

    private void processClassExcludeDefault(EEModuleDescription eeModuleDescription, final ClassInfo classInfo) {
        final ComponentDescription componentDescription = eeModuleDescription.getComponentByClassName(classInfo.name().toString());
        if (componentDescription == null) {
            return; // We ignore non-components
        }
        componentDescription.setExcludeDefaultInterceptors(true);
    }

    private void processMethodExcludeDefault(EEModuleDescription eeModuleDescription, MethodInfo methodInfo) {
        final ComponentDescription componentDescription = eeModuleDescription.getComponentByClassName(methodInfo.declaringClass().name().toString());
        if (componentDescription == null) {
            return; // We ignore non-components
        }
        componentDescription.excludeDefaultInterceptors(methodIdentifierFromMethodInfo(methodInfo));
    }

    private void processExcludeClass(final EEModuleDescription eeModuleDescription, final AnnotationInstance annotation) throws DeploymentUnitProcessingException {
        final AnnotationTarget target = annotation.target();
        if (target instanceof MethodInfo) {
            final MethodInfo methodInfo = MethodInfo.class.cast(target);
            final ComponentDescription componentDescription = eeModuleDescription.getComponentByClassName(methodInfo.declaringClass().name().toString());
            if (componentDescription == null) {
                return; // We ignore non-components
            }
            componentDescription.excludeClassInterceptors(methodIdentifierFromMethodInfo(methodInfo));
        } else {
            throw new DeploymentUnitProcessingException("@ExcludeDefaultInterceptors annotation is only allowed on methods");
        }
    }

    public void undeploy(DeploymentUnit context) {
    }

    private static MethodIdentifier methodIdentifierFromMethodInfo(MethodInfo methodInfo) {
        final String[] argTypes = new String[methodInfo.args().length];
        int i = 0;
        for (Type argType : methodInfo.args()) {
            argTypes[i++] = argType.name().toString();
        }
        return MethodIdentifier.getIdentifier(methodInfo.returnType().name().toString(), methodInfo.name(), argTypes);
    }
}
