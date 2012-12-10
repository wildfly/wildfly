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

import static org.jboss.as.server.ServerLogger.DEPLOYMENT_LOGGER;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;

import org.jboss.as.server.ServerLogger;
import org.jboss.as.server.ServerMessages;
import org.jboss.msc.service.DelegatingServiceRegistry;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceContainer;
import org.jboss.msc.service.ServiceController.Mode;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;

/**
 * A service which executes a particular phase of deployment.
 *
 * @param <T> the public type of this deployment unit phase
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
final class DeploymentUnitPhaseService<T> implements Service<T> {

    private static final AttachmentKey<AttachmentList<DeploymentUnit>> UNVISITED_DEFERRED_MODULES = AttachmentKey.createList(DeploymentUnit.class);

    private final InjectedValue<DeployerChains> deployerChainsInjector = new InjectedValue<DeployerChains>();
    private final DeploymentUnit deploymentUnit;
    private final Phase phase;
    private final AttachmentKey<T> valueKey;
    private final List<AttachedDependency> injectedAttachedDependencies = new ArrayList<AttachedDependency>();

    private DeploymentUnitPhaseService(final DeploymentUnit deploymentUnit, final Phase phase, final AttachmentKey<T> valueKey) {
        this.deploymentUnit = deploymentUnit;
        this.phase = phase;
        this.valueKey = valueKey;
    }

    private static <T> DeploymentUnitPhaseService<T> create(final DeploymentUnit deploymentUnit, final Phase phase, AttachmentKey<T> valueKey) {
        return new DeploymentUnitPhaseService<T>(deploymentUnit, phase, valueKey);
    }

    static DeploymentUnitPhaseService<?> create(final DeploymentUnit deploymentUnit, final Phase phase) {
        return create(deploymentUnit, phase, phase.getPhaseKey());
    }

    @SuppressWarnings("unchecked")
    public synchronized void start(final StartContext context) throws StartException {
        final DeployerChains chains = deployerChainsInjector.getValue();
        final DeploymentUnit deploymentUnit = this.deploymentUnit;
        final List<RegisteredDeploymentUnitProcessor> list = chains.getChain(phase);
        final ListIterator<RegisteredDeploymentUnitProcessor> iterator = list.listIterator();
        final ServiceContainer container = context.getController().getServiceContainer();
        final ServiceTarget serviceTarget = context.getChildTarget().subTarget();
        final Phase nextPhase = phase.next();
        final String name = deploymentUnit.getName();
        final DeploymentUnit parent = deploymentUnit.getParent();
        final ServiceBuilder<?> phaseServiceBuilder;
        final DeploymentUnitPhaseService<?> phaseService;
        if (nextPhase != null) {
            final ServiceName serviceName = DeploymentUtils.getDeploymentUnitPhaseServiceName(deploymentUnit, nextPhase);
            phaseService = DeploymentUnitPhaseService.create(deploymentUnit, nextPhase);
            phaseServiceBuilder = this.deploymentUnit.getAttachment(Attachments.DEPLOYMENT_UNIT_PHASE_SERVICE_BUILDER).build(serviceTarget, serviceName, phaseService, nextPhase);
        } else {
            phaseServiceBuilder = null;
            phaseService = null;
        }
        final DeploymentPhaseContext processorContext = new DeploymentPhaseContextImpl(serviceTarget, new DelegatingServiceRegistry(container), phaseServiceBuilder,
                deploymentUnit, phase);

        // attach any injected values from the last phase
        for (AttachedDependency attachedDependency : injectedAttachedDependencies) {
            final Attachable target;
            if (attachedDependency.isDeploymentUnit()) {
                target = deploymentUnit;
            } else {
                target = processorContext;
            }
            if (attachedDependency.getAttachmentKey() instanceof ListAttachmentKey) {
                target.addToAttachmentList((AttachmentKey) attachedDependency.getAttachmentKey(), attachedDependency.getValue().getValue());
            } else {
                target.putAttachment((AttachmentKey) attachedDependency.getAttachmentKey(), attachedDependency.getValue().getValue());
            }
        }

        while (iterator.hasNext()) {
            final RegisteredDeploymentUnitProcessor processor = iterator.next();
            try {
                if (shouldRun(deploymentUnit, processor)) {
                    processor.getProcessor().deploy(processorContext);
                }
            } catch (Throwable e) {
                while (iterator.hasPrevious()) {
                    final RegisteredDeploymentUnitProcessor prev = iterator.previous();
                    safeUndeploy(deploymentUnit, phase, prev);
                }
                throw ServerMessages.MESSAGES.deploymentPhaseFailed(phase, deploymentUnit, e);
            }
        }
        if (nextPhase != null) {
            phaseServiceBuilder.addDependency(Services.JBOSS_DEPLOYMENT_CHAINS, DeployerChains.class, phaseService.getDeployerChainsInjector());
            phaseServiceBuilder.addDependency(context.getController().getName());

            final List<ServiceName> nextPhaseDeps = processorContext.getAttachment(Attachments.NEXT_PHASE_DEPS);
            if (nextPhaseDeps != null) {
                phaseServiceBuilder.addDependencies(nextPhaseDeps);
            }
            final List<AttachableDependency> nextPhaseAttachableDeps = processorContext.getAttachment(Attachments.NEXT_PHASE_ATTACHABLE_DEPS);
            if (nextPhaseAttachableDeps != null) {
                for (AttachableDependency attachableDep : nextPhaseAttachableDeps) {
                    AttachedDependency result = new AttachedDependency(attachableDep.getAttachmentKey(), attachableDep.isDeploymentUnit());
                    phaseServiceBuilder.addDependency(attachableDep.getServiceName(), result.getValue());
                    phaseService.injectedAttachedDependencies.add(result);
                }
            }

            // Add a dependency on the parent's next phase
            if (parent != null) {
                phaseServiceBuilder.addDependencies(Services.deploymentUnitName(parent.getName(), nextPhase));
            }

            // Make sure all sub deployments have finished this phase before moving to the next one
            List<DeploymentUnit> subDeployments = deploymentUnit.getAttachmentList(Attachments.SUB_DEPLOYMENTS);
            for (DeploymentUnit du : subDeployments) {
                phaseServiceBuilder.addDependencies(du.getServiceName().append(phase.name()));
            }

            // Defer the {@link Phase.FIRST_MODULE_USE} phase
            List<String> deferredModules = DeploymentUtils.getDeferredModules(deploymentUnit);
            if (nextPhase == Phase.FIRST_MODULE_USE) {
                Mode initialMode = getDeferableInitialMode(deploymentUnit, deferredModules);
                if (initialMode != Mode.ACTIVE) {
                    DEPLOYMENT_LOGGER.infoDeferDeploymentPhase(nextPhase, name, initialMode);
                    phaseServiceBuilder.setInitialMode(initialMode);
                }
            }

            phaseServiceBuilder.install();
        }
    }

    public synchronized void stop(final StopContext context) {
        final DeploymentUnit deploymentUnitContext = deploymentUnit;
        final DeployerChains chains = deployerChainsInjector.getValue();
        final List<RegisteredDeploymentUnitProcessor> list = chains.getChain(phase);
        final ListIterator<RegisteredDeploymentUnitProcessor> iterator = list.listIterator(list.size());
        while (iterator.hasPrevious()) {
            final RegisteredDeploymentUnitProcessor prev = iterator.previous();
            safeUndeploy(deploymentUnitContext, phase, prev);
        }
    }

    private Mode getDeferableInitialMode(final DeploymentUnit deploymentUnit, List<String> deferredModules) {
        // Make the deferred module NEVER
        if (deferredModules.contains(deploymentUnit.getName())) {
            return Mode.NEVER;
        }
        Mode initialMode = Mode.ACTIVE;
        DeploymentUnit parent = DeploymentUtils.getTopDeploymentUnit(deploymentUnit);
        if (parent == deploymentUnit) {
            List<DeploymentUnit> subDeployments = parent.getAttachmentList(Attachments.SUB_DEPLOYMENTS);
            for (DeploymentUnit du : subDeployments) {
                // Always make the EAR LAZY if it could contain deferrable sub-deployments
                if (du.hasAttachment(Attachments.OSGI_MANIFEST)) {
                    initialMode = Mode.LAZY;
                    break;
                }
            }
            // Initialize the list of unvisited deferred modules
            if (initialMode == Mode.LAZY) {
                for (DeploymentUnit du : subDeployments) {
                    parent.addToAttachmentList(UNVISITED_DEFERRED_MODULES, du);
                }
            }
        } else {
            // Make the non-deferred sibling PASSIVE if it is not the last to visit
            List<DeploymentUnit> unvisited = parent.getAttachmentList(UNVISITED_DEFERRED_MODULES);
            synchronized (unvisited) {
                unvisited.remove(deploymentUnit);
                if (!deferredModules.isEmpty() || !unvisited.isEmpty()) {
                    initialMode = Mode.PASSIVE;
                }
            }
        }
        return initialMode;
    }

    private static void safeUndeploy(final DeploymentUnit deploymentUnit, final Phase phase, final RegisteredDeploymentUnitProcessor prev) {
        try {
            if (shouldRun(deploymentUnit, prev)) {
                prev.getProcessor().undeploy(deploymentUnit);
            }
        } catch (Throwable t) {
            ServerLogger.DEPLOYMENT_LOGGER.caughtExceptionUndeploying(t, prev.getProcessor(), phase, deploymentUnit);
        }
    }

    public synchronized T getValue() throws IllegalStateException, IllegalArgumentException {
        return deploymentUnit.getAttachment(valueKey);
    }

    InjectedValue<DeployerChains> getDeployerChainsInjector() {
        return deployerChainsInjector;
    }

    private static boolean shouldRun(final DeploymentUnit unit, final RegisteredDeploymentUnitProcessor deployer) {
        Set<String> shouldNotRun = unit.getAttachment(Attachments.EXCLUDED_SUBSYSTEMS);
        if (shouldNotRun == null) {
            if (unit.getParent() != null) {
                shouldNotRun = unit.getParent().getAttachment(Attachments.EXCLUDED_SUBSYSTEMS);
            }
            if (shouldNotRun == null) {
                return true;
            }
        }
        return !shouldNotRun.contains(deployer.getSubsystemName());
    }
}
