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

package org.jboss.as.osgi.deployment;

import org.jboss.as.deployment.chain.DeploymentChain;
import org.jboss.as.deployment.chain.DeploymentChainImpl;
import org.jboss.as.deployment.chain.DeploymentChainProcessorInjector;
import org.jboss.as.deployment.chain.DeploymentChainProvider;
import org.jboss.as.deployment.chain.DeploymentChainProviderInjector;
import org.jboss.as.deployment.chain.DeploymentChainProviderService;
import org.jboss.as.deployment.chain.DeploymentChainService;
import org.jboss.as.deployment.chain.JarDeploymentActivator;
import org.jboss.as.deployment.unit.DeploymentUnitProcessor;
import org.jboss.as.deployment.unit.DeploymentUnitProcessorService;
import org.jboss.msc.service.BatchBuilder;
import org.jboss.msc.service.BatchServiceBuilder;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.value.Value;
import org.jboss.msc.value.Values;

/**
 * Service activator which installs the various service required for OSGi deployments.
 *
 * @author Thomas.Diesler@jboss.com
 * @since 07-Oct-2010
 */
public class OSGiDeploymentActivator {
    public static final long OSGI_DEPLOYMENT_CHAIN_PRIORITY = JarDeploymentActivator.JAR_DEPLOYMENT_CHAIN_PRIORITY +  1000L;
    public static final ServiceName OSGI_DEPLOYMENT_CHAIN_SERVICE_NAME = DeploymentChain.SERVICE_NAME.append("osgi");

    /**
     * Activate the services required for service deployments.
     */
    public void activate(final BatchBuilder batchBuilder) {
        batchBuilder.addServiceValueIfNotExist(DeploymentChainProviderService.SERVICE_NAME, new DeploymentChainProviderService());

        final Value<DeploymentChain> deploymentChainValue = Values.immediateValue((DeploymentChain)new DeploymentChainImpl(OSGI_DEPLOYMENT_CHAIN_SERVICE_NAME.toString()))   ;
        final DeploymentChainService deploymentChainService = new DeploymentChainService(deploymentChainValue);
        BatchServiceBuilder<?> serviceBuilder = batchBuilder.addService(OSGI_DEPLOYMENT_CHAIN_SERVICE_NAME, deploymentChainService);
        DeploymentChainProviderInjector<DeploymentChain> injector = new DeploymentChainProviderInjector<DeploymentChain>(deploymentChainValue, new OSGiDeploymentChainSelector(), OSGI_DEPLOYMENT_CHAIN_PRIORITY);
        serviceBuilder.addDependency(DeploymentChainProviderService.SERVICE_NAME, DeploymentChainProvider.class, injector);
        addDeploymentProcessor(batchBuilder, new OSGiManifestDeploymentProcessor(), OSGiManifestDeploymentProcessor.PRIORITY);
        addDeploymentProcessor(batchBuilder, new OSGiAttachmentsDeploymentProcessor(), OSGiAttachmentsDeploymentProcessor.PRIORITY);
    }

    private <T extends DeploymentUnitProcessor> BatchServiceBuilder<T> addDeploymentProcessor(final BatchBuilder batchBuilder, final T deploymentUnitProcessor, final long priority) {
        final DeploymentUnitProcessorService<T> deploymentUnitProcessorService = new DeploymentUnitProcessorService<T>(deploymentUnitProcessor);
        ServiceName serviceName = OSGI_DEPLOYMENT_CHAIN_SERVICE_NAME.append(deploymentUnitProcessor.getClass().getName());
        BatchServiceBuilder<T> serviceBuilder = batchBuilder.addService(serviceName, deploymentUnitProcessorService);
        DeploymentChainProcessorInjector<T> injector = new DeploymentChainProcessorInjector<T>(deploymentUnitProcessorService, priority);
        return serviceBuilder.addDependency(OSGI_DEPLOYMENT_CHAIN_SERVICE_NAME, DeploymentChain.class, injector);
    }
}
