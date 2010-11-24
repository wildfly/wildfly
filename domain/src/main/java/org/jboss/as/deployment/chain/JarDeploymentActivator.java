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

package org.jboss.as.deployment.chain;

import org.jboss.as.deployment.Phase;
import org.jboss.as.deployment.module.DeploymentModuleLoader;
import org.jboss.as.deployment.module.DeploymentModuleLoaderProcessor;
import org.jboss.as.deployment.module.DeploymentModuleLoaderService;
import org.jboss.as.deployment.module.ManifestAttachmentProcessor;
import org.jboss.as.deployment.module.ModuleConfigProcessor;
import org.jboss.as.deployment.module.ModuleDependencyProcessor;
import org.jboss.as.deployment.module.ModuleDeploymentProcessor;
import org.jboss.as.deployment.naming.ModuleContextProcessor;
import org.jboss.as.deployment.processor.AnnotationIndexProcessor;
import org.jboss.as.deployment.processor.ServiceActivatorDependencyProcessor;
import org.jboss.as.deployment.processor.ServiceActivatorProcessor;
import org.jboss.as.deployment.unit.DeploymentUnitProcessor;
import org.jboss.as.deployment.unit.DeploymentUnitProcessorService;
import org.jboss.msc.service.BatchBuilder;
import org.jboss.msc.service.BatchServiceBuilder;
import org.jboss.msc.service.ServiceActivator;
import org.jboss.msc.service.ServiceActivatorContext;
import org.jboss.msc.value.InjectedValue;

/**
 * Service activator which installs the various service required for jar deployments.
 *
 * @author John E. Bailey
 */
public class JarDeploymentActivator implements ServiceActivator {

    /**
     * Activate the services required for service deployments.
     *
     * @param context The service activator context
     */
    public void activate(final ServiceActivatorContext context) {
        final BatchBuilder batchBuilder = context.getBatchBuilder();

        addDeploymentProcessor(batchBuilder, new ManifestAttachmentProcessor(), Phase.MANIFEST_ATTACHMENT_PROCESSOR);
        addDeploymentProcessor(batchBuilder, new AnnotationIndexProcessor(), Phase.ANNOTATION_INDEX_PROCESSOR);
        addDeploymentProcessor(batchBuilder, new ModuleDependencyProcessor(), Phase.MODULE_DEPENDENCY_PROCESSOR);
        addDeploymentProcessor(batchBuilder, new ModuleConfigProcessor(), Phase.MODULE_CONFIG_PROCESSOR);
        final InjectedValue<DeploymentModuleLoader> moduleLoaderInjector = new InjectedValue<DeploymentModuleLoader>();
        addDeploymentProcessor(batchBuilder, new DeploymentModuleLoaderProcessor(moduleLoaderInjector), Phase.DEPLOYMENT_MODULE_LOADER_PROCESSOR)
            .addDependency(DeploymentModuleLoaderService.SERVICE_NAME, DeploymentModuleLoader.class, moduleLoaderInjector);
        addDeploymentProcessor(batchBuilder, new ModuleDeploymentProcessor(), Phase.MODULE_DEPLOYMENT_PROCESSOR);
        addDeploymentProcessor(batchBuilder, new ModuleContextProcessor(), Phase.MODULE_CONTEXT_PROCESSOR);
        addDeploymentProcessor(batchBuilder, new ServiceActivatorDependencyProcessor(), Phase.SERVICE_ACTIVATION_DEPENDENCY_PROCESSOR);
        addDeploymentProcessor(batchBuilder, new ServiceActivatorProcessor(), Phase.SERVICE_ACTIVATOR_PROCESSOR);
    }

    private <T extends DeploymentUnitProcessor> BatchServiceBuilder<T> addDeploymentProcessor(final BatchBuilder batchBuilder, final T deploymentUnitProcessor, final long priority) {
        final DeploymentUnitProcessorService<T> deploymentUnitProcessorService = new DeploymentUnitProcessorService<T>(deploymentUnitProcessor);
        return batchBuilder.addService(DeploymentUnitProcessor.SERVICE_NAME_BASE.append(deploymentUnitProcessor.getClass().getName()), deploymentUnitProcessorService)
            .addDependency(DeploymentChain.SERVICE_NAME, DeploymentChain.class, new DeploymentChainProcessorInjector<T>(deploymentUnitProcessorService, priority));
    }
}
