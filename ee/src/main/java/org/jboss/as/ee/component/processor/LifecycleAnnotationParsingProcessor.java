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

package org.jboss.as.ee.component.processor;

import java.util.List;
import java.util.Map;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import org.jboss.as.ee.component.ComponentConfiguration;
import org.jboss.as.ee.component.liefcycle.ComponentLifecycleConfiguration;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.annotation.CompositeIndex;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.Type;

/**
 * Deployment processor responsible for analyzing each attached {@link org.jboss.as.ee.component.ComponentConfiguration} instance to configure
 * required life-cycle methods.
 *
 * @author John Bailey
 */
public class LifecycleAnnotationParsingProcessor extends AbstractComponentConfigProcessor {
    private static final DotName POST_CONSTRUCT_ANNOTATION = DotName.createSimple(PostConstruct.class.getName());
    private static final DotName PRE_DESTROY_ANNOTATION = DotName.createSimple(PreDestroy.class.getName());

    /** {@inheritDoc} **/
    protected void processComponentConfig(final DeploymentUnit deploymentUnit, final DeploymentPhaseContext phaseContext, final CompositeIndex index, final ComponentConfiguration componentConfiguration) {
        DotName current = DotName.createSimple(componentConfiguration.getBeanClass());
        while (current != null && !Object.class.getName().equals(current.toString())) {
            final ClassInfo classInfo = index.getClassByName(current);
            final ComponentLifecycleConfiguration postConstructMethod = getLifeCycle(classInfo, POST_CONSTRUCT_ANNOTATION);
            if (postConstructMethod != null) {
                componentConfiguration.addPostConstructLifecycle(postConstructMethod);
            }
            final ComponentLifecycleConfiguration preDestroyMethod = getLifeCycle(classInfo, PRE_DESTROY_ANNOTATION);
            if (preDestroyMethod != null) {
                componentConfiguration.addPreDestroyLifecycle(preDestroyMethod);
            }
            current = classInfo.superName();
        }
    }

    public static ComponentLifecycleConfiguration getLifeCycle(final ClassInfo classInfo, final DotName annotationType) {
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
            return new ComponentLifecycleConfiguration(methodInfo.name());
        } else {
            throw new IllegalArgumentException("Invalid number of arguments for method " + methodInfo.name() + " annotated with " + annotationType + " on class " + classInfo.name());
        }
    }
}
