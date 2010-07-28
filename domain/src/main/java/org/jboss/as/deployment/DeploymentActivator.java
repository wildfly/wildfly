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

package org.jboss.as.deployment;

import org.jboss.as.deployment.chain.DeploymentChainProvider;
import org.jboss.as.deployment.module.DeploymentModuleLoader;
import org.jboss.as.deployment.module.DeploymentModuleLoaderImpl;
import org.jboss.as.deployment.module.DeploymentModuleLoaderProvider;
import org.jboss.as.deployment.module.DeploymentModuleLoaderService;
import org.jboss.as.deployment.module.TempFileProviderService;
import org.jboss.modules.ModuleLoaderSelector;
import org.jboss.msc.service.BatchBuilder;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceActivator;
import org.jboss.msc.service.ServiceContainer;
import org.jboss.msc.value.Values;
import org.jboss.vfs.VirtualFile;

/**
 * ServiceActivator used to setup initial services for deployment.  
 *
 * @author John E. Bailey
 */
public class DeploymentActivator implements ServiceActivator {
    @Override
    public void activate(ServiceContainer container, BatchBuilder batchBuilder) {
        batchBuilder.addService(DeploymentChainProvider.SERVICE_NAME, new DeploymentChainProvider());
        batchBuilder.addService(DeploymentModuleLoaderProvider.SERVICE_NAME, new DeploymentModuleLoaderProvider());

        // Setup default deployment module loader
        final DeploymentModuleLoader deploymentModuleLoader = new DeploymentModuleLoaderImpl(ModuleLoaderSelector.DEFAULT.getCurrentLoader());
        final Service<DeploymentModuleLoader> deploymentModuleLoaderService = new DeploymentModuleLoaderService(deploymentModuleLoader);
        batchBuilder.addService(DeploymentModuleLoaderImpl.SERVICE_NAME, deploymentModuleLoaderService)
            .addDependency(DeploymentModuleLoaderProvider.SERVICE_NAME).toInjector(
                new DeploymentModuleLoaderProvider.SelectorInjector<DeploymentModuleLoaderProvider.Selector>(deploymentModuleLoaderService,
                        Values.<DeploymentModuleLoaderProvider.Selector>immediateValue(new DeploymentModuleLoaderProvider.Selector() {
                            public boolean supports(VirtualFile root) {
                                return true;
                            }
                }), DeploymentModuleLoaderImpl.SELECTOR_PRIORITY));

        batchBuilder.addService(TempFileProviderService.SERVICE_NAME, new TempFileProviderService());
    }
}
