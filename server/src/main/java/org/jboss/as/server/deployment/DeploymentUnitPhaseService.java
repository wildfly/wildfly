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
import java.util.List;
import java.util.ListIterator;
import java.util.Set;
import org.jboss.as.deployment.AttachmentKey;
import org.jboss.as.deployment.Phase;
import org.jboss.as.deployment.unit.DeploymentPhaseContextImpl;
import org.jboss.as.deployment.unit.DeploymentUnit;
import org.jboss.as.deployment.unit.DeploymentUnitProcessor;
import org.jboss.as.deployment.unit.DeploymentPhaseContext;
import org.jboss.logging.Logger;
import org.jboss.msc.inject.Injector;
import org.jboss.msc.service.DelegatingServiceRegistry;
import org.jboss.msc.service.LifecycleContext;
import org.jboss.msc.service.MultipleRemoveListener;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceContainer;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.service.TrackingServiceTarget;
import org.jboss.msc.value.InjectedValue;

/**
 * A service which executes a particular phase of deployment.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
final class DeploymentUnitPhaseService<T> implements Service<T> {

    private final InjectedValue<DeployerChains> deployerChainsInjector = new InjectedValue<DeployerChains>();
    private final InjectedValue<DeploymentUnit> deploymentUnitContextInjector = new InjectedValue<DeploymentUnit>();
    private final Phase phase;
    private final AttachmentKey<T> valueKey;

    private Set<ServiceName> serviceNames;

    private static final Logger log = Logger.getLogger("org.jboss.as.server.deployment");

    private DeploymentUnitPhaseService(final Phase phase, final AttachmentKey<T> valueKey) {
        this.phase = phase;
        this.valueKey = valueKey;
    }

    private static <T> DeploymentUnitPhaseService<T> create(final Phase phase, AttachmentKey<T> valueKey) {
        return new DeploymentUnitPhaseService<T>(phase, valueKey);
    }

    static DeploymentUnitPhaseService<?> create(final Phase phase) {
        return create(phase, phase.getPhaseKey());
    }

    public synchronized void start(final StartContext context) throws StartException {
        final DeployerChains chains = deployerChainsInjector.getValue();
        final DeploymentUnit deploymentUnitContext = deploymentUnitContextInjector.getValue();
        final List<DeploymentUnitProcessor> list = chains.getChain(phase);
        final ListIterator<DeploymentUnitProcessor> iterator = list.listIterator();
        final ServiceContainer container = context.getController().getServiceContainer();
        final TrackingServiceTarget serviceTarget = new TrackingServiceTarget(container.subTarget());
        final DeploymentPhaseContext processorContext = new DeploymentPhaseContextImpl(serviceTarget, new DelegatingServiceRegistry(container), deploymentUnitContext);
        while (iterator.hasNext()) {
            final DeploymentUnitProcessor processor = iterator.next();
            try {
                processor.deploy(processorContext);
            } catch (Throwable e) {
                final StartException cause = new StartException(String.format("Failed to process %s", deploymentUnitContext), e);
                while (iterator.hasPrevious()) {
                    final DeploymentUnitProcessor prev = iterator.previous();
                    safeUndeploy(deploymentUnitContext, prev);
                }
                final MultipleRemoveListener<Throwable> listener = MultipleRemoveListener.create(new MultipleRemoveListener.Callback<Throwable>() {
                    public void handleDone(final Throwable parameter) {
                        context.failed(cause);
                    }
                }, cause);
                for (ServiceName serviceName : serviceTarget.getSet()) {
                    final ServiceController<?> controller = container.getService(serviceName);
                    if (controller != null) {
                        controller.setMode(ServiceController.Mode.REMOVE);
                        controller.addListener(listener);
                    }
                }
                listener.done();
                return;
            }
        }
        serviceNames = new HashSet<ServiceName>(serviceTarget.getSet());
    }

    public synchronized void stop(final StopContext context) {
        final DeploymentUnit deploymentUnitContext = deploymentUnitContextInjector.getValue();
        final DeployerChains chains = deployerChainsInjector.getValue();
        final List<DeploymentUnitProcessor> list = chains.getChain(phase);
        final ListIterator<DeploymentUnitProcessor> iterator = list.listIterator();
        while (iterator.hasPrevious()) {
            final DeploymentUnitProcessor prev = iterator.previous();
            safeUndeploy(deploymentUnitContext, prev);
        }
        final MultipleRemoveListener<LifecycleContext> listener = MultipleRemoveListener.create(context);
        final ServiceContainer container = context.getController().getServiceContainer();
        for (ServiceName serviceName : serviceNames) {
            final ServiceController<?> controller = container.getService(serviceName);
            if (controller != null) {
                controller.setMode(ServiceController.Mode.REMOVE);
                controller.addListener(listener);
            }
        }
        listener.done();
    }

    private static void safeUndeploy(final DeploymentUnit deploymentUnitContext, final DeploymentUnitProcessor prev) {
        try {
            prev.undeploy(deploymentUnitContext);
        } catch (Throwable t) {
            log.errorf(t, "Deployment unit processor %s unexpectedly threw an exception during undeploy of %s", prev, deploymentUnitContext);
        }
    }

    public synchronized T getValue() throws IllegalStateException, IllegalArgumentException {
        return deploymentUnitContextInjector.getValue().getAttachment(valueKey);
    }

    InjectedValue<DeployerChains> getDeployerChainsInjector() {
        return deployerChainsInjector;
    }

    Injector<DeploymentUnit> getDeploymentUnitContextInjector() {
        return deploymentUnitContextInjector;
    }
}
