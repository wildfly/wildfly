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

import org.jboss.as.ee.naming.ContextNames;
import org.jboss.as.ee.naming.RootContextService;
import org.jboss.as.naming.ManagedReferenceFactory;
import org.jboss.as.naming.NamingStore;
import org.jboss.as.naming.service.BinderService;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.modules.Module;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;

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
        // Iterate through each component, installing it into the container
        for (ComponentConfiguration configuration : moduleDescription.getComponentConfigurations()) {
            try {
                deployComponent(phaseContext, configuration);
            }
            catch (RuntimeException e) {
                throw new DeploymentUnitProcessingException("Failed to install component " + configuration, e);
            }
        }
    }

    protected void deployComponent(final DeploymentPhaseContext phaseContext, final ComponentConfiguration configuration) throws DeploymentUnitProcessingException {

        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        final ServiceTarget serviceTarget = phaseContext.getServiceTarget();

        final String applicationName = configuration.getApplicationName();
        final String moduleName = configuration.getModuleName();
        final String componentName = configuration.getComponentName();
        final ServiceName baseName = deploymentUnit.getServiceName().append("component").append(componentName);

        //create additional injectors
        final ServiceName createServiceName = baseName.append("CREATE");
        final ServiceName startServiceName = baseName.append("START");
        final BasicComponentCreateService createService = configuration.getComponentCreateServiceFactory().constructService(configuration);
        final ServiceBuilder<Component> createBuilder = serviceTarget.addService(createServiceName, createService);
        final ComponentStartService startService = new ComponentStartService();
        final ServiceBuilder<Component> startBuilder = serviceTarget.addService(startServiceName, startService);

        // Add all service dependencies
        for (DependencyConfigurator configurator : configuration.getCreateDependencies()) {
            configurator.configureDependency(createBuilder);
        }
        for (DependencyConfigurator configurator : configuration.getStartDependencies()) {
            configurator.configureDependency(startBuilder);
        }

        // START depends on CREATE
        startBuilder.addDependency(createServiceName, BasicComponent.class, startService.getComponentInjector());
        final ServiceName contextServiceName;
        //set up the naming context if nessesary
        if(configuration.getComponentDescription().getNamingMode() == ComponentNamingMode.CREATE) {
            final RootContextService contextService = new RootContextService();
            contextServiceName = ContextNames.contextServiceNameOfComponent(configuration.getApplicationName(), configuration.getModuleName(), configuration.getComponentName());
            serviceTarget.addService(contextServiceName, contextService).install();
        } else {
            contextServiceName = ContextNames.contextServiceNameOfModule(configuration.getApplicationName(), configuration.getModuleName());
        }

        InjectionSource.ResolutionContext resolutionContext = new InjectionSource.ResolutionContext(
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
                final BinderService service = new BinderService(bindingName);
                ServiceBuilder<ManagedReferenceFactory> serviceBuilder = serviceTarget.addService(ContextNames.serviceNameOfContext(applicationName, moduleName, componentName, bindingName), service);
                bindingConfiguration.getSource().getResourceValue(resolutionContext, serviceBuilder, phaseContext, service.getManagedObjectInjector());
                serviceBuilder.addDependency(contextServiceName, NamingStore.class, service.getNamingStoreInjector());
                serviceBuilder.install();
            }
        }


        // The bindings for the component
        for (BindingConfiguration bindingConfiguration : configuration.getBindingConfigurations()) {
            final String bindingName = bindingConfiguration.getName();
            final BinderService service = new BinderService(bindingName);
            ServiceBuilder<ManagedReferenceFactory> serviceBuilder = serviceTarget.addService(ContextNames.serviceNameOfEnvEntry(configuration, bindingName), service);
            bindingConfiguration.getSource().getResourceValue(resolutionContext, serviceBuilder, phaseContext, service.getManagedObjectInjector());
            serviceBuilder.addDependency(contextServiceName, NamingStore.class, service.getNamingStoreInjector());
            serviceBuilder.install();
        }

        //TODO: we need to deal with duplicates
        // The bindings for the component class
        for (BindingConfiguration bindingConfiguration : configuration.getModuleClassConfiguration().getBindingConfigurations()) {
            final String bindingName = bindingConfiguration.getName();
            final BinderService service = new BinderService(bindingName);
            ServiceBuilder<ManagedReferenceFactory> serviceBuilder = serviceTarget.addService(ContextNames.serviceNameOfEnvEntry(configuration, bindingName), service);
            bindingConfiguration.getSource().getResourceValue(resolutionContext, serviceBuilder, phaseContext, service.getManagedObjectInjector());
            serviceBuilder.addDependency(contextServiceName, NamingStore.class, service.getNamingStoreInjector());
            serviceBuilder.install();
        }

        createBuilder.install();
        startBuilder.install();
    }

    public void undeploy(final DeploymentUnit context) {
    }
}
