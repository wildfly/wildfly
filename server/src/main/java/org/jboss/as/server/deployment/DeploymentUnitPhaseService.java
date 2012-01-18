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

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

import org.jboss.as.server.ServerLogger;
import org.jboss.msc.service.DelegatingServiceRegistry;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceContainer;
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
        final List<DeploymentUnitProcessor> list = chains.getChain(phase);
        final ListIterator<DeploymentUnitProcessor> iterator = list.listIterator();
        final ServiceContainer container = context.getController().getServiceContainer();
        final ServiceTarget serviceTarget = context.getChildTarget().subTarget();
        final Phase nextPhase = phase.next();
        final String name = deploymentUnit.getName();
        final DeploymentUnit parent = deploymentUnit.getParent();
        final ServiceBuilder<?> phaseServiceBuilder;
        final DeploymentUnitPhaseService<?> phaseService;
        if(nextPhase != null) {
            final ServiceName serviceName = parent == null ? Services.deploymentUnitName(name, nextPhase) : Services.deploymentUnitName(parent.getName(), name, nextPhase);
            phaseService = DeploymentUnitPhaseService.create(deploymentUnit, nextPhase);
            phaseServiceBuilder = serviceTarget.addService(serviceName, phaseService);
        } else {
            phaseServiceBuilder = null;
            phaseService = null;
        }
        final DeploymentPhaseContext processorContext = new DeploymentPhaseContextImpl(serviceTarget, new DelegatingServiceRegistry(container), phaseServiceBuilder, deploymentUnit, phase);

        // attach any injected values from the last phase
        for (AttachedDependency attachedDependency : injectedAttachedDependencies) {
            final Attachable target;
            if (attachedDependency.isDeploymentUnit()) {
                target = deploymentUnit;
            } else {
                target = processorContext;
            }
            if (attachedDependency.getAttachmentKey() instanceof ListAttachmentKey) {
                target.addToAttachmentList((AttachmentKey) attachedDependency.getAttachmentKey(), attachedDependency.getValue()
                        .getValue());
            } else {
                target.putAttachment((AttachmentKey) attachedDependency.getAttachmentKey(), attachedDependency.getValue()
                        .getValue());
            }
        }

        while (iterator.hasNext()) {
            final DeploymentUnitProcessor processor = iterator.next();
            try {
                processor.deploy(processorContext);
            } catch (Throwable e) {
                while (iterator.hasPrevious()) {
                    final DeploymentUnitProcessor prev = iterator.previous();
                    safeUndeploy(deploymentUnit, phase, prev);
                }
                throw new StartException(String.format("Failed to process phase %s of %s", phase, deploymentUnit), e);
            }
        }
        if (nextPhase != null) {
            phaseServiceBuilder.addDependency(Services.JBOSS_DEPLOYMENT_CHAINS, DeployerChains.class, phaseService.getDeployerChainsInjector());
            phaseServiceBuilder.addDependency(context.getController().getName());

            final List<ServiceName> nextPhaseDeps = processorContext.getAttachment(Attachments.NEXT_PHASE_DEPS);
            if(nextPhaseDeps != null) {
                phaseServiceBuilder.addDependencies(nextPhaseDeps);
            }
            final List<AttachableDependency> nextPhaseAttachableDeps = processorContext
                    .getAttachment(Attachments.NEXT_PHASE_ATTACHABLE_DEPS);
            if (nextPhaseAttachableDeps != null) {
                for (AttachableDependency attachableDep : nextPhaseAttachableDeps) {
                    AttachedDependency result = new AttachedDependency(attachableDep.getAttachmentKey(), attachableDep
                            .isDeploymentUnit());
                    phaseServiceBuilder.addDependency(attachableDep.getServiceName(), result.getValue());
                    phaseService.injectedAttachedDependencies.add(result);

                }
            }
            if (deploymentUnit.getParent() != null) {
                phaseServiceBuilder.addDependencies(Services.deploymentUnitName(deploymentUnit.getParent().getName(), nextPhase));
            }
            List<DeploymentUnit> subDeployments = deploymentUnit.getAttachmentList(Attachments.SUB_DEPLOYMENTS);
            // make sure all sub deployments have finished this phase before moving to the next one
            for (DeploymentUnit du : subDeployments) {
                phaseServiceBuilder.addDependencies(du.getServiceName().append(phase.name()));
            }

            phaseServiceBuilder.install();
        }
    }

    public synchronized void stop(final StopContext context) {
        final DeploymentUnit deploymentUnitContext = deploymentUnit;
        final DeployerChains chains = deployerChainsInjector.getValue();
        final List<DeploymentUnitProcessor> list = chains.getChain(phase);
        final ListIterator<DeploymentUnitProcessor> iterator = list.listIterator(list.size());
        while (iterator.hasPrevious()) {
            final DeploymentUnitProcessor prev = iterator.previous();
            safeUndeploy(deploymentUnitContext, phase, prev);
        }
    }

    private static void safeUndeploy(final DeploymentUnit deploymentUnit, final Phase phase, final DeploymentUnitProcessor prev) {
        try {
            prev.undeploy(deploymentUnit);
        } catch (Throwable t) {
            ServerLogger.DEPLOYMENT_LOGGER.caughtExceptionUndeploying(t, prev, phase, deploymentUnit);
        }
    }

    public synchronized T getValue() throws IllegalStateException, IllegalArgumentException {
        return deploymentUnit.getAttachment(valueKey);
    }

    InjectedValue<DeployerChains> getDeployerChainsInjector() {
        return deployerChainsInjector;
    }
}
