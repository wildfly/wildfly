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

import java.util.IdentityHashMap;
import org.jboss.logging.Logger;
import org.jboss.msc.inject.Injector;
import org.jboss.msc.service.AbstractServiceListener;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceListener;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceRegistry;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;

import java.util.Collections;
import java.util.Set;

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
    private volatile DeploymentServiceListener listener;

    protected AbstractDeploymentUnitService() {
    }

    public synchronized void start(final StartContext context) throws StartException {
        ServiceTarget target = context.getChildTarget();
        final String deploymentName = context.getController().getName().getSimpleName();
        final DeploymentServiceListener listener = new DeploymentServiceListener(deploymentName);
        this.listener = listener;
        log.infof("Starting deployment of \"%s\"", deploymentName);
        // Create the first phase deployer
        target.addListener(ServiceListener.Inheritance.ALL, listener);
        deploymentUnit = createAndInitializeDeploymentUnit(context.getController().getServiceContainer());
        deploymentUnit.putAttachment(Attachments.STATUS_LISTENER, listener);

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
        deploymentUnit = null;
        log.infof("Stopped deployment %s in %dms", deploymentName, Integer.valueOf((int) (context.getElapsedTime() / 1000000L)));
    }

    public synchronized DeploymentUnit getValue() throws IllegalStateException, IllegalArgumentException {
        return deploymentUnit;
    }

    public void reportStatus() {
        listener.explainStatus();
    }

    public DeploymentStatus getStatus() {
        return listener.getStatus();
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

    public static final class DeploymentServiceListener extends AbstractServiceListener<Object> {
        private final String deploymentName;

        private final Set<ServiceController<?>> startFailedServices = Collections.newSetFromMap(new IdentityHashMap<ServiceController<?>, Boolean>());
        private final Set<ServiceController<?>> servicesMissingDependencies = Collections.newSetFromMap(new IdentityHashMap<ServiceController<?>, Boolean>());
        private DeploymentStatus previous = DeploymentStatus.NEW;

        public DeploymentServiceListener(final String deploymentName) {
            this.deploymentName = deploymentName;
        }

        public synchronized DeploymentStatus getStatus() {
            return startFailedServices.isEmpty() && servicesMissingDependencies.isEmpty() ? DeploymentStatus.OK : DeploymentStatus.FAILED;
        }

        public synchronized void explainStatus() {
            DeploymentStatus oldStatus = previous;
            boolean hasFailed = !startFailedServices.isEmpty();
            boolean hasMissing = !servicesMissingDependencies.isEmpty();
            final DeploymentStatus newStatus = hasFailed || hasMissing ? DeploymentStatus.FAILED : DeploymentStatus.OK;
            if (oldStatus != newStatus) {
                previous = newStatus;
                if (hasFailed || hasMissing) {
                    // write out failure
                    final StringBuilder msg = new StringBuilder();
                    msg.append("Deployment '").append(deploymentName).append("' has ");
                    if (hasFailed) {
                        msg.append("failed services");
                        if (hasMissing) {
                            msg.append(" and ");
                        }
                    }
                    if (hasMissing) {
                        msg.append("services missing dependencies");
                    }
                    if (hasFailed) {
                        msg.append("\n    Failed services:");
                        for (ServiceController<?> service : startFailedServices) {
                            final StartException cause = service.getStartException();
                            if (cause != null) {
                                msg.append("\n        ").append(service.getName()).append(": ").append(cause);
                            }
                        }
                    }
                    if (hasMissing) {
                        msg.append("\n    Missing dependencies:");
                        for (ServiceController<?> service : servicesMissingDependencies) {
                            msg.append("\n        ").append(service.getName()).append(" is missing: ");
                            for (ServiceName name : service.getImmediateUnavailableDependencies()) {
                                msg.append("\n            ").append(name);
                            }
                        }
                    }
                    log.error(msg);
                } else {
                    log.infof("Deployment '%s' started successfully", deploymentName);
                }
            }
        }

        public synchronized void immediateDependencyAvailable(final ServiceController<? extends Object> controller) {
            servicesMissingDependencies.remove(controller);
        }

        public synchronized void immediateDependencyUnavailable(final ServiceController<? extends Object> controller) {
            servicesMissingDependencies.add(controller);
        }

        public synchronized void transition(final ServiceController<? extends Object> controller, final ServiceController.Transition transition) {
            switch (transition) {
                case REMOVING_to_REMOVED: {
                    servicesMissingDependencies.remove(controller);
                    // fall through
                }
                case START_FAILED_to_DOWN:
                case START_FAILED_to_STARTING: {
                    startFailedServices.remove(controller);
                    break;
                }
                case STARTING_to_START_FAILED: {
                    startFailedServices.add(controller);
                    break;
                }
            }
        }
    }
}
