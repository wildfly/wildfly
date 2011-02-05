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

import java.util.ArrayList;
import java.util.Collection;
import javax.naming.Context;
import javax.naming.Reference;

import java.util.List;
import org.jboss.as.ee.component.Component;
import org.jboss.as.ee.component.ComponentBinding;
import org.jboss.as.ee.component.ComponentConfiguration;
import org.jboss.as.ee.component.ComponentFactory;
import org.jboss.as.ee.component.injection.ResourceInjectionDependency;
import org.jboss.as.ee.component.service.ComponentCreateService;
import org.jboss.as.ee.component.service.ComponentStartService;
import org.jboss.as.naming.deployment.ResourceBinder;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
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

    /**
     * {@inheritDoc}
     */
    protected void processComponentConfig(final DeploymentUnit deploymentUnit, final DeploymentPhaseContext phaseContext, final ComponentConfiguration componentConfiguration) throws DeploymentUnitProcessingException {
        final ServiceTarget serviceTarget = phaseContext.getServiceTarget();

        final ComponentFactory componentFactory = componentConfiguration.getComponentFactory();

        // Create the component
        final ServiceName componentCreateServiceName  = deploymentUnit.getServiceName().append("component").append(componentConfiguration.getName());
        final ServiceName componentStartServiceName  = deploymentUnit.getServiceName().append("component").append(componentConfiguration.getName()).append("START");

        ComponentCreateService createService = new ComponentCreateService(componentFactory, componentConfiguration);
        serviceTarget.addService(componentCreateServiceName, createService)
            .addDependency(deploymentUnit.getServiceName(), DeploymentUnit.class, createService.getDeploymentUnitInjector())
            .addDependency(componentConfiguration.getCompContextServiceName(), Context.class, createService.getCompContextInjector())
            .addDependency(componentConfiguration.getModuleContextServiceName(), Context.class, createService.getModuleContextInjector())
            .addDependency(componentConfiguration.getAppContextServiceName(), Context.class, createService.getAppContextInjector())
            .install();

        // Create required component bindings, each depending on the component create service

        // Create required component bindings
        final List<ServiceName> componentBindingDeps = new ArrayList<ServiceName>();
        final Collection<ComponentBinding> componentBindings = componentFactory.getComponentBindings(deploymentUnit, componentConfiguration,componentStartServiceName);
        if(componentBindings != null) {
            for(ComponentBinding componentBinding : componentBindings) {
                final ResourceBinder<Reference> factoryBinder = new ResourceBinder<Reference>(componentBinding.getBindName(), Values.immediateValue(componentBinding.getReference()));
                final ServiceName referenceBinderName = componentBinding.getContextServiceName().append(componentBinding.getBindName());
                serviceTarget.addService(referenceBinderName, factoryBinder)
                        .addDependency(componentCreateServiceName)
                        .addDependency(componentBinding.getContextServiceName(), Context.class, factoryBinder.getContextInjector())
                        .setInitialMode(ServiceController.Mode.ON_DEMAND)
                        .install();
                componentBindingDeps.add(referenceBinderName);
            }
        }

        // TODO: set up start service so that if a dep is failed or missing, createInstance fails fast
        // Create the component start service, which depends on all injections and the create service
        final ComponentStartService startService = new ComponentStartService();
        final ServiceBuilder<?> serviceBuilder = serviceTarget.addService(componentStartServiceName, startService)
                .addDependencies(componentBindingDeps)
                .addDependency(componentCreateServiceName, Component.class, startService.getComponentInjector())
                .setInitialMode(ServiceController.Mode.ACTIVE);

        // Make sure all the dependencies are in place for the component's resource injections
        final Collection<ResourceInjectionDependency<?>> dependencies = componentConfiguration.getDependencies();
        if (dependencies != null) for (ResourceInjectionDependency<?> dependency : dependencies) {
            addDependency(serviceBuilder, dependency);
        }
        serviceBuilder.install();
    }

    private <T> void addDependency(final ServiceBuilder<?> serviceBuilder, final ResourceInjectionDependency<T> dependency) {
        if (dependency.getInjector() != null) {
            serviceBuilder.addDependency(dependency.getServiceName(), dependency.getInjectorType(), dependency.getInjector());
        } else {
            serviceBuilder.addDependency(dependency.getServiceName());
        }
    }
}
