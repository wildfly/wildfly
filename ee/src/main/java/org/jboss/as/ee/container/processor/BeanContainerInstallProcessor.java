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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.naming.Context;
import javax.naming.LinkRef;
import javax.naming.Reference;
import org.jboss.as.ee.container.BeanContainerConfig;
import org.jboss.as.ee.container.BeanContainerFactory;
import org.jboss.as.ee.container.injection.ResourceInjection;
import org.jboss.as.ee.container.injection.ResourceInjectionConfiguration;
import org.jboss.as.ee.container.injection.ResourceInjectionResolver;
import org.jboss.as.ee.container.interceptor.MethodInterceptorConfiguration;
import org.jboss.as.ee.container.interceptor.MethodInterceptor;
import org.jboss.as.ee.container.interceptor.MethodInterceptorFactory;
import org.jboss.as.ee.container.service.Attachments;
import org.jboss.as.ee.container.service.BeanContainerObjectFactory;
import org.jboss.as.ee.container.service.BeanContainerService;
import org.jboss.as.naming.ServiceReferenceObjectFactory;
import org.jboss.as.naming.deployment.ContextService;
import org.jboss.as.naming.deployment.ResourceBinder;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.msc.inject.Injector;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.value.Values;

/**
 * Deployment processor responsible for converting {@link BeanContainerConfig} instances into {@link org.jboss.as.ee.container.BeanContainer}instances.
 *
 * @author John Bailey
 */
public class BeanContainerInstallProcessor implements DeploymentUnitProcessor {

    /** {@inheritDoc} **/
    public void deploy(DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        final List<BeanContainerConfig> beanContainerConfigs = deploymentUnit.getAttachment(Attachments.BEAN_CONTAINER_CONFIGS);
        if (beanContainerConfigs == null || beanContainerConfigs.isEmpty()) {
            return;
        }

        for (BeanContainerConfig containerConfig : beanContainerConfigs) {
            processContainerConfig(deploymentUnit, phaseContext.getServiceTarget(), containerConfig);
        }
    }

    protected void processContainerConfig(final DeploymentUnit deploymentUnit, final ServiceTarget serviceTarget, final BeanContainerConfig containerConfig) throws DeploymentUnitProcessingException {
        final BeanContainerFactory containerFactory = containerConfig.getContainerFactory();

        final ResourceInjectionResolver resolver = containerFactory.getResourceInjectionResolver();
        final List<ResourceInjection> injections = new ArrayList<ResourceInjection>(containerConfig.getResourceInjectionConfigs().length);
        final Set<ResourceInjectionResolver.ResolverDependency<?>> resourceDependencies = new HashSet<ResourceInjectionResolver.ResolverDependency<?>>();

        for (ResourceInjectionConfiguration resourceConfiguration : containerConfig.getResourceInjectionConfigs()) {
            final ResourceInjectionResolver.ResolverResult result = resolver.resolve(deploymentUnit, containerConfig, resourceConfiguration);
            resourceDependencies.addAll(result.getDependencies());

            injections.add(result.getInjection());
            if (result.shouldBind()) {
                resourceDependencies.add(bindResource(serviceTarget, result));
            }
        }

        final MethodInterceptorFactory methodInterceptorFactory = containerFactory.getMethodInterceptorFactory();
        final List<MethodInterceptor> interceptors = new ArrayList<MethodInterceptor>(containerConfig.getMethodInterceptorConfigs().length);
        for (MethodInterceptorConfiguration interceptorConfiguration : containerConfig.getMethodInterceptorConfigs()) {
            final List<ResourceInjection> interceptorInjections = new ArrayList<ResourceInjection>(interceptorConfiguration.getResourceConfigurations().size());

            for (ResourceInjectionConfiguration resourceConfiguration : interceptorConfiguration.getResourceConfigurations()) {
                final ResourceInjectionResolver.ResolverResult result = resolver.resolve(deploymentUnit, containerConfig, resourceConfiguration);
                resourceDependencies.addAll(result.getDependencies());

                interceptorInjections.add(result.getInjection());
                if (result.shouldBind()) {
                    resourceDependencies.add(bindResource(serviceTarget, result));
                }
            }
            interceptors.add(methodInterceptorFactory.createInterceptor(deploymentUnit, interceptorConfiguration, interceptorInjections));
        }

        final BeanContainerFactory.ConstructedBeanContainer constructedContainer = containerFactory.createBeanContainer(deploymentUnit, containerConfig, injections, interceptors);

        final ServiceName beanEnvContextServiceName = constructedContainer.getEnvContextServiceName().append(containerConfig.getName());
        final ContextService actualBeanContext = new ContextService(containerConfig.getName());
        serviceTarget.addService(beanEnvContextServiceName, actualBeanContext)
                .addDependency(constructedContainer.getEnvContextServiceName(), Context.class, actualBeanContext.getParentContextInjector())
                .install();

        final ServiceName bindContextServiceName = constructedContainer.getBindContextServiceName();
        final Reference containerFactoryReference = ServiceReferenceObjectFactory.createReference(constructedContainer.getContainerServiceName(), BeanContainerObjectFactory.class);
        final ResourceBinder<Reference> factoryBinder = new ResourceBinder<Reference>(constructedContainer.getBindName(), Values.immediateValue(containerFactoryReference));
        final ServiceName referenceBinderName = bindContextServiceName.append(constructedContainer.getBindName());
        serviceTarget.addService(referenceBinderName, factoryBinder)
                .addDependency(bindContextServiceName, Context.class, factoryBinder.getContextInjector())
                .setInitialMode(ServiceController.Mode.ON_DEMAND)
                .install();

        final ServiceBuilder<?> serviceBuilder = serviceTarget.addService(constructedContainer.getContainerServiceName(), new BeanContainerService(constructedContainer.getBeanContainer()))
                .addDependency(referenceBinderName)
                .setInitialMode(ServiceController.Mode.ACTIVE);

        for (ResourceInjectionResolver.ResolverDependency<?> resolverDependency : resourceDependencies) {
            addDependency(serviceBuilder, resolverDependency);
        }
        serviceBuilder.install();
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

    /** {@inheritDoc} **/
    public void undeploy(DeploymentUnit context) {
        // TODO
    }
}
