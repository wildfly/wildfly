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
import org.jboss.as.ee.component.FixedInjectionSource;
import org.jboss.as.ee.component.InjectionSource;
import org.jboss.as.ee.component.ViewConfiguration;
import org.jboss.as.ee.component.interceptors.InterceptorOrder;
import org.jboss.as.ee.concurrent.DefaultContextConfiguration;
import org.jboss.as.ee.concurrent.interceptors.ConcurrentContextInterceptor;
import org.jboss.as.ee.concurrent.interceptors.SecurityContextInterceptor;
import org.jboss.as.ee.concurrent.naming.JndiDefaultContextService;
import org.jboss.as.ee.concurrent.naming.JndiDefaultManagedExecutorService;
import org.jboss.as.ee.concurrent.naming.JndiDefaultManagedScheduledExecutorService;
import org.jboss.as.ee.concurrent.naming.JndiDefaultManagedThreadFactory;
import org.jboss.as.ee.concurrent.service.ConcurrentServiceNames;
import org.jboss.as.ee.concurrent.service.ContextServiceImplService;
import org.jboss.as.ee.concurrent.service.ManagedExecutorServiceImplService;
import org.jboss.as.ee.concurrent.service.ManagedScheduledExecutorServiceImplService;
import org.jboss.as.ee.concurrent.service.ManagedThreadFactoryImplService;
import org.jboss.as.naming.ContextListAndJndiViewManagedReferenceFactory;
import org.jboss.as.naming.ImmediateManagedReference;
import org.jboss.as.naming.ManagedReference;
import org.jboss.as.naming.ManagedReferenceFactory;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.invocation.ImmediateInterceptorFactory;
import org.jboss.invocation.InterceptorFactory;
import org.jboss.invocation.Interceptors;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.wildfly.ee.concurrent.ContextConfiguration;
import org.wildfly.ee.concurrent.TaskDecoratorExecutorService;
import org.wildfly.ee.concurrent.TaskDecoratorScheduledExecutorService;

import javax.enterprise.concurrent.ContextService;
import javax.enterprise.concurrent.ManagedExecutorService;
import javax.enterprise.concurrent.ManagedScheduledExecutorService;
import javax.enterprise.concurrent.ManagedThreadFactory;
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
            installServices(phaseContext, componentDescription);
        }

        // add the jndi bindings
        // install one instance of each default concurrent resources for each component
        boolean bindToModuleEnv = false;
        for (ComponentDescription componentDescription : componentDescriptions) {
            switch (componentDescription.getNamingMode()) {
                case NONE:
                    break;
                case USE_MODULE:
                    bindToModuleEnv = true;
                    break;
                case CREATE:
                    addBindingsConfigurations("java:comp/", componentDescription.getBindingConfigurations());
                    break;
            }
        }
        if (bindToModuleEnv) {
            addBindingsConfigurations("java:module/", eeModuleDescription.getBindingConfigurations());
        }

    }

    private void installServices(DeploymentPhaseContext phaseContext, ComponentDescription componentDescription) {

        final String applicationName = componentDescription.getApplicationName();
        final String moduleName = componentDescription.getModuleName();
        final String componentName = componentDescription.getComponentName();

        final ContextConfiguration contextConfiguration = new DefaultContextConfiguration();
        final ServiceTarget serviceTarget = phaseContext.getServiceTarget();

        final ContextServiceImplService contextServiceImplService = new ContextServiceImplService(contextConfiguration);
        final ServiceName contextServiceImplServiceName = ConcurrentServiceNames.getDefaultContextServiceServiceName(applicationName, moduleName, componentName);
        serviceTarget.addService(contextServiceImplServiceName, contextServiceImplService).install();

        final ManagedThreadFactoryImplService managedThreadFactoryService = new ManagedThreadFactoryImplService(contextConfiguration);
        final ServiceName managedThreadFactoryServiceName = ConcurrentServiceNames.getDefaultManagedThreadFactoryServiceName(applicationName, moduleName, componentName);
        serviceTarget.addService(managedThreadFactoryServiceName, managedThreadFactoryService).install();


        final ManagedExecutorServiceImplService managedExecutorServiceImplService = new ManagedExecutorServiceImplService(contextConfiguration);
        final ServiceName managedExecutorServiceImplServiceName = ConcurrentServiceNames.getDefaultManagedExecutorServiceServiceName(applicationName, moduleName, componentName);
        serviceTarget.addService(managedExecutorServiceImplServiceName, managedExecutorServiceImplService)
                .addDependency(ConcurrentServiceNames.getDefaultTaskDecoratorExecutorServiceServiceName(), TaskDecoratorExecutorService.class, managedExecutorServiceImplService.getTaskDecoratorExecutorService())
                .install();

        final ManagedScheduledExecutorServiceImplService managedScheduledExecutorServiceImplService = new ManagedScheduledExecutorServiceImplService(contextConfiguration);
        final ServiceName managedScheduledExecutorServiceImplServiceName = ConcurrentServiceNames.getDefaultManagedScheduledExecutorServiceServiceName(applicationName, moduleName, componentName);
        serviceTarget.addService(managedScheduledExecutorServiceImplServiceName, managedScheduledExecutorServiceImplService)
                .addDependency(ConcurrentServiceNames.getDefaultTaskDecoratorScheduledExecutorServiceServiceName(), TaskDecoratorScheduledExecutorService.class, managedScheduledExecutorServiceImplService.getTaskDecoratorScheduledExecutorService())
                .install();

        final ComponentConfigurator componentConfigurator = new ComponentConfigurator() {
            @Override
            public void configure(DeploymentPhaseContext context, ComponentDescription description, final ComponentConfiguration configuration) throws DeploymentUnitProcessingException {
                // add the interceptor which manages the concurrent context
                final ConcurrentContextInterceptor interceptor = new ConcurrentContextInterceptor(configuration);
                context.addDependency(contextServiceImplServiceName, ContextService.class, interceptor.getDefaultContextService());
                context.addDependency(managedThreadFactoryServiceName, ManagedThreadFactory.class, interceptor.getDefaultManagedThreadFactory());
                context.addDependency(managedExecutorServiceImplServiceName, ManagedExecutorService.class, interceptor.getDefaultManagedExecutorService());
                context.addDependency(managedScheduledExecutorServiceImplServiceName, ManagedScheduledExecutorService.class, interceptor.getDefaultManagedScheduledExecutorService());
                final InterceptorFactory interceptorFactory = new ImmediateInterceptorFactory(interceptor);
                configuration.addPostConstructInterceptor(interceptorFactory, InterceptorOrder.ComponentPostConstruct.CONCURRENT_CONTEXT);
                configuration.addPreDestroyInterceptor(interceptorFactory, InterceptorOrder.ComponentPreDestroy.CONCURRENT_CONTEXT);
                if (description.isPassivationApplicable()) {
                    configuration.addPrePassivateInterceptor(interceptorFactory, InterceptorOrder.ComponentPassivation.CONCURRENT_CONTEXT);
                    configuration.addPostActivateInterceptor(interceptorFactory, InterceptorOrder.ComponentPassivation.CONCURRENT_CONTEXT);
                }
                // add it also to all view configurations, requires that at this point all were already added to the component configuration
                for (ViewConfiguration viewConfiguration : configuration.getViews()) {
                    viewConfiguration.addViewInterceptor(interceptorFactory, InterceptorOrder.View.CONCURRENT_CONTEXT);
                    viewConfiguration.addClientInterceptor(interceptorFactory, InterceptorOrder.Client.CONCURRENT_CONTEXT);
                }
                // add the default interceptors for contextual invocations
                configuration.addDefaultConcurrentContextInterceptor(SecurityContextInterceptor.getFactory(), InterceptorOrder.ConcurrentContext.SECURITY_CONTEXT);
                configuration.addDefaultConcurrentContextInterceptor(interceptorFactory, InterceptorOrder.ConcurrentContext.CONCURRENT_CONTEXT);
                configuration.addDefaultConcurrentContextInterceptor(Interceptors.getInvokingInterceptorFactory(), InterceptorOrder.ConcurrentContext.INVOKING_INTERCEPTOR);
            }
        };
        componentDescription.getConfigurators().add(componentConfigurator);
    }

    private void addBindingsConfigurations(String bindingNamePrefix, List<BindingConfiguration> bindingConfigurations) {
        bindingConfigurations.add(new BindingConfiguration(bindingNamePrefix + "DefaultContextService", getFixedInjectionSource(JndiDefaultContextService.getInstance())));
        bindingConfigurations.add(new BindingConfiguration(bindingNamePrefix + "DefaultManagedThreadFactory", getFixedInjectionSource(JndiDefaultManagedThreadFactory.getInstance())));
        bindingConfigurations.add(new BindingConfiguration(bindingNamePrefix + "DefaultManagedExecutorService", getFixedInjectionSource(JndiDefaultManagedExecutorService.getInstance())));
        bindingConfigurations.add(new BindingConfiguration(bindingNamePrefix + "DefaultManagedScheduledExecutorService", getFixedInjectionSource(JndiDefaultManagedScheduledExecutorService.getInstance())));
    }

    @Override
    public void undeploy(DeploymentUnit context) {
    }

    private InjectionSource getFixedInjectionSource(final Object value) {
        final ManagedReferenceFactory managedReferenceFactory = new ContextListAndJndiViewManagedReferenceFactory() {
            @Override
            public String getInstanceClassName() {
                return value.getClass().getName();
            }

            @Override
            public String getJndiViewInstanceValue() {
                return value.toString();
            }

            @Override
            public ManagedReference getReference() {
                return new ImmediateManagedReference(value);
            }
        };
        return new FixedInjectionSource(managedReferenceFactory, value);
    }

}
