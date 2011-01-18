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

import java.util.List;
import java.util.Map;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import org.jboss.as.ee.container.BeanContainerConfiguration;
import org.jboss.as.ee.container.interceptor.LifecycleInterceptorConfiguration;
import org.jboss.as.ee.container.service.Attachments;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.server.deployment.annotation.CompositeIndex;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.Type;

/**
 * Deployment processor responsible for analyzing each attached {@link org.jboss.as.ee.container.BeanContainerConfiguration} instance to configure
 * required life-cycle methods.
 *
 * @author John Bailey
 */
public class LifecycleAnnotationParsingProcessor implements DeploymentUnitProcessor {
    private static final DotName POST_CONSTRUCT_ANNOTATION = DotName.createSimple(PostConstruct.class.getName());
    private static final DotName PRE_DESTROY_ANNOTATION = DotName.createSimple(PreDestroy.class.getName());


    public void deploy(DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        final List<BeanContainerConfiguration> containerConfigs = deploymentUnit.getAttachment(Attachments.BEAN_CONTAINER_CONFIGS);
        if (containerConfigs == null || containerConfigs.isEmpty()) {
            return;
        }

        final CompositeIndex index = deploymentUnit.getAttachment(org.jboss.as.server.deployment.Attachments.COMPOSITE_ANNOTATION_INDEX);
        if (index == null) {
            return;
        }

        for (BeanContainerConfiguration containerConfig : containerConfigs) {
            DotName current = DotName.createSimple(containerConfig.getBeanClass());
            while (current != null && !Object.class.getName().equals(current.toString())) {
                final ClassInfo classInfo = index.getClassByName(current);
                final LifecycleInterceptorConfiguration postConstructMethod = getLifeCycle(classInfo, POST_CONSTRUCT_ANNOTATION);
                if (postConstructMethod != null) {
                    containerConfig.addPostConstructLifecycle(postConstructMethod);
                }
                final LifecycleInterceptorConfiguration preDestroyMethod = getLifeCycle(classInfo, PRE_DESTROY_ANNOTATION);
                if (preDestroyMethod != null) {
                    containerConfig.addPreDestroyLifecycle(preDestroyMethod);
                }
                current = classInfo.superName();
            }
        }
    }

    public void undeploy(DeploymentUnit context) {
    }

    public static LifecycleInterceptorConfiguration getLifeCycle(final ClassInfo classInfo, final DotName annotationType) {
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
        if (args.length == 0) {
            return new LifecycleInterceptorConfiguration(methodInfo.name());
        } else {
            throw new IllegalArgumentException("Invalid number of arguments for method " + methodInfo.name() + " annotated with " + annotationType + " on class " + classInfo.name());
        }
    }
}
