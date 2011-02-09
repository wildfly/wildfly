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

import java.util.concurrent.atomic.AtomicInteger;
import org.jboss.logging.Logger;
import org.jboss.msc.inject.Injector;
import org.jboss.msc.service.AbstractServiceListener;
import org.jboss.msc.service.LifecycleContext;
import org.jboss.msc.service.MultipleRemoveListener;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceRegistry;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;

/**
 * Abstract service responsible for managing the life-cycle of a {@link DeploymentUnit}.
 *
 * @author John Bailey
 */
public abstract class AbstractDeploymentUnitService implements Service<DeploymentUnit> {

    private static final Logger log = Logger.getLogger("org.jboss.as.server.deployment");
    private static final String FIRST_PHASE_NAME = Phase.values()[0].name();
    private final InjectedValue<DeployerChains> deployerChainsInjector = new InjectedValue<DeployerChains>();
    private DeploymentUnit deploymentUnit;

    public synchronized void start(final StartContext context) throws StartException {
        ServiceTarget target = context.getChildTarget();
        final String deploymentName = context.getController().getName().getSimpleName();
        final DeploymentServiceListener listener = new DeploymentServiceListener(System.nanoTime(), target, deploymentName);
        log.infof("Starting deployment of \"%s\"", deploymentName);
        // Create the first phase deployer
        target.addListener(listener);
        deploymentUnit = createAndInitializeDeploymentUnit(context.getController().getServiceContainer());

        final ServiceName serviceName = deploymentUnit.getServiceName().append(FIRST_PHASE_NAME);
        final Phase firstPhase = Phase.values()[0];
        final DeploymentUnitPhaseService<?> phaseService = DeploymentUnitPhaseService.create(firstPhase);
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
        // Delete the first phase deployer
        final ServiceName serviceName = deploymentUnit.getServiceName().append(FIRST_PHASE_NAME);
        final ServiceController<?> controller = context.getController().getServiceContainer().getService(serviceName);
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

    private static final class DeploymentServiceListener extends AbstractServiceListener<Object> {
        private final long startTime;
        private final ServiceTarget target;
        private final String deploymentName;
        private final AtomicInteger count = new AtomicInteger();

        public DeploymentServiceListener(final long time, final ServiceTarget target, final String deploymentName) {
            startTime = time;
            this.target = target;
            this.deploymentName = deploymentName;
        }

        public void listenerAdded(final ServiceController<? extends Object> controller) {
            final ServiceController.Mode mode = controller.getMode();
            if (mode == ServiceController.Mode.ACTIVE) {
                count.incrementAndGet();
            } else {
                controller.removeListener(this);
            }
        }

        public void serviceStarted(final ServiceController<? extends Object> controller) {
            controller.removeListener(this);
            tick();
        }

        public void serviceFailed(final ServiceController<? extends Object> controller, final StartException reason) {
            controller.removeListener(this);
            tick();
        }

        public void serviceRemoved(final ServiceController<? extends Object> controller) {
            controller.removeListener(this);
            tick();
        }

        public void dependencyFailed(final ServiceController<? extends Object> controller) {
            controller.removeListener(this);
            tick();
        }

        public void dependencyUninstalled(final ServiceController<? extends Object> controller) {
            controller.removeListener(this);
            tick();
        }

        private void tick() {
            if (count.decrementAndGet() == 0) {
                target.removeListener(this);
                log.infof("Completed deployment of \"%s\" in %d ms", deploymentName, Long.valueOf((System.nanoTime() - startTime) / 1000000L));
            }
        }
    }
}
