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
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.Type;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.interceptor.InvocationContext;
import java.util.List;
import java.util.Map;

/**
 * Deployment processor responsible for analyzing each attached {@link org.jboss.as.ee.component.AbstractComponentDescription} instance to configure
 * required life-cycle methods.
 *
 * @author John Bailey
 * @author Stuart Douglas
 */
public class LifecycleAnnotationParsingProcessor extends AbstractComponentConfigProcessor {
    private static final DotName POST_CONSTRUCT_ANNOTATION = DotName.createSimple(PostConstruct.class.getName());
    private static final DotName PRE_DESTROY_ANNOTATION = DotName.createSimple(PreDestroy.class.getName());

    /**
     * {@inheritDoc} *
     */
    protected void processComponentConfig(final DeploymentUnit deploymentUnit, final DeploymentPhaseContext phaseContext, final CompositeIndex index, final AbstractComponentDescription componentConfiguration) throws DeploymentUnitProcessingException {
        processClass(index, componentConfiguration, DotName.createSimple(componentConfiguration.getComponentClassName()), componentConfiguration.getComponentClassName(), true);

        for (InterceptorDescription description : componentConfiguration.getClassInterceptors()) {
            processClass(index, description, DotName.createSimple(description.getInterceptorClassName()), description.getInterceptorClassName(), false);
        }
    }

    private void processClass(final CompositeIndex index, final AbstractLifecycleCapableDescription lifecycleCapableDescription, final DotName className, final String actualClassName, boolean declaredOnTargetClass) {
        final ClassInfo classInfo = index.getClassByName(className);
        if (classInfo == null) {
            return;
        }

        final DotName superName = classInfo.superName();
        if (superName != null) {
            processClass(index, lifecycleCapableDescription, superName, actualClassName, declaredOnTargetClass);
        }

        final InterceptorMethodDescription postConstructMethod = getLifeCycle(classInfo, actualClassName, POST_CONSTRUCT_ANNOTATION, declaredOnTargetClass);
        if (postConstructMethod != null) {
            lifecycleCapableDescription.addPostConstructMethod(postConstructMethod);
        }
        final InterceptorMethodDescription preDestroyMethod = getLifeCycle(classInfo, actualClassName, PRE_DESTROY_ANNOTATION, declaredOnTargetClass);
        if (preDestroyMethod != null) {
            lifecycleCapableDescription.addPreDestroyMethod(preDestroyMethod);
        }
    }

    private InterceptorMethodDescription getLifeCycle(final ClassInfo classInfo, final String actualClass, final DotName annotationType, boolean declaredOnTargetClass) {
        if (classInfo == null) {
            return null; // No index info
        }

        // Try to resolve with the help of the annotation index
        final Map<DotName, List<AnnotationInstance>> classAnnotations = classInfo.annotations();

        final List<AnnotationInstance> instances = classAnnotations.get(annotationType);
        if (instances == null || instances.isEmpty()) {
            return null;
        }

        if (instances.size() > 1) {
            throw new IllegalArgumentException("Only one method may be annotated with " + annotationType + " per bean.");
        }

        final AnnotationTarget target = instances.get(0).target();
        if (!(target instanceof MethodInfo)) {
            throw new IllegalArgumentException(annotationType + " is only valid on method targets.");
        }

        final MethodInfo methodInfo = MethodInfo.class.cast(target);
        final Type[] args = methodInfo.args();
        if (declaredOnTargetClass) {
            if (args.length == 0) {
                return InterceptorMethodDescription.create(classInfo.name().toString(), actualClass, methodInfo, declaredOnTargetClass);
            } else {
                throw new IllegalArgumentException("Invalid number of arguments for method " + methodInfo.name() + " annotated with " + annotationType + " on class " + classInfo.name());
            }
        } else {
            if (args.length == 1 && args[0].name().toString().equals(InvocationContext.class.getName())) {
                return InterceptorMethodDescription.create(classInfo.name().toString(), actualClass, methodInfo, declaredOnTargetClass);
            } else {
                throw new IllegalArgumentException("Invalid signature for method " + methodInfo.name() + " annotated with " + annotationType + " on class " + classInfo.name() + ", signature must be void methodName(InvocationContext ctx)");
            }
        }
    }
}
