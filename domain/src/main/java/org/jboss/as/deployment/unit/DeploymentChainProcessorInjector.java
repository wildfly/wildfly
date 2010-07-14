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

package org.jboss.as.deployment.unit;

import org.jboss.msc.inject.InjectionException;
import org.jboss.msc.inject.Injector;
import org.jboss.msc.value.Value;

/**
 * Injector used to allow a deployment processor to add itself to a deployment chain.
 *
 * Ex.
 *      DeploymentUnitProcessorService deploymentProcessorService = new DeploymentUnitProcessorService(new DeploymentProcessor());
 *      BatchServiceBuilder processorServiceBuilder = batchBuilder.addService(processorServiceBuilderName, deploymentProcessorService);
 *      processorServiceBuilder.addDependency(deploymentChainServiceName)
 *          .toInjector(new DeploymentChainProcessorInjector(deploymentProcessorService, 1000L));
 *
 * @author John E. Bailey
 */
public class DeploymentChainProcessorInjector <T extends DeploymentUnitProcessor> implements Injector<DeploymentChain> {
    private final Value<T> deploymentUnitProcessorValue;
    private final long priority;
    private DeploymentChain deploymentChain;

    public DeploymentChainProcessorInjector(final Value<T> deploymentUnitProcessorValue, final long priority) {
        this.deploymentUnitProcessorValue = deploymentUnitProcessorValue;
        this.priority = priority;
    }

    @Override
    public void inject(final DeploymentChain deploymentChain) throws InjectionException {
        final DeploymentUnitProcessor processor = deploymentUnitProcessorValue.getValue();
        deploymentChain.addProcessor(processor, priority);
        this.deploymentChain = deploymentChain;
    }

    @Override
    public void uninject() {
        final DeploymentUnitProcessor processor = deploymentUnitProcessorValue.getValue();
        if(deploymentChain != null) {
            deploymentChain.removeProcessor(processor, priority);
            deploymentChain  = null;
        }
    }
}

