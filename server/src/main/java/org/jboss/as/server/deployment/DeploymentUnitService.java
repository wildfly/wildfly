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

import org.jboss.as.server.deployment.api.ServerDeploymentRepository;
import org.jboss.msc.inject.Injector;
import org.jboss.msc.service.DelegatingServiceRegistry;
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
public final class DeploymentUnitService implements Service<DeploymentUnit> {
    private static final String FIRST_PHASE_NAME = Phase.values()[0].name();

    private final InjectedValue<DeployerChains> deployerChainsInjector = new InjectedValue<DeployerChains>();
    private final InjectedValue<ServerDeploymentRepository> serverDeploymentRepositoryInjector = new InjectedValue<ServerDeploymentRepository>();
    private final String name;
    private final String runtimeName;
    private final byte[] deploymentHash;
    private final DeploymentUnit parent;

    private DeploymentUnit deploymentUnit;

    /**
     * Construct a new instance.
     *
     * @param name the deployment unit simple name
     * @param runtimeName the deployoment runtime name
     * @param deploymentHash the deployment hash
     * @param parent
     */
    public DeploymentUnitService(final String name, final String runtimeName, final byte[] deploymentHash,  final DeploymentUnit parent) {
        this.name = name;
        this.parent = parent;
        this.runtimeName = runtimeName;
        this.deploymentHash = deploymentHash;
    }

    public synchronized void start(final StartContext context) throws StartException {
        // Create the first phase deployer
        final ServiceContainer container = context.getController().getServiceContainer();

        deploymentUnit = new DeploymentUnitImpl(parent, name, new DelegatingServiceRegistry(container));
        deploymentUnit.putAttachment(Attachments.RUNTIME_NAME, runtimeName);
        deploymentUnit.putAttachment(Attachments.DEPLOYMENT_HASH, deploymentHash);

        // Attach the deployment repo
        deploymentUnit.putAttachment(Attachments.SERVER_DEPLOYMENT_REPOSITORY, serverDeploymentRepositoryInjector.getValue());


        final ServiceName serviceName = deploymentUnit.getServiceName().append(FIRST_PHASE_NAME);
        final Phase firstPhase = Phase.values()[0];
        final DeploymentUnitPhaseService<?> phaseService = DeploymentUnitPhaseService.create(firstPhase);
        final ServiceBuilder<?> phaseServiceBuilder = container.addService(serviceName, phaseService);
        // depend on this service
        phaseServiceBuilder.addDependency(deploymentUnit.getServiceName(), DeploymentUnit.class, phaseService.getDeploymentUnitInjector());
        phaseServiceBuilder.addDependency(Services.JBOSS_DEPLOYMENT_CHAINS, DeployerChains.class, phaseService.getDeployerChainsInjector());
        phaseServiceBuilder.install();
    }

    public synchronized void stop(final StopContext context) {
        // Delete the first phase deployer
        final ServiceController<?> controller = context.getController().getServiceContainer().getService(
                deploymentUnit.getServiceName().append(FIRST_PHASE_NAME));
        if (controller != null) {
            controller.setMode(ServiceController.Mode.REMOVE);
            final MultipleRemoveListener<LifecycleContext> listener = MultipleRemoveListener.create(context);
            context.asynchronous();
            controller.addListener(listener);
            listener.done();
        }
    }

    public synchronized DeploymentUnit getValue() throws IllegalStateException, IllegalArgumentException {
        return deploymentUnit;
    }

    Injector<DeployerChains> getDeployerChainsInjector() {
        return deployerChainsInjector;
    }

    Injector<ServerDeploymentRepository> getServerDeploymentRepositoryInjector() {
        return serverDeploymentRepositoryInjector;
    }
}
