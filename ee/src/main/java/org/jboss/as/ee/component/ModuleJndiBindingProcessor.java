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

import org.jboss.as.naming.ManagedReferenceFactory;
import org.jboss.as.naming.NamingStore;
import org.jboss.as.naming.deployment.ContextNames;
import org.jboss.as.naming.service.BinderService;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.value.Value;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Processor that sets up JNDI bindings that are owned by the module. It also handles class level jndi bindings
 * that belong to components that do not have their own java:comp namespace, and class level bindings declared in
 * namespaces above java:comp.
 *
 * @author Stuart Douglas
 */
public class ModuleJndiBindingProcessor implements DeploymentUnitProcessor {

    public void deploy(final DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        final EEModuleConfiguration moduleConfiguration = deploymentUnit.getAttachment(Attachments.EE_MODULE_CONFIGURATION);
        if (moduleConfiguration == null) {
            return;
        }

        final Map<ServiceName, BindingConfiguration> existingBindings = new HashMap<ServiceName, BindingConfiguration>();
        final Map<ServiceName, BindingConfiguration> deploymentDescriptorBindings = new HashMap<ServiceName, BindingConfiguration>();

        // bindings
        // Handle duplicates binding from the same source
        // TODO: Should the view configuration just return a Set instead of a List? Or is there a better way to
        // handle these duplicates?
        final Set<BindingConfiguration> bindingConfigurations = new HashSet<BindingConfiguration>(moduleConfiguration.getBindingConfigurations());
        for (BindingConfiguration binding : bindingConfigurations) {

            final ServiceName serviceName = ContextNames.serviceNameOfEnvEntry(moduleConfiguration.getApplicationName(), moduleConfiguration.getModuleName(), null, false, binding.getName());

            final BindingConfiguration existingConfiguration = existingBindings.get(serviceName);
            if (existingConfiguration != null && !existingConfiguration.equals(binding)) {
                throw new DeploymentUnitProcessingException("Multiple module level bindings with the same name at " + binding.getName() + " " + binding + " and " + existingConfiguration);
            }
            existingBindings.put(serviceName, binding);
            deploymentDescriptorBindings.put(serviceName, binding);
            addJndiBinding(moduleConfiguration, binding, phaseContext, serviceName);
        }

        //now we process all component level bindings, for components that do not have their own java:comp namespace.
        // these are bindings that have been added via a deployment descriptor
        for (final ComponentConfiguration componentConfiguration : moduleConfiguration.getComponentConfigurations()) {

            // TODO: Should the view configuration just return a Set instead of a List? Or is there a better way to
            // handle these duplicates?
            final Set<BindingConfiguration> componentLevelBindings = new HashSet(componentConfiguration.getBindingConfigurations());
            for (BindingConfiguration binding : componentLevelBindings) {
                final String bindingName = binding.getName();
                final boolean compBinding = bindingName.startsWith("java:comp") || !bindingName.startsWith("java:");
                if (componentConfiguration.getComponentDescription().getNamingMode() == ComponentNamingMode.CREATE && compBinding) {
                    //components with there own comp context do their own binding
                    continue;
                }
                final ServiceName serviceName = ContextNames.serviceNameOfEnvEntry(moduleConfiguration.getApplicationName(), moduleConfiguration.getModuleName(), null, false, binding.getName());

                final BindingConfiguration existingConfiguration = existingBindings.get(serviceName);
                if (existingConfiguration != null && !existingConfiguration.equals(binding)) {
                    throw new DeploymentUnitProcessingException("Multiple module level bindings with the same name at " + binding.getName() + " " + binding + " and " + existingConfiguration);
                }
                existingBindings.put(serviceName, binding);
                deploymentDescriptorBindings.put(serviceName, binding);
                addJndiBinding(moduleConfiguration, binding, phaseContext, serviceName);
            }
        }

        //now add all class level bindings
        final Set<String> handledClasses = new HashSet<String>();
        for (final ComponentConfiguration componentConfiguration : moduleConfiguration.getComponentConfigurations()) {

            final Set<EEModuleClassConfiguration> classConfigurations = new HashSet<EEModuleClassConfiguration>();
            classConfigurations.add(componentConfiguration.getModuleClassConfiguration());
            for (final InterceptorDescription interceptor : componentConfiguration.getComponentDescription().getAllInterceptors().values()) {
                final EEModuleClassConfiguration interceptorClass = moduleConfiguration.getClassConfiguration(interceptor.getInterceptorClassName());
                if (interceptorClass != null) {
                    classConfigurations.add(interceptorClass);
                }
            }
            for (final EEModuleClassConfiguration classConfiguration : classConfigurations) {
                new ClassDescriptionTraversal(classConfiguration, moduleConfiguration) {

                    @Override
                    protected void handle(final EEModuleClassConfiguration configuration, final EEModuleClassDescription classDescription) throws DeploymentUnitProcessingException {
                        //only process classes once
                        if (handledClasses.contains(classDescription.getClassName())) {
                            return;
                        }
                        handledClasses.add(classDescription.getClassName());
                        // TODO: Should the view configuration just return a Set instead of a List? Or is there a better way to
                        // handle these duplicates?
                        final Set<BindingConfiguration> classLevelBindings = new HashSet(configuration.getBindingConfigurations());
                        for (BindingConfiguration binding : classLevelBindings) {
                            final String bindingName = binding.getName();
                            final boolean compBinding = bindingName.startsWith("java:comp") || !bindingName.startsWith("java:");
                            if (componentConfiguration.getComponentDescription().getNamingMode() == ComponentNamingMode.CREATE && compBinding) {
                                //components with their own comp context do their own binding
                                continue;
                            }
                            final ServiceName serviceName = ContextNames.serviceNameOfEnvEntry(moduleConfiguration.getApplicationName(), moduleConfiguration.getModuleName(), null, false, binding.getName());
                            if (deploymentDescriptorBindings.containsKey(serviceName)) {
                                continue; //this has been overridden by a DD binding
                            }
                            final BindingConfiguration existingConfiguration = existingBindings.get(serviceName);
                            if (existingConfiguration != null && !existingConfiguration.equals(binding)) {
                                throw new DeploymentUnitProcessingException("Bindings with the same name at " + binding.getName() + " " + binding + " and " + existingConfiguration);
                            }
                            existingBindings.put(serviceName, binding);
                            addJndiBinding(moduleConfiguration, binding, phaseContext, serviceName);
                        }
                    }
                }.run();
            }

        }

    }


    protected void addJndiBinding(final EEModuleConfiguration module, final BindingConfiguration bindingConfiguration, final DeploymentPhaseContext phaseContext, ServiceName serviceName) throws DeploymentUnitProcessingException {
        // Gather information about the dependency
        final String bindingName = bindingConfiguration.getName().startsWith("java:") ? bindingConfiguration.getName() : "java:module/env/" + bindingConfiguration.getName();

        Value<ManagedReferenceFactory> resourceValue;

        // Check to see if this entry should actually be bound into JNDI.
        if (bindingName != null) {
            final BinderService service = new BinderService(bindingName);
            if (serviceName == null) {
                throw new IllegalArgumentException("Invalid context name '" + bindingName + "' for binding");
            }
            // The service builder for the binding
            ServiceBuilder<ManagedReferenceFactory> sourceServiceBuilder = phaseContext.getServiceTarget().addService(serviceName, service);
            InjectionSource.ResolutionContext resolutionContext = new InjectionSource.ResolutionContext(
                    true,
                    module.getModuleName(),
                    module.getModuleName(),
                    module.getApplicationName()
            );
            // The resource value is determined by the reference source, which may add a dependency on the original value to the binding
            bindingConfiguration.getSource().getResourceValue(resolutionContext, sourceServiceBuilder, phaseContext, service.getManagedObjectInjector());
            resourceValue = sourceServiceBuilder
                    .addDependency(serviceName.getParent(), NamingStore.class, service.getNamingStoreInjector())
                    .install();
        } else {
            throw new DeploymentUnitProcessingException("Binding name must not be null: " + bindingConfiguration);
        }
    }

    public void undeploy(DeploymentUnit context) {
    }
}
