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

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import javax.interceptor.InvocationContext;
import javax.naming.Context;
import org.jboss.as.ee.component.AbstractComponent;
import org.jboss.as.ee.component.ComponentConfiguration;
import org.jboss.as.ee.component.injection.ResourceInjection;
import org.jboss.as.ee.component.injection.ResourceInjectionConfiguration;
import org.jboss.as.ee.component.injection.ResourceInjectionDependency;
import org.jboss.as.ee.component.interceptor.ComponentInterceptorFactories;
import org.jboss.as.ee.component.interceptor.InjectingInterceptorInstanceFactory;
import org.jboss.as.ee.component.interceptor.MethodInterceptorConfiguration;
import org.jboss.as.ee.component.interceptor.MethodInterceptorFilter;
import org.jboss.as.naming.deployment.NamingLookupValue;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.reflect.ClassReflectionIndex;
import org.jboss.as.server.deployment.reflect.DeploymentReflectionIndex;
import org.jboss.invocation.InterceptorFactory;
import org.jboss.invocation.InterceptorInstanceFactory;
import org.jboss.invocation.Interceptors;
import org.jboss.invocation.MethodInterceptorFactory;
import org.jboss.invocation.SimpleInterceptorInstanceFactory;
import org.jboss.modules.Module;
import org.jboss.msc.service.ServiceName;

/**
 * @author John Bailey
 */
public class InterceptorInstallProcessor extends AbstractComponentConfigProcessor {

    protected void processComponentConfig(final DeploymentUnit deploymentUnit, final DeploymentPhaseContext phaseContext, final ComponentConfiguration componentConfiguration) throws DeploymentUnitProcessingException {
        final Module module = deploymentUnit.getAttachment(org.jboss.as.server.deployment.Attachments.MODULE);
        final ClassLoader classLoader = module.getClassLoader();

        final DeploymentReflectionIndex deploymentReflectionIndex = deploymentUnit.getAttachment(org.jboss.as.server.deployment.Attachments.REFLECTION_INDEX);

        final Class<?> componentClass = componentConfiguration.getComponentClass();

        // Process the component's interceptors
        processInterceptors(componentConfiguration, classLoader, componentClass, deploymentReflectionIndex);

    }

    private void processInterceptors(final ComponentConfiguration componentConfiguration, final ClassLoader classLoader, final Class<?> componentClass, final DeploymentReflectionIndex deploymentReflectionIndex) throws DeploymentUnitProcessingException {
        final ComponentInterceptorFactories interceptorFactories = componentConfiguration.getComponentInterceptorFactories();
        final List<Method> allMethods = new ArrayList<Method>();
        Class<?> current = componentClass;
        while (current != null) {
            final ClassReflectionIndex classReflectionIndex = deploymentReflectionIndex.getClassIndex(current);
            allMethods.addAll(classReflectionIndex.getMethods());
            current = current.getSuperclass();
        }

        for (MethodInterceptorConfiguration interceptorConfiguration : componentConfiguration.getClassInterceptorConfigs()) {
            final MethodInterceptorFilter methodFilter = interceptorConfiguration.getMethodFilter();
            final InterceptorFactory interceptorFactory = createInterceptorFactory(componentConfiguration, classLoader, componentClass, deploymentReflectionIndex, interceptorConfiguration);
            for (Method method : allMethods) {
                if (methodFilter.intercepts(method)) {
                    interceptorFactories.addClassInterceptorFactory(method, interceptorFactory);
                }
            }
        }

        for (MethodInterceptorConfiguration interceptorConfiguration : componentConfiguration.getMethodInterceptorConfigs()) {
            final MethodInterceptorFilter methodFilter = interceptorConfiguration.getMethodFilter();
            final InterceptorFactory interceptorFactory = createInterceptorFactory(componentConfiguration, classLoader, componentClass, deploymentReflectionIndex, interceptorConfiguration);
            for (Method method : allMethods) {
                if (methodFilter.intercepts(method)) {
                    interceptorFactories.addMethodInterceptorFactory(method, interceptorFactory);
                }
            }
        }

        for (MethodInterceptorConfiguration interceptorConfiguration : componentConfiguration.getComponentInterceptorConfigs()) {
            final MethodInterceptorFilter methodFilter = interceptorConfiguration.getMethodFilter();
            final InterceptorFactory interceptorFactory = createInterceptorFactory(componentConfiguration, classLoader, componentClass, deploymentReflectionIndex, interceptorConfiguration);
            for (Method method : allMethods) {
                if (methodFilter.intercepts(method)) {
                    interceptorFactories.addComponentInterceptorFactory(method, interceptorFactory);
                }
            }
        }

        for (Method method : allMethods) {
            interceptorFactories.addComponentInterceptorFactory(method, Interceptors.getInvokingInterceptorFactory());

        }
    }

    private InterceptorFactory createInterceptorFactory(ComponentConfiguration componentConfiguration, ClassLoader classLoader, Class<?> componentClass, DeploymentReflectionIndex deploymentReflectionIndex, MethodInterceptorConfiguration interceptorConfiguration) throws DeploymentUnitProcessingException {
        final Class<?> interceptorClass;
        try {
            interceptorClass = classLoader.loadClass(interceptorConfiguration.getInterceptorClassName());
        } catch (ClassNotFoundException e) {
            throw new DeploymentUnitProcessingException("Failed to load interceptors class " + interceptorConfiguration.getInterceptorClassName(), e);
        }

        final ClassReflectionIndex interceptorReflectionIndex = deploymentReflectionIndex.getClassIndex(interceptorClass);

        final Method interceptorMethod = interceptorReflectionIndex.getMethod(Object.class, interceptorConfiguration.getMethodName(), InvocationContext.class);
        if (interceptorMethod == null) {
            throw new DeploymentUnitProcessingException("Unable to find interceptor method [" + interceptorConfiguration.getMethodName() + "] on interceptor class [" + interceptorClass + "]");
        }

        final InterceptorInstanceFactory interceptorInstanceFactory;
        if (interceptorClass.equals(componentClass)) {
            interceptorInstanceFactory = AbstractComponent.INSTANCE_FACTORY;
        } else {
            final List<ResourceInjection> interceptorInjections = new ArrayList<ResourceInjection>(interceptorConfiguration.getResourceInjectionConfigs().size());
            final ServiceName envContextServiceName = componentConfiguration.getEnvContextServiceName();
            for (ResourceInjectionConfiguration resourceConfiguration : interceptorConfiguration.getResourceInjectionConfigs()) {
                final NamingLookupValue<Object> lookupValue = new NamingLookupValue<Object>(resourceConfiguration.getLocalContextName());
                final ResourceInjection injection = ResourceInjection.Factory.create(resourceConfiguration, interceptorClass, interceptorReflectionIndex, lookupValue);
                if (injection != null) {
                    interceptorInjections.add(injection);
                }
                componentConfiguration.addDependency(new ResourceInjectionDependency<Context>(envContextServiceName, Context.class, lookupValue.getContextInjector()));
            }
            interceptorInstanceFactory = new InjectingInterceptorInstanceFactory(new SimpleInterceptorInstanceFactory(interceptorClass), interceptorInjections);
        }
        return new MethodInterceptorFactory(interceptorInstanceFactory, interceptorMethod);
    }
}
