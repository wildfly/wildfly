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

package org.jboss.as.server.deployment;

import org.jboss.as.deployment.Phase;
import org.jboss.as.deployment.unit.DeploymentUnitContext;
import org.jboss.as.deployment.unit.DeploymentUnitContextImpl;
import org.jboss.msc.inject.Injector;
import org.jboss.msc.service.LifecycleContext;
import org.jboss.msc.service.MultipleRemoveListener;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceContainer;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;

/**
 * The top-level service corresponding to a deployment unit.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
final class DeploymentUnitService implements Service<DeploymentUnitContext> {
    private static final String FIRST_PHASE_NAME = Phase.values()[0].name();

    private final InjectedValue<DeployerChains> deployerChainsInjector = new InjectedValue<DeployerChains>();
    private final String name;

    private DeploymentUnitContext deploymentUnitContext;

    /**
     * Construct a new instance.
     *
     * @param name the deployment unit simple name
     */
    public DeploymentUnitService(final String name) {
        this.name = name;
    }

    public void start(final StartContext context) throws StartException {
        // Create the first phase deployer
        final ServiceContainer container = context.getController().getServiceContainer();

        deploymentUnitContext = new DeploymentUnitContextImpl(name, null, null);
        final ServiceName serviceName = Services.DEPLOYMENT_BASE.append(name).append(FIRST_PHASE_NAME);
        final DeploymentUnitPhaseService<Object> phaseService = new DeploymentUnitPhaseService<Object>(phase);
        final ServiceBuilder<Object> phaseServiceBuilder = container.addService(serviceName, phaseService);
        // depend on this service
        phaseServiceBuilder.addDependency(Services.DEPLOYMENT_BASE.append(name), DeploymentUnitContext.class, phaseService.getDeploymentUnitContextInjector());

    }

    public void stop(final StopContext context) {
        // Delete the first phase deployer
        final ServiceController<?> controller = context.getController().getServiceContainer().getRequiredService(Services.DEPLOYMENT_BASE.append(name).append(FIRST_PHASE_NAME));
        controller.setMode(ServiceController.Mode.REMOVE);
        final MultipleRemoveListener<LifecycleContext> listener = MultipleRemoveListener.create(context);
        controller.addListener(listener);
        listener.done();
    }

    public DeploymentUnitContext getValue() throws IllegalStateException, IllegalArgumentException {
        return null;
    }

    Injector<DeployerChains> getDeployerChainsInjector() {
        return deployerChainsInjector;
    }
}
