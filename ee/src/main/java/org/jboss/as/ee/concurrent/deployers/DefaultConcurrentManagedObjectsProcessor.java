/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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
package org.jboss.as.ee.concurrent.deployers;

import org.jboss.as.ee.component.Attachments;
import org.jboss.as.ee.component.BindingConfiguration;
import org.jboss.as.ee.component.ComponentConfiguration;
import org.jboss.as.ee.component.ComponentConfigurator;
import org.jboss.as.ee.component.ComponentDescription;
import org.jboss.as.ee.component.ComponentNamingMode;
import org.jboss.as.ee.component.EEModuleDescription;
import org.jboss.as.ee.component.ServiceInjectionSource;
import org.jboss.as.ee.component.interceptors.InterceptorOrder;
import org.jboss.as.ee.concurrent.ConcurrentContextInterceptor;
import org.jboss.as.ee.concurrent.handle.ClassLoaderContextHandleFactory;
import org.jboss.as.ee.concurrent.handle.ConcurrentContextHandleFactory;
import org.jboss.as.ee.concurrent.handle.NamingContextHandleFactory;
import org.jboss.as.ee.concurrent.handle.SecurityContextHandleFactory;
import org.jboss.as.ee.concurrent.service.ConcurrentServiceNames;
import org.jboss.as.ee.concurrent.service.ContextServiceService;
import org.jboss.as.ee.concurrent.service.ManagedExecutorServiceService;
import org.jboss.as.ee.concurrent.service.ManagedScheduledExecutorServiceService;
import org.jboss.as.ee.concurrent.service.ManagedThreadFactoryService;
import org.jboss.as.ee.structure.DeploymentType;
import org.jboss.as.ee.structure.DeploymentTypeMarker;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.invocation.ImmediateInterceptorFactory;
import org.jboss.invocation.InterceptorFactory;

import java.util.Collection;
import java.util.List;

/**
 * The {@link DeploymentUnitProcessor} which sets up the default concurrent managed objects for each EE component in the deployment unit.
 *
 * @author Eduardo Martins
 */
public class DefaultConcurrentManagedObjectsProcessor implements DeploymentUnitProcessor {

    @Override
    public void deploy(DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {

        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();

        final EEModuleDescription eeModuleDescription = deploymentUnit.getAttachment(Attachments.EE_MODULE_DESCRIPTION);
        final Collection<ComponentDescription> componentDescriptions = eeModuleDescription.getComponentDescriptions();
        if (componentDescriptions == null || componentDescriptions.isEmpty()) {
            return;
        }

        // install the default concurrent managed object services and the interceptor configurator for each component
        for (ComponentDescription componentDescription : componentDescriptions) {
            if (componentDescription.getNamingMode() == ComponentNamingMode.NONE) {
                // skip components without namespace
                continue;
            }
            installComponentConfigurator(componentDescription);
        }

        // add the jndi bindings
        if(DeploymentTypeMarker.isType(DeploymentType.WAR, deploymentUnit)) {
            addBindingsConfigurations("java:module/", eeModuleDescription.getBindingConfigurations());
        }
        // install one instance of each default concurrent resources for each component
        for (ComponentDescription componentDescription : componentDescriptions) {
            if (componentDescription.getNamingMode() == ComponentNamingMode.CREATE) {
                addBindingsConfigurations("java:comp/", componentDescription.getBindingConfigurations());
            }
        }
    }

    private void installComponentConfigurator(ComponentDescription componentDescription) {

        final ComponentConfigurator componentConfigurator = new ComponentConfigurator() {
            @Override
            public void configure(DeploymentPhaseContext context, ComponentDescription description, final ComponentConfiguration configuration) throws DeploymentUnitProcessingException {
                // add the interceptor which manages the concurrent context
                final ConcurrentContextInterceptor interceptor = new ConcurrentContextInterceptor(configuration);
                final InterceptorFactory interceptorFactory = new ImmediateInterceptorFactory(interceptor);
                configuration.addPostConstructInterceptor(interceptorFactory, InterceptorOrder.ComponentPostConstruct.CONCURRENT_CONTEXT);
                configuration.addPreDestroyInterceptor(interceptorFactory, InterceptorOrder.ComponentPreDestroy.CONCURRENT_CONTEXT);
                if (description.isPassivationApplicable()) {
                    configuration.addPrePassivateInterceptor(interceptorFactory, InterceptorOrder.ComponentPassivation.CONCURRENT_CONTEXT);
                    configuration.addPostActivateInterceptor(interceptorFactory, InterceptorOrder.ComponentPassivation.CONCURRENT_CONTEXT);
                }
                configuration.addComponentInterceptor(interceptorFactory,InterceptorOrder.Component.CONCURRENT_CONTEXT,false);
                // add default context handle factories
                configuration.getAllChainedContextHandleFactory().add(ClassLoaderContextHandleFactory.INSTANCE);
                configuration.getAllChainedContextHandleFactory().add(SecurityContextHandleFactory.INSTANCE);
                configuration.getAllChainedContextHandleFactory().add(new NamingContextHandleFactory(configuration.getNamespaceContextSelector(), context.getDeploymentUnit().getServiceName()));
                configuration.getAllChainedContextHandleFactory().add(ConcurrentContextHandleFactory.INSTANCE);
            }
        };
        componentDescription.getConfigurators().add(componentConfigurator);
    }

    private void addBindingsConfigurations(String bindingNamePrefix, List<BindingConfiguration> bindingConfigurations) {
        bindingConfigurations.add(new BindingConfiguration(bindingNamePrefix + "DefaultContextService", new ServiceInjectionSource(ConcurrentServiceNames.getDefaultContextServiceServiceName(), ContextServiceService.SERVICE_VALUE_TYPE)));
        bindingConfigurations.add(new BindingConfiguration(bindingNamePrefix + "DefaultManagedThreadFactory", new ServiceInjectionSource(ConcurrentServiceNames.getDefaultManagedThreadFactoryServiceName(), ManagedThreadFactoryService.SERVICE_VALUE_TYPE)));
        bindingConfigurations.add(new BindingConfiguration(bindingNamePrefix + "DefaultManagedExecutorService", new ServiceInjectionSource(ConcurrentServiceNames.getDefaultManagedExecutorServiceServiceName(), ManagedExecutorServiceService.SERVICE_VALUE_TYPE)));
        bindingConfigurations.add(new BindingConfiguration(bindingNamePrefix + "DefaultManagedScheduledExecutorService", new ServiceInjectionSource(ConcurrentServiceNames.getDefaultManagedScheduledExecutorServiceServiceName(), ManagedScheduledExecutorServiceService.SERVICE_VALUE_TYPE)));
    }

    @Override
    public void undeploy(DeploymentUnit context) {
    }

}
