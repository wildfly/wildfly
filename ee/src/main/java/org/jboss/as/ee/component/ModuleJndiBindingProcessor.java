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
import org.jboss.as.naming.ManagedReferenceFactory;
import org.jboss.as.naming.NamingStore;
import org.jboss.as.naming.service.BinderService;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.value.Value;

/**
 * Processor that sets up JNDI bindings that are owned by the module.
 *
 * @author Stuart Douglas
 */
public class ModuleJndiBindingProcessor implements DeploymentUnitProcessor {

    public void deploy(DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        final EEModuleConfiguration moduleConfiguration = deploymentUnit.getAttachment(Attachments.EE_MODULE_CONFIGURATION);

        // bindings
        for (BindingConfiguration binding : moduleConfiguration.getBindingConfigurations()) {
            addJndiBinding(moduleConfiguration, binding, phaseContext);
        }

    }


    protected void addJndiBinding(final EEModuleConfiguration module, final BindingConfiguration bindingConfiguration, final DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        // Gather information about the dependency
        final String bindingName = bindingConfiguration.getName();

        Value<ManagedReferenceFactory> resourceValue;

        // Check to see if this entry should actually be bound into JNDI.
        if (bindingName != null) {
            // bind into JNDI
            final String serviceBindingName;

            int idx = bindingName.indexOf('/');
            if (idx == -1) {
                serviceBindingName = bindingName;
            } else {
                serviceBindingName = bindingName.substring(idx + 1);
            }
            final BinderService service = new BinderService(serviceBindingName);
            final ServiceName bindingServiceName = ContextNames.serviceNameOfContext(module.getApplicationName(), module.getModuleName(), module.getModuleName(), bindingName);
            if (bindingServiceName == null) {
                throw new IllegalArgumentException("Invalid context name '" + bindingName + "' for binding");
            }
            // The service builder for the binding
            ServiceBuilder<ManagedReferenceFactory> sourceServiceBuilder = phaseContext.getServiceTarget().addService(bindingServiceName, service);
            InjectionSource.ResolutionContext resolutionContext = new InjectionSource.ResolutionContext(
                    true,
                    module.getModuleName(),
                    module.getModuleName(),
                    module.getApplicationName()
            );
            // The resource value is determined by the reference source, which may add a dependency on the original value to the binding
            bindingConfiguration.getSource().getResourceValue(resolutionContext, sourceServiceBuilder, phaseContext, service.getManagedObjectInjector());
            resourceValue = sourceServiceBuilder
                    .addDependency(bindingServiceName.getParent(), NamingStore.class, service.getNamingStoreInjector())
                    .install();
        } else {
            throw new DeploymentUnitProcessingException("Binding name must not be null: " + bindingConfiguration);
        }
    }

    public void undeploy(DeploymentUnit context) {
    }
}
