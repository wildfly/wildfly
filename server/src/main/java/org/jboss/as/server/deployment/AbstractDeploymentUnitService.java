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

import java.util.HashSet;
import java.util.Set;

import org.jboss.as.server.ServerLogger;
import org.jboss.msc.inject.Injector;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceRegistry;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.StabilityMonitor;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;

/**
 * Abstract service responsible for managing the life-cycle of a {@link DeploymentUnit}.
 *
 * @author John Bailey
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
public abstract class AbstractDeploymentUnitService implements Service<DeploymentUnit> {

    private static final String FIRST_PHASE_NAME = Phase.values()[0].name();
    private final InjectedValue<DeployerChains> deployerChainsInjector = new InjectedValue<DeployerChains>();

    private DeploymentUnit deploymentUnit;
    private volatile StabilityMonitor monitor;

    protected AbstractDeploymentUnitService() {
    }

    public synchronized void start(final StartContext context) throws StartException {
        ServiceTarget target = context.getChildTarget();
        final String deploymentName = context.getController().getName().getSimpleName();
        monitor = new StabilityMonitor();
        monitor.addController(context.getController());
        // Create the first phase deployer
        deploymentUnit = createAndInitializeDeploymentUnit(context.getController().getServiceContainer());

        final String managementName = deploymentUnit.getAttachment(Attachments.MANAGEMENT_NAME);
        ServerLogger.DEPLOYMENT_LOGGER.startingDeployment(managementName, deploymentName);

        final ServiceName serviceName = deploymentUnit.getServiceName().append(FIRST_PHASE_NAME);
        final Phase firstPhase = Phase.values()[0];
        final DeploymentUnitPhaseService<?> phaseService = DeploymentUnitPhaseService.create(deploymentUnit, firstPhase);
        final ServiceBuilder<?> phaseServiceBuilder = target.addService(serviceName, phaseService);
        phaseServiceBuilder.addDependency(Services.JBOSS_DEPLOYMENT_CHAINS, DeployerChains.class, phaseService.getDeployerChainsInjector());
        phaseServiceBuilder.install();
    }

    /**
     * Template method required for implementations to create and fully initialize a deployment unit instance.  This method
     * should be used to attach any initial deployment unit attachments required for the deployment type.
     *
     * @param registry The service registry
     * @return An initialized DeploymentUnit instance
     */
    protected abstract DeploymentUnit createAndInitializeDeploymentUnit(final ServiceRegistry registry);

    public synchronized void stop(final StopContext context) {
        final String deploymentName = context.getController().getName().getSimpleName();
        final String managementName = deploymentUnit.getAttachment(Attachments.MANAGEMENT_NAME);
        ServerLogger.DEPLOYMENT_LOGGER.stoppedDeployment(managementName, deploymentName, (int) (context.getElapsedTime() / 1000000L));
        deploymentUnit = null;
        monitor.removeController(context.getController());
        monitor = null;
    }

    public synchronized DeploymentUnit getValue() throws IllegalStateException, IllegalArgumentException {
        return deploymentUnit;
    }

    public DeploymentStatus getStatus() {
        final Set<ServiceController<?>> problems = new HashSet<ServiceController<?>>();
        try {
            monitor.awaitStability(problems, problems);
        } catch (final InterruptedException e) {
            // ignore
        }
        return problems.isEmpty() ? DeploymentStatus.OK : DeploymentStatus.FAILED;
    }

    Injector<DeployerChains> getDeployerChainsInjector() {
        return deployerChainsInjector;
    }

    public enum DeploymentStatus {
        NEW,
        OK,
        FAILED,
        STOPPED
    }
}
