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

import java.util.List;
import java.util.ListIterator;
import org.jboss.as.deployment.Phase;
import org.jboss.as.deployment.unit.DeploymentUnitContext;
import org.jboss.as.deployment.unit.DeploymentUnitProcessor;
import org.jboss.as.deployment.unit.DeploymentPhaseContext;
import org.jboss.msc.inject.Injector;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;

/**
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
final class DeploymentUnitPhaseService<T> implements Service<T> {

    private final InjectedValue<DeployerChains> deployerChainsInjector = new InjectedValue<DeployerChains>();
    private final InjectedValue<DeploymentUnitContext> deploymentUnitContextInjector = new InjectedValue<DeploymentUnitContext>();
    private final Phase phase;

    public DeploymentUnitPhaseService(final Phase phase) {
        this.phase = phase;
    }

    public synchronized void start(final StartContext context) throws StartException {
        final DeployerChains chains = deployerChainsInjector.getValue();
        final List<DeploymentUnitProcessor> list = chains.getChain(phase);
        final ListIterator<DeploymentUnitProcessor> iterator = list.listIterator();
        final DeploymentPhaseContext processorContext = new DeploymentPhaseContext() {
        };
        while (iterator.hasNext()) {
            final DeploymentUnitProcessor processor = iterator.next();
            processor.deploy(processorContext);
        }
    }

    public synchronized void stop(final StopContext context) {
    }

    public synchronized T getValue() throws IllegalStateException, IllegalArgumentException {
        return null;
    }

    InjectedValue<DeployerChains> getDeployerChainsInjector() {
        return deployerChainsInjector;
    }

    Injector<DeploymentUnitContext> getDeploymentUnitContextInjector() {
        return deploymentUnitContextInjector;
    }
}
