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

package org.jboss.as.ee.component.deployers;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.jboss.as.ee.component.Attachments;
import org.jboss.as.ee.component.BasicComponent;
import org.jboss.as.ee.component.BasicComponentCreateService;
import org.jboss.as.ee.component.BindingConfiguration;
import org.jboss.as.ee.component.ClassDescriptionTraversal;
import org.jboss.as.ee.component.Component;
import org.jboss.as.ee.component.ComponentConfiguration;
import org.jboss.as.ee.component.ComponentNamingMode;
import org.jboss.as.ee.component.ComponentStartService;
import org.jboss.as.ee.component.DependencyConfigurator;
import org.jboss.as.ee.component.EEApplicationClasses;
import org.jboss.as.ee.component.EEModuleClassDescription;
import org.jboss.as.ee.component.EEModuleConfiguration;
import org.jboss.as.ee.component.InjectionSource;
import org.jboss.as.ee.component.InterceptorDescription;
import org.jboss.as.ee.component.ViewConfiguration;
import org.jboss.as.ee.component.ViewService;
import org.jboss.as.ee.metadata.MetadataCompleteMarker;
import org.jboss.as.naming.ManagedReferenceFactory;
import org.jboss.as.naming.ServiceBasedNamingStore;
import org.jboss.as.naming.deployment.ContextNames;
import org.jboss.as.naming.deployment.JndiNamingDependencyProcessor;
import org.jboss.as.naming.service.BinderService;
import org.jboss.as.naming.service.NamingStoreService;
import org.jboss.as.server.CurrentServiceContainer;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.modules.Module;
import org.jboss.msc.service.CircularDependencyException;
import org.jboss.msc.service.DuplicateServiceException;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;

import static org.jboss.as.ee.EeLogger.ROOT_LOGGER;
import static org.jboss.as.ee.EeMessages.MESSAGES;
import static org.jboss.as.ee.component.Attachments.EE_MODULE_CONFIGURATION;
import static org.jboss.as.server.deployment.Attachments.MODULE;

/**
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public final class ComponentInstallProcessor implements DeploymentUnitProcessor {

    public void deploy(final DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        final Module module = deploymentUnit.getAttachment(MODULE);
        if (module == null) {
            // Nothing to do
            return;
        }
        final EEModuleConfiguration moduleDescription = deploymentUnit.getAttachment(EE_MODULE_CONFIGURATION);

        final Set<ServiceName> dependencies = deploymentUnit.getAttachment(org.jboss.as.server.deployment.Attachments.JNDI_DEPENDENCIES);

        final ServiceName bindingDependencyService = JndiNamingDependencyProcessor.serviceName(deploymentUnit);

        // Iterate through each component, installing it into the container
        for (final ComponentConfiguration configuration : moduleDescription.getComponentConfigurations()) {
            try {
                ROOT_LOGGER.tracef("Installing component %s", configuration.getComponentClass().getName());
                deployComponent(phaseContext, configuration, dependencies, bindingDependencyService);

                //we need to make sure that the web deployment has a dependency on all components it the app, so web components are started
                //when the web subsystem is starting
                //we only add a dependency on components in the same sub deployment, otherwise we get circular dependencies when initialize-in-order is used
                deploymentUnit.addToAttachmentList(org.jboss.as.server.deployment.Attachments.WEB_DEPENDENCIES, configuration.getComponentDescription().getStartServiceName());
            } catch (Exception e) {
                throw MESSAGES.failedToInstallComponent(e, configuration.getComponentName());
            }
        }
    }

    protected void deployComponent(final DeploymentPhaseContext phaseContext, final ComponentConfiguration configuration, final Set<ServiceName> dependencies, final ServiceName bindingDependencyService) throws DeploymentUnitProcessingException {

        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        final ServiceTarget serviceTarget = phaseContext.getServiceTarget();

        final String applicationName = configuration.getApplicationName();
        final String moduleName = configuration.getModuleName();
        final String componentName = configuration.getComponentName();
        final EEApplicationClasses applicationClasses = deploymentUnit.getAttachment(Attachments.EE_APPLICATION_CLASSES_DESCRIPTION);
        final Module module = deploymentUnit.getAttachment(org.jboss.as.server.deployment.Attachments.MODULE);

        //create additional injectors

        final ServiceName createServiceName = configuration.getComponentDescription().getCreateServiceName();
        final ServiceName startServiceName = configuration.getComponentDescription().getStartServiceName();
        final BasicComponentCreateService createService = configuration.getComponentCreateServiceFactory().constructService(configuration);
        final ServiceBuilder<Component> createBuilder = serviceTarget.addService(createServiceName, createService);
        // inject the DU
        createBuilder.addDependency(deploymentUnit.getServiceName(), DeploymentUnit.class, createService.getDeploymentUnitInjector());

        final ComponentStartService startService = new ComponentStartService();
        final ServiceBuilder<Component> startBuilder = serviceTarget.addService(startServiceName, startService);
        final EEModuleConfiguration moduleConfiguration = deploymentUnit.getAttachment(Attachments.EE_MODULE_CONFIGURATION);

        if (moduleConfiguration == null) {
            return;
        }
        // Add all service dependencies
        for (DependencyConfigurator configurator : configuration.getCreateDependencies()) {
            configurator.configureDependency(createBuilder, createService);
        }
        for (DependencyConfigurator configurator : configuration.getStartDependencies()) {
            configurator.configureDependency(startBuilder, startService);
        }

        // START depends on CREATE
        startBuilder.addDependency(createServiceName, BasicComponent.class, startService.getComponentInjector());

        //don't start components until all bindings are up
        startBuilder.addDependency(bindingDependencyService);
        final ServiceName contextServiceName;
        //set up the naming context if nessesary
        if (configuration.getComponentDescription().getNamingMode() == ComponentNamingMode.CREATE) {
            final NamingStoreService contextService = new NamingStoreService();
            contextServiceName = configuration.getComponentDescription().getContextServiceName();
            serviceTarget.addService(contextServiceName, contextService).install();
        } else {
            contextServiceName = configuration.getComponentDescription().getContextServiceName();
        }

        final InjectionSource.ResolutionContext resolutionContext = new InjectionSource.ResolutionContext(
                configuration.getComponentDescription().getNamingMode() == ComponentNamingMode.USE_MODULE,
                configuration.getComponentName(),
                configuration.getModuleName(),
                configuration.getApplicationName()
        );

        // Iterate through each view, creating the services for each
        for (ViewConfiguration viewConfiguration : configuration.getViews()) {
            final ServiceName serviceName = viewConfiguration.getViewServiceName();
            final ViewService viewService = new ViewService(viewConfiguration);
            serviceTarget.addService(serviceName, viewService)
                    .addDependency(createServiceName, Component.class, viewService.getComponentInjector())
                    .install();

            // The bindings for the view
            for (BindingConfiguration bindingConfiguration : viewConfiguration.getBindingConfigurations()) {
                final String bindingName = bindingConfiguration.getName();
                final ContextNames.BindInfo bindInfo = ContextNames.bindInfoFor(applicationName, moduleName, componentName, bindingName);
                final BinderService service = new BinderService(bindInfo.getBindName(), bindingConfiguration.getSource());

                //these bindings should never be merged, if a view binding is duplicated it is an error
                dependencies.add(bindInfo.getBinderServiceName());

                ServiceBuilder<ManagedReferenceFactory> serviceBuilder = serviceTarget.addService(bindInfo.getBinderServiceName(), service);
                bindingConfiguration.getSource().getResourceValue(resolutionContext, serviceBuilder, phaseContext, service.getManagedObjectInjector());
                serviceBuilder.addDependency(bindInfo.getParentContextServiceName(), ServiceBasedNamingStore.class, service.getNamingStoreInjector());
                serviceBuilder.install();
            }
        }

        if (configuration.getComponentDescription().getNamingMode() == ComponentNamingMode.CREATE) {
            // The bindings for the component
            final Set<ServiceName> bound = new HashSet<ServiceName>();
            processBindings(phaseContext, configuration, serviceTarget, contextServiceName, resolutionContext, configuration.getComponentDescription().getBindingConfigurations(), dependencies, bound);

            //class level bindings should be ignored if the deployment is metadata complete
            if (!MetadataCompleteMarker.isMetadataComplete(phaseContext.getDeploymentUnit())) {

                // The bindings for the component class
                new ClassDescriptionTraversal(configuration.getComponentClass(), applicationClasses) {
                    @Override
                    protected void handle(final Class<?> clazz, final EEModuleClassDescription classDescription) throws DeploymentUnitProcessingException {
                        if (classDescription != null) {
                            processBindings(phaseContext, configuration, serviceTarget, contextServiceName, resolutionContext, classDescription.getBindingConfigurations(), dependencies, bound);
                        }
                    }
                }.run();


                for (InterceptorDescription interceptor : configuration.getComponentDescription().getAllInterceptors()) {
                    final Class<?> interceptorClass;
                    try {
                        interceptorClass = module.getClassLoader().loadClass(interceptor.getInterceptorClassName());
                    } catch (ClassNotFoundException e) {
                        throw MESSAGES.cannotLoadInterceptor(e, interceptor.getInterceptorClassName(), configuration.getComponentClass());
                    }
                    if (interceptorClass != null) {
                        new ClassDescriptionTraversal(interceptorClass, applicationClasses) {
                            @Override
                            protected void handle(final Class<?> clazz, final EEModuleClassDescription classDescription) throws DeploymentUnitProcessingException {
                                if (classDescription != null) {
                                    processBindings(phaseContext, configuration, serviceTarget, contextServiceName, resolutionContext, classDescription.getBindingConfigurations(), dependencies, bound);
                                }
                            }
                        }.run();
                    }
                }
            }
        }

        createBuilder.install();
        startBuilder.install();
    }

    private void processBindings(DeploymentPhaseContext phaseContext, ComponentConfiguration configuration, ServiceTarget serviceTarget, ServiceName contextServiceName, InjectionSource.ResolutionContext resolutionContext, List<BindingConfiguration> bindings, final Set<ServiceName> dependencies, final Set<ServiceName> bound) throws DeploymentUnitProcessingException {

        //we only handle java:comp bindings for components that have their own namespace here, the rest are processed by ModuleJndiBindingProcessor
        for (BindingConfiguration bindingConfiguration : bindings) {
            if (bindingConfiguration.getName().startsWith("java:comp") || !bindingConfiguration.getName().startsWith("java:")) {
                final String bindingName = bindingConfiguration.getName().startsWith("java:comp") ? bindingConfiguration.getName() : "java:comp/env/" + bindingConfiguration.getName();
                final ContextNames.BindInfo bindInfo = ContextNames.bindInfoForEnvEntry(configuration.getApplicationName(), configuration.getModuleName(), configuration.getComponentName(), configuration.getComponentDescription().getNamingMode() == ComponentNamingMode.CREATE, bindingName);
                if (bound.contains(bindInfo.getBinderServiceName())) {
                    continue;
                }
                bound.add(bindInfo.getBinderServiceName());
                try {
                    final BinderService service = new BinderService(bindInfo.getBindName(), bindingConfiguration.getSource());
                    dependencies.add(bindInfo.getBinderServiceName());
                    ServiceBuilder<ManagedReferenceFactory> serviceBuilder = serviceTarget.addService(bindInfo.getBinderServiceName(), service);
                    bindingConfiguration.getSource().getResourceValue(resolutionContext, serviceBuilder, phaseContext, service.getManagedObjectInjector());
                    serviceBuilder.addDependency(bindInfo.getParentContextServiceName(), ServiceBasedNamingStore.class, service.getNamingStoreInjector());
                    serviceBuilder.install();
                } catch (DuplicateServiceException e) {
                    ServiceController<ManagedReferenceFactory> registered = (ServiceController<ManagedReferenceFactory>) CurrentServiceContainer.getServiceContainer().getService(bindInfo.getBinderServiceName());
                    if (registered == null)
                        throw e;

                    BinderService service = (BinderService) registered.getService();
                    if (!service.getSource().equals(bindingConfiguration.getSource()))
                        throw MESSAGES.conflictingBinding(bindingName, bindingConfiguration.getSource());
                } catch (CircularDependencyException e) {
                    throw MESSAGES.circularDependency(bindingName);
                }
            }
        }
    }

    public void undeploy(final DeploymentUnit context) {
    }
}
