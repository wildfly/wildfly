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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.interceptor.InvocationContext;
import javax.naming.Context;
import javax.naming.LinkRef;
import javax.naming.Reference;
import org.jboss.as.ee.component.ComponentConfiguration;
import org.jboss.as.ee.component.ComponentFactory;
import org.jboss.as.ee.component.injection.ResourceInjection;
import org.jboss.as.ee.component.injection.ResourceInjectionConfiguration;
import org.jboss.as.ee.component.injection.ResourceInjectionResolver;
import org.jboss.as.ee.component.interceptor.ComponentInstanceInterceptorInstanceFactory;
import org.jboss.as.ee.component.interceptor.InjectingInterceptorInstanceFactory;
import org.jboss.as.ee.component.interceptor.MethodInterceptorFilter;
import org.jboss.as.ee.component.lifecycle.ComponentLifecycle;
import org.jboss.as.ee.component.lifecycle.ComponentLifecycleConfiguration;
import org.jboss.as.ee.component.interceptor.MethodInterceptorConfiguration;
import org.jboss.as.ee.component.lifecycle.ComponentLifecycleMethod;
import org.jboss.as.ee.component.service.ComponentObjectFactory;
import org.jboss.as.ee.component.service.ComponentService;
import org.jboss.as.naming.ServiceReferenceObjectFactory;
import org.jboss.as.naming.deployment.ContextService;
import org.jboss.as.naming.deployment.ResourceBinder;
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
import org.jboss.msc.inject.Injector;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.value.Values;

/**
 * Deployment processor responsible for converting {@link org.jboss.as.ee.component.ComponentConfiguration} instances into {@link org.jboss.as.ee.component.Component}instances.
 *
 * @author John Bailey
 */
public class ComponentInstallProcessor extends AbstractComponentConfigProcessor {

    /** {@inheritDoc} **/
    protected void processComponentConfig(final DeploymentUnit deploymentUnit, final DeploymentPhaseContext phaseContext, final ComponentConfiguration componentConfiguration) throws DeploymentUnitProcessingException {
        final ServiceTarget serviceTarget = phaseContext.getServiceTarget();

        final Module module = deploymentUnit.getAttachment(org.jboss.as.server.deployment.Attachments.MODULE);
        final ClassLoader classLoader = module.getClassLoader();

        final DeploymentReflectionIndex deploymentReflectionIndex = deploymentUnit.getAttachment(org.jboss.as.server.deployment.Attachments.REFLECTION_INDEX);

        final ComponentFactory componentFactory = componentConfiguration.getComponentFactory();
        final String beanName = componentConfiguration.getName();
        final Class<?> beanClass;
        try {
            beanClass = classLoader.loadClass(componentConfiguration.getBeanClass());
        } catch (ClassNotFoundException e) {
            throw new DeploymentUnitProcessingException("Failed to load component class", e);
        }

        final ResourceInjectionResolver resolver = componentFactory.getResourceInjectionResolver();
        final List<ResourceInjection> injections = new ArrayList<ResourceInjection>(componentConfiguration.getResourceInjectionConfigs().size());
        final Set<ResourceInjectionResolver.ResolverDependency<?>> resourceDependencies = new HashSet<ResourceInjectionResolver.ResolverDependency<?>>();

        // Process the component's injections
        processInjections(deploymentUnit, serviceTarget, componentConfiguration, beanName, beanClass, resolver, injections, resourceDependencies);
        // Process the component's PostConstruct methods
        final List<ComponentLifecycle> postConstructLifecycles = processPostConstructs(componentConfiguration, classLoader);
        // Process the component's PreDestroy methods
        final List<ComponentLifecycle> preDestroyLifecycles = processPreDestroys(componentConfiguration, classLoader);
        // Process the component's interceptors
        final Map<Method, InterceptorFactory> methodInterceptorFactories = processInterceptors(deploymentUnit, serviceTarget, componentConfiguration, classLoader, beanName, beanClass, resolver, resourceDependencies, deploymentReflectionIndex);
        // Create the component
        final ComponentFactory.ConstructedComponent constructedComponent = componentFactory.createComponent(deploymentUnit, beanName, beanClass, classLoader, injections, postConstructLifecycles, preDestroyLifecycles, methodInterceptorFactories);

        // Add the required services
        final ServiceName beanEnvContextServiceName = constructedComponent.getEnvContextServiceName().append(beanName);
        final ContextService actualBeanContext = new ContextService(beanName);
        serviceTarget.addService(beanEnvContextServiceName, actualBeanContext)
                .addDependency(constructedComponent.getEnvContextServiceName(), Context.class, actualBeanContext.getParentContextInjector())
                .install();

        final ServiceName bindContextServiceName = constructedComponent.getBindContextServiceName();
        final Reference componentFactoryReference = ServiceReferenceObjectFactory.createReference(constructedComponent.getComponentServiceName(), ComponentObjectFactory.class);
        final ResourceBinder<Reference> factoryBinder = new ResourceBinder<Reference>(constructedComponent.getBindName(), Values.immediateValue(componentFactoryReference));
        final ServiceName referenceBinderName = bindContextServiceName.append(constructedComponent.getBindName());
        serviceTarget.addService(referenceBinderName, factoryBinder)
                .addDependency(bindContextServiceName, Context.class, factoryBinder.getContextInjector())
                .setInitialMode(ServiceController.Mode.ON_DEMAND)
                .install();

        final ComponentService componentService = new ComponentService(constructedComponent.getComponent());
        final ServiceBuilder<?> serviceBuilder = serviceTarget.addService(constructedComponent.getComponentServiceName(), componentService)
                .addDependency(referenceBinderName)
                .addDependency(constructedComponent.getCompContextServiceName(), Context.class, componentService.getCompContextInjector())
                .addDependency(constructedComponent.getModuleContextServiceName(), Context.class, componentService.getModuleContextInjector())
                .addDependency(constructedComponent.getAppContextServiceName(), Context.class, componentService.getAppContextInjector())
                .setInitialMode(ServiceController.Mode.ACTIVE);

        for (ResourceInjectionResolver.ResolverDependency<?> resolverDependency : resourceDependencies) {
            addDependency(serviceBuilder, resolverDependency);
        }
        serviceBuilder.install();
    }

    private void processInjections(DeploymentUnit deploymentUnit, ServiceTarget serviceTarget, ComponentConfiguration componentConfiguration, String beanName, Class<?> beanClass, ResourceInjectionResolver resolver, List<ResourceInjection> injections, Set<ResourceInjectionResolver.ResolverDependency<?>> resourceDependencies) throws DeploymentUnitProcessingException {
        for (ResourceInjectionConfiguration resourceConfiguration : componentConfiguration.getResourceInjectionConfigs()) {
            final ResourceInjectionResolver.ResolverResult result = resolver.resolve(deploymentUnit, beanName, beanClass, resourceConfiguration);
            resourceDependencies.addAll(result.getDependencies());

            if(result.getInjection() != null) {
                injections.add(result.getInjection());
                if (result.shouldBind()) {
                    resourceDependencies.add(bindResource(serviceTarget, result));
                }
            }
        }
    }

    private List<ComponentLifecycle> processPostConstructs(ComponentConfiguration componentConfiguration, ClassLoader classLoader) throws DeploymentUnitProcessingException {
        final List<ComponentLifecycle> postConstructLifecycles = new ArrayList<ComponentLifecycle>(componentConfiguration.getPostConstructLifecycles().size());
        for (ComponentLifecycleConfiguration lifecycleConfiguration : componentConfiguration.getPostConstructLifecycles()) {
            try {
                postConstructLifecycles.add(createLifecycleInterceptor(classLoader, componentConfiguration, lifecycleConfiguration));
            } catch (Exception e) {
                throw new DeploymentUnitProcessingException("Failed to create lifecycle interceptor instance: " + lifecycleConfiguration.getMethodName(), e);
            }
        }
        return postConstructLifecycles;
    }

    private List<ComponentLifecycle> processPreDestroys(ComponentConfiguration componentConfiguration, ClassLoader classLoader) throws DeploymentUnitProcessingException {
        final List<ComponentLifecycle> preDestroyLifecycles = new ArrayList<ComponentLifecycle>(componentConfiguration.getPreDestroyLifecycles().size());
        for (ComponentLifecycleConfiguration lifecycleConfiguration : componentConfiguration.getPreDestroyLifecycles()) {
            try {
                preDestroyLifecycles.add(createLifecycleInterceptor(classLoader, componentConfiguration, lifecycleConfiguration));
            } catch (Exception e) {
                throw new DeploymentUnitProcessingException("Failed to create lifecycle interceptor instance: " + lifecycleConfiguration.getMethodName(), e);
            }
        }
        return preDestroyLifecycles;
    }

    private Map<Method, InterceptorFactory> processInterceptors(DeploymentUnit deploymentUnit, ServiceTarget serviceTarget, ComponentConfiguration componentConfiguration, ClassLoader classLoader, String beanName, Class<?> beanClass, ResourceInjectionResolver resolver, Set<ResourceInjectionResolver.ResolverDependency<?>> resourceDependencies, final DeploymentReflectionIndex deploymentReflectionIndex) throws DeploymentUnitProcessingException {
        final Map<Method, List<InterceptorFactory>> methodInterceptorFactories = new HashMap<Method, List<InterceptorFactory>>();

        final List<Method> allMethods = new ArrayList<Method>();
        Class<?> current = beanClass;
        while(current != null) {
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
            if (interceptorClass.equals(beanClass)) {
                interceptorInstanceFactory = new ComponentInstanceInterceptorInstanceFactory(beanClass);
            } else {
                final List<ResourceInjection> interceptorInjections = new ArrayList<ResourceInjection>(interceptorConfiguration.getResourceInjectionConfigs().size());
                for (ResourceInjectionConfiguration resourceConfiguration : interceptorConfiguration.getResourceInjectionConfigs()) {
                    final ResourceInjectionResolver.ResolverResult result = resolver.resolve(deploymentUnit, beanName, beanClass, resourceConfiguration);
                    resourceDependencies.addAll(result.getDependencies());

                    interceptorInjections.add(result.getInjection());
                    if (result.shouldBind()) {
                        resourceDependencies.add(bindResource(serviceTarget, result));
                    }
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
            if(interceptorFactories != null) {
                interceptorFactories.add(Interceptors.getInvokingInterceptorFactory());
                result.put(method, Interceptors.getChainedInterceptorFactory(interceptorFactories));
            } else {
                result.put(method, Interceptors.getInvokingInterceptorFactory());
            }

        }
        return result;
    }

    private ComponentLifecycle createLifecycleInterceptor(final ClassLoader classLoader, final ComponentConfiguration componentConfiguration, final ComponentLifecycleConfiguration lifecycleConfiguration) throws NoSuchMethodException, ClassNotFoundException {
        final Class<?> interceptorClass = classLoader.loadClass(componentConfiguration.getBeanClass());
        final Method lifecycleMethod = interceptorClass.getMethod(lifecycleConfiguration.getMethodName());
        return new ComponentLifecycleMethod(lifecycleMethod);
    }

    private ResourceInjectionResolver.ResolverDependency<?> bindResource(final ServiceTarget serviceTarget, final ResourceInjectionResolver.ResolverResult resolverResult) throws DeploymentUnitProcessingException {
        final ServiceName binderName = resolverResult.getBindContextName().append(resolverResult.getBindName());

        final LinkRef linkRef = new LinkRef(resolverResult.getBindTargetName());
        final ResourceBinder<LinkRef> resourceBinder = new ResourceBinder<LinkRef>(resolverResult.getBindName(), Values.immediateValue(linkRef));

        serviceTarget.addService(binderName, resourceBinder)
                .addDependency(resolverResult.getBindContextName(), Context.class, resourceBinder.getContextInjector())
                .install();

        return new ResourceInjectionResolver.ResolverDependency<Object>() {
            public ServiceName getServiceName() {
                return binderName;
            }

            public Injector<Object> getInjector() {
                return null;
            }

            public Class<Object> getInjectorType() {
                return null;
            }
        };
    }

    private <T> void addDependency(final ServiceBuilder<?> serviceBuilder, final ResourceInjectionResolver.ResolverDependency<T> dependency) {
        if (dependency.getInjector() != null) {
            serviceBuilder.addDependency(dependency.getServiceName(), dependency.getInjectorType(), dependency.getInjector());
        } else {
            serviceBuilder.addDependency(dependency.getServiceName());
        }
    }
}
