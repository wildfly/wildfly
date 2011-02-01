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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.interceptor.InvocationContext;
import org.jboss.as.ee.component.Attachments;
import org.jboss.as.ee.component.ComponentConfiguration;
import org.jboss.as.ee.component.injection.ResourceInjection;
import org.jboss.as.ee.component.injection.ResourceInjectionConfiguration;
import org.jboss.as.ee.component.injection.ResourceInjectionResolver;
import org.jboss.as.ee.component.interceptor.ComponentInstanceInterceptorInstanceFactory;
import org.jboss.as.ee.component.interceptor.ComponentInterceptorFactories;
import org.jboss.as.ee.component.interceptor.InjectingInterceptorInstanceFactory;
import org.jboss.as.ee.component.interceptor.MethodInterceptorConfiguration;
import org.jboss.as.ee.component.interceptor.MethodInterceptorFilter;
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

/**
 * @author John Bailey
 */
public class InterceptorInstallProcessor extends AbstractComponentConfigProcessor {

    protected void processComponentConfig(final DeploymentUnit deploymentUnit, final DeploymentPhaseContext phaseContext, final ComponentConfiguration componentConfiguration) throws DeploymentUnitProcessingException {
        final Module module = deploymentUnit.getAttachment(org.jboss.as.server.deployment.Attachments.MODULE);
        final ClassLoader classLoader = module.getClassLoader();

        final DeploymentReflectionIndex deploymentReflectionIndex = deploymentUnit.getAttachment(org.jboss.as.server.deployment.Attachments.REFLECTION_INDEX);

        final Class<?> componentClass = componentConfiguration.getAttachment(Attachments.COMPONENT_CLASS);

        final ResourceInjectionResolver resolver = componentConfiguration.getAttachment(Attachments.RESOURCE_INJECTION_RESOLVER);

        // Process the component's interceptors
        final Map<Method, InterceptorFactory> methodInterceptorFactories = processInterceptors(deploymentUnit, componentConfiguration, classLoader, componentClass, resolver, deploymentReflectionIndex);

        componentConfiguration.putAttachment(Attachments.COMPONENT_INTERCEPTOR_FACTORIES, new ComponentInterceptorFactories(methodInterceptorFactories));
    }

    private Map<Method, InterceptorFactory> processInterceptors(final DeploymentUnit deploymentUnit, final ComponentConfiguration componentConfiguration, final ClassLoader classLoader, final Class<?> componentClass, final ResourceInjectionResolver resolver, final DeploymentReflectionIndex deploymentReflectionIndex) throws DeploymentUnitProcessingException {
        final Map<Method, List<InterceptorFactory>> methodInterceptorFactories = new HashMap<Method, List<InterceptorFactory>>();

        final List<Method> allMethods = new ArrayList<Method>();
        Class<?> current = componentClass;
        while (current != null) {
            final ClassReflectionIndex classReflectionIndex = deploymentReflectionIndex.getClassIndex(current);
            allMethods.addAll(classReflectionIndex.getMethods());
            current = current.getSuperclass();
        }

        for (MethodInterceptorConfiguration interceptorConfiguration : componentConfiguration.getMethodInterceptorConfigs()) {

            final Class<?> interceptorClass;
            try {
                interceptorClass = classLoader.loadClass(interceptorConfiguration.getInterceptorClassName());
            } catch (ClassNotFoundException e) {
                throw new DeploymentUnitProcessingException("Failed to load interceptors class " + interceptorConfiguration.getInterceptorClassName(), e);
            }

            final Method interceptorMethod;
            try {
                interceptorMethod = interceptorClass.getMethod(interceptorConfiguration.getMethodName(), InvocationContext.class);
            } catch (NoSuchMethodException e) {
                throw new DeploymentUnitProcessingException("Unable to find interceptor method [" + interceptorConfiguration.getMethodName() + "] on interceptor class [" + interceptorClass + "]");
            }

            final MethodInterceptorFilter methodFilter = interceptorConfiguration.getMethodFilter();

            final InterceptorInstanceFactory interceptorInstanceFactory;
            if (interceptorClass.equals(componentClass)) {
                interceptorInstanceFactory = new ComponentInstanceInterceptorInstanceFactory(componentClass);
            } else {
                final List<ResourceInjection> interceptorInjections = new ArrayList<ResourceInjection>(interceptorConfiguration.getResourceInjectionConfigs().size());
                for (ResourceInjectionConfiguration resourceConfiguration : interceptorConfiguration.getResourceInjectionConfigs()) {
                    final ResourceInjectionResolver.ResolverResult result = resolver.resolve(deploymentUnit, componentConfiguration.getName(), componentClass, resourceConfiguration);
                    if(result.getInjection() != null) {
                        interceptorInjections.add(result.getInjection());
                    }
                    componentConfiguration.addToAttachmentList(Attachments.RESOLVED_RESOURCES, result);
                }
                interceptorInstanceFactory = new InjectingInterceptorInstanceFactory(new SimpleInterceptorInstanceFactory(interceptorClass), interceptorInjections);
            }
            final InterceptorFactory interceptorFactory = new MethodInterceptorFactory(interceptorInstanceFactory, interceptorMethod);
            for (Method method : allMethods) {
                if (methodFilter.intercepts(method)) {
                    List<InterceptorFactory> methodFactories = methodInterceptorFactories.get(method);
                    if (methodFactories == null) {
                        methodFactories = new ArrayList<InterceptorFactory>();
                        methodInterceptorFactories.put(method, methodFactories);
                    }
                    methodFactories.add(interceptorFactory);
                }
            }
        }

        final Map<Method, InterceptorFactory> result = new HashMap<Method, InterceptorFactory>();
        for (Method method : allMethods) {
            final List<InterceptorFactory> interceptorFactories = methodInterceptorFactories.get(method);
            if (interceptorFactories != null) {
                interceptorFactories.add(Interceptors.getInvokingInterceptorFactory());
                result.put(method, Interceptors.getChainedInterceptorFactory(interceptorFactories));
            } else {
                result.put(method, Interceptors.getInvokingInterceptorFactory());
            }

        }
        return result;
    }
}
