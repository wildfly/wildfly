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

package org.jboss.as.deployment.service;

import org.jboss.as.deployment.chain.DeploymentChainImpl;
import org.jboss.as.deployment.chain.DeploymentChainProcessorInjector;
import org.jboss.as.deployment.chain.DeploymentChainProvider;
import org.jboss.as.deployment.chain.DeploymentChainService;
import org.jboss.as.deployment.processor.ModuleConfgProcessor;
import org.jboss.as.deployment.processor.ModuleDependencyProcessor;
import org.jboss.as.deployment.processor.ModuleDeploymentProcessor;
import org.jboss.as.deployment.processor.ParsedServiceDeploymentProcessor;
import org.jboss.as.deployment.processor.ServiceDeploymentParsingProcessor;
import org.jboss.as.deployment.processor.ServiceDeploymentProcessor;
import org.jboss.as.deployment.unit.DeploymentUnitProcessor;
import org.jboss.as.deployment.unit.DeploymentUnitProcessorService;
import org.jboss.msc.service.BatchBuilder;
import org.jboss.msc.service.ServiceActivator;
import org.jboss.msc.service.ServiceContainer;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.value.Values;

/**
 * Service activator which installs the various service required for service deployments.
 * 
 * @author John E. Bailey
 */
public class ServiceDeploymentActivator implements ServiceActivator {

    public static final ServiceName SERVICE_DEPLOYMENT_CHAIN_NAME = ServiceName.JBOSS.append("service", "deployment", "chain");
    public static final long SERVICE_DEPLOYMENT_CHAIN_PRIORITY = 100000L;

    @Override
    public void activate(ServiceContainer container, BatchBuilder batchBuilder) {
        final DeploymentChainService deploymentChainService = new DeploymentChainService(new DeploymentChainImpl("deployment.chain.service"));
        batchBuilder.addService(SERVICE_DEPLOYMENT_CHAIN_NAME, deploymentChainService)
            .addDependency(DeploymentChainProvider.SERVICE_NAME)
                .toInjector(new DeploymentChainProvider.SelectorInjector(deploymentChainService, Values.immediateValue(new ServiceDeploymentChainSelector()), SERVICE_DEPLOYMENT_CHAIN_PRIORITY));

        final ServiceName processorNameBase = ServiceName.JBOSS.append("deployment", "processor");
        addProcessor(batchBuilder, processorNameBase.append("module", "dependency"), new ModuleDependencyProcessor(), ModuleDependencyProcessor.PRIORITY);
        addProcessor(batchBuilder, processorNameBase.append("module", "config"), new ModuleConfgProcessor(), ModuleConfgProcessor.PRIORITY);
        addProcessor(batchBuilder, processorNameBase.append("module", "deployment"), new ModuleDeploymentProcessor(), ModuleDeploymentProcessor.PRIORITY);
        addProcessor(batchBuilder, processorNameBase.append("service", "parser"), new ServiceDeploymentParsingProcessor(), ServiceDeploymentParsingProcessor.PRIORITY);
        addProcessor(batchBuilder, processorNameBase.append("service", "deployment"), new ServiceDeploymentProcessor(), ServiceDeploymentProcessor.PRIORITY);
        addProcessor(batchBuilder, processorNameBase.append("service", "parsed", "deployment"), new ParsedServiceDeploymentProcessor(), ParsedServiceDeploymentProcessor.PRIORITY);
    }

    private <T extends DeploymentUnitProcessor> void addProcessor(final BatchBuilder builder, final ServiceName serviceName, final T deploymentUnitProcessor, final long priority) {
        final DeploymentUnitProcessorService<T> deploymentUnitProcessorService = new DeploymentUnitProcessorService<T>(deploymentUnitProcessor);
        builder.addService(serviceName, deploymentUnitProcessorService)
            .addDependency(SERVICE_DEPLOYMENT_CHAIN_NAME)
                .toInjector(new DeploymentChainProcessorInjector<T>(deploymentUnitProcessorService, priority));
    }
}
