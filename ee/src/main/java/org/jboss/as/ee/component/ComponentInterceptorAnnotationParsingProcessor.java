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
import java.util.Map;

/**
 * Deployment processor responsible for analyzing each attached {@link ComponentDescription} instance to configure
 * required method interceptors.
 * <p>
 * This class does not process annotations on interceptors themselves, this is handled by the {@link InterceptorAnnotationParsingProcessor}
 *
 * //TODO: We need to figure out how deployment descriptors are going to override this
 * @author John Bailey
 * @author Stuart Douglas
 */
public class ComponentInterceptorAnnotationParsingProcessor extends AbstractComponentConfigProcessor {
    private static final DotName INTERCEPTORS_ANNOTATION_NAME = DotName.createSimple(Interceptors.class.getName());
    private static final DotName EXCLUDE_DEFAULT_ANNOTATION_NAME = DotName.createSimple(ExcludeDefaultInterceptors.class.getName());
    private static final DotName EXCLUDE_CLASS_ANNOTATION_NAME = DotName.createSimple(ExcludeClassInterceptors.class.getName());

    /**
     * {@inheritDoc} *
     */
    protected void processComponentConfig(final DeploymentUnit deploymentUnit, final DeploymentPhaseContext phaseContext, final CompositeIndex index, final ComponentDescription componentDescription) throws DeploymentUnitProcessingException {
        final ClassInfo classInfo = index.getClassByName(DotName.createSimple(componentDescription.getComponentClassName()));
        if (classInfo == null) {
            return; // We can't continue without the annotation index info.
        }
        processInterceptorConfigs(classInfo,classInfo.name().toString(), index, componentDescription,true);
    }

    private void processInterceptorConfigs(final ClassInfo classInfo, final String actualClassName, final CompositeIndex index, final ComponentDescription componentConfiguration, boolean actualClass) throws DeploymentUnitProcessingException {
        final ClassInfo superClassInfo = index.getClassByName(classInfo.superName());
        final String className = classInfo.name().toString();
        if (superClassInfo != null) {
            processInterceptorConfigs(superClassInfo,actualClassName, index, componentConfiguration,false);
        }

        final Map<DotName, List<AnnotationInstance>> classAnnotations = classInfo.annotations();
        if (classAnnotations == null) {
            return;
        }

        //process the ExcludeDefaultInterceptors annotation
        final List<AnnotationInstance> excludeDefaultAnnotations = classAnnotations.get(EXCLUDE_DEFAULT_ANNOTATION_NAME);
        if(excludeDefaultAnnotations != null ) {
            for(AnnotationInstance annotation : excludeDefaultAnnotations) {
                final AnnotationTarget target = annotation.target();
                if (target instanceof MethodInfo) {
                    final MethodInfo methodInfo = MethodInfo.class.cast(target);
                    final MethodIdentifier methodIdentifier = methodIdentifierFromMethodInfo(methodInfo);
                    componentConfiguration.excludeDefaultInterceptors(methodIdentifier);
                } else {
                    componentConfiguration.setExcludeDefaultInterceptors(true);
                }
            }
        }

        //process the ExcludeClassInterceptors annotation
        final List<AnnotationInstance> excludeClassAnnotations = classAnnotations.get(EXCLUDE_CLASS_ANNOTATION_NAME);
        if(excludeClassAnnotations != null ) {
            for(AnnotationInstance annotation : excludeClassAnnotations) {
                final AnnotationTarget target = annotation.target();
                if (target instanceof MethodInfo) {
                    final MethodInfo methodInfo = MethodInfo.class.cast(target);
                    final MethodIdentifier methodIdentifier = methodIdentifierFromMethodInfo(methodInfo);
                    componentConfiguration.excludeClassInterceptors(methodIdentifier);
                } else {
                    throw new DeploymentUnitProcessingException("ExcludeClassInterceptors not applied to method: " + target);
                }
            }
        }

        final List<AnnotationInstance> interceptorAnnotations = classAnnotations.get(INTERCEPTORS_ANNOTATION_NAME);
        if (interceptorAnnotations == null || interceptorAnnotations.isEmpty()) {
            return;
        }

        for (AnnotationInstance annotationInstance : interceptorAnnotations) {

            final AnnotationValue value = annotationInstance.value();
            if (value != null) for (Type interceptorClass : value.asClassArray()) {
                final ClassInfo interceptorClassInfo = index.getClassByName(interceptorClass.name());
                if (interceptorClassInfo == null) {
                    continue; // TODO: Process without index info
                }

                final AnnotationTarget target = annotationInstance.target();
                if (target instanceof MethodInfo) {
                    final MethodInfo methodInfo = MethodInfo.class.cast(target);
                    componentConfiguration.addMethodInterceptor(methodIdentifierFromMethodInfo(methodInfo), new InterceptorDescription(interceptorClassInfo.name().toString()));
                } else {
                    //we do not process @Interceptors on the superclass
                    if(actualClass) {
                        componentConfiguration.addClassInterceptor(new InterceptorDescription(interceptorClassInfo.name().toString()));
                    }
                }
            }
        }
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
