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

import java.util.ArrayList;
import java.util.List;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.reflect.DeploymentReflectionIndex;
import org.jboss.invocation.MethodInterceptorFactory;
import org.jboss.invocation.SimpleInterceptorInstanceFactory;
import org.jboss.modules.Module;

import javax.interceptor.InvocationContext;
import java.lang.reflect.Method;

/**
 * Utility class for adding creating interceptor configurations.
 *
 * @author Stuart Douglas
 * @author John Bailey
 */
public class LifecycleInterceptorBuilder {

    /**
     * Create a list of {@link LifecycleInterceptorFactory} instances from a list of {@link InterceptorMethodDescription}.
     *
     * @param lifecycleDescriptions The lifecycle descriptions.
     * @param module The deployment module
     * @param deploymentReflectionIndex The deployment reflection index
     * @return the list of factories
     * @throws DeploymentUnitProcessingException If the lifecycle interceptor factories cannot be created
     */
    public static List<LifecycleInterceptorFactory> createLifecycleInterceptors(final List<InterceptorMethodDescription> lifecycleDescriptions, final Module module, final DeploymentReflectionIndex deploymentReflectionIndex) throws DeploymentUnitProcessingException {
        final List<LifecycleInterceptorFactory> lifecycleInterceptors = new ArrayList<LifecycleInterceptorFactory>(lifecycleDescriptions.size());
        final ClassLoader classLoader = module.getClassLoader();

        // we assume that the lifecycle methods are already in the correct order
        for (InterceptorMethodDescription lifecycleConfiguration : lifecycleDescriptions) {
            try {
                if (!lifecycleConfiguration.isDeclaredOnTargetClass()) {
                    lifecycleInterceptors.add(createInterceptorLifecycle(classLoader, lifecycleConfiguration, deploymentReflectionIndex));
                }
            } catch (Exception e) {
                throw new DeploymentUnitProcessingException("Failed to create lifecycle interceptor instance: " + lifecycleConfiguration.getIdentifier().getName(), e);
            }
        }
        return lifecycleInterceptors;
    }

    /**
     * @param descriptions              The lifecycle descriptions
     * @param module                    The module for the deployment unit
     * @param deploymentReflectionIndex The reflection index
     * @return the list of component lifecycles
     * @throws DeploymentUnitProcessingException If the lifecycle methods cannot be created
     */
    public static List<ComponentLifecycle> createLifecycless(final List<InterceptorMethodDescription> descriptions, final Module module, final DeploymentReflectionIndex deploymentReflectionIndex) throws DeploymentUnitProcessingException {
        final List<ComponentLifecycle> lifecycles = new ArrayList<ComponentLifecycle>(descriptions.size());
        final ClassLoader classLoader = module.getClassLoader();

        // we assume that the lifecycle methods are already in the correct order
        for (InterceptorMethodDescription lifecycleConfiguration : descriptions) {
            try {
                if (lifecycleConfiguration.isDeclaredOnTargetClass()) {
                    lifecycles.add(createLifecycle(classLoader, lifecycleConfiguration, deploymentReflectionIndex));
                }
            } catch (Exception e) {
                throw new DeploymentUnitProcessingException("Failed to create lifecycle interceptor instance: " + lifecycleConfiguration.getIdentifier().getName(), e);
            }
        }

        return lifecycles;
    }

    private static ComponentLifecycle createLifecycle(final ClassLoader classLoader, final InterceptorMethodDescription lifecycleConfiguration, DeploymentReflectionIndex deploymentReflectionIndex) throws NoSuchMethodException, ClassNotFoundException {
        final Class<?> interceptorClass = classLoader.loadClass(lifecycleConfiguration.getDeclaringClass());
        //This has to be a no-arg method
        final Method lifecycleMethod = deploymentReflectionIndex.getClassIndex(interceptorClass).getMethod(void.class, lifecycleConfiguration.getIdentifier().getName());
        return new ComponentLifecycleMethod(lifecycleMethod);
    }

    private static LifecycleInterceptorFactory createInterceptorLifecycle(final ClassLoader classLoader, final InterceptorMethodDescription lifecycleConfiguration, final DeploymentReflectionIndex deploymentReflectionIndex) throws NoSuchMethodException, ClassNotFoundException {
        final Class<?> declaringClass = classLoader.loadClass(lifecycleConfiguration.getDeclaringClass());
        final Class<?> instanceClass = classLoader.loadClass(lifecycleConfiguration.getInstanceClass());
        final Method lifecycleMethod = deploymentReflectionIndex.getClassIndex(declaringClass).getMethod(void.class, lifecycleConfiguration.getIdentifier().getName(), InvocationContext.class);
        final MethodInterceptorFactory delegate = new MethodInterceptorFactory(new SimpleInterceptorInstanceFactory(instanceClass), lifecycleMethod);
        return new LifecycleInterceptorFactory(delegate, lifecycleMethod);
    }
}
