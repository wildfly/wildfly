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

import org.jboss.as.deployment.chain.DeploymentChain;
import org.jboss.as.deployment.chain.DeploymentChainImpl;
import org.jboss.as.deployment.chain.DeploymentChainProcessorInjector;
import org.jboss.as.deployment.chain.DeploymentChainProvider;
import org.jboss.as.deployment.module.DeploymentModuleLoaderProcessor;
import org.jboss.as.deployment.module.ModuleConfigProcessor;
import org.jboss.as.deployment.module.ModuleDependencyProcessor;
import org.jboss.as.deployment.module.ModuleDeploymentProcessor;
import org.jboss.as.deployment.unit.DeploymentUnitProcessor;
import org.jboss.as.deployment.unit.DeploymentUnitProcessorService;
import org.jboss.msc.service.BatchBuilder;
import org.jboss.msc.service.ServiceActivator;
import org.jboss.msc.service.ServiceActivatorContext;
import org.jboss.msc.service.ServiceName;

/**
 * Service activator which installs the various service required for service deployments.
 * 
 * @author John E. Bailey
 */
public class ServiceDeploymentActivator implements ServiceActivator {

    public static final ServiceName SERVICE_DEPLOYMENT_CHAIN_NAME = ServiceName.JBOSS.append("service", "deployment", "chain");
    public static final long SERVICE_DEPLOYMENT_CHAIN_PRIORITY = 100000L;

    /**
     * Activate the services required for service deployments.
     * 
     * @param context The service activator context
     */
    public void activate(final ServiceActivatorContext context) {
        final DeploymentChain deploymentChain = new DeploymentChainImpl("deployment.chain.service");
        deploymentChain.addProcessor(new ModuleDependencyProcessor(), ModuleDependencyProcessor.PRIORITY);
        deploymentChain.addProcessor(new ModuleConfigProcessor(), ModuleConfigProcessor.PRIORITY);
        deploymentChain.addProcessor(new DeploymentModuleLoaderProcessor(), DeploymentModuleLoaderProcessor.PRIORITY);
        deploymentChain.addProcessor(new ModuleDeploymentProcessor(), ModuleDeploymentProcessor.PRIORITY);
        deploymentChain.addProcessor(new ServiceDeploymentParsingProcessor(), ServiceDeploymentParsingProcessor.PRIORITY);
        deploymentChain.addProcessor(new ParsedServiceDeploymentProcessor(), ParsedServiceDeploymentProcessor.PRIORITY);
        DeploymentChainProvider.INSTANCE.addDeploymentChain(deploymentChain, new ServiceDeploymentChainSelector(), SERVICE_DEPLOYMENT_CHAIN_PRIORITY);
    }
}
