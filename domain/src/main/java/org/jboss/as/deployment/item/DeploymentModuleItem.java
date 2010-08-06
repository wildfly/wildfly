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

package org.jboss.as.deployment.item;

import org.jboss.as.deployment.module.DeploymentModuleLoader;
import org.jboss.as.deployment.module.DeploymentModuleLoaderProvider;
import org.jboss.as.deployment.module.DeploymentModuleLoaderProviderTranslator;
import org.jboss.as.deployment.module.DeploymentModuleService;
import org.jboss.as.deployment.module.ModuleConfig;
import org.jboss.msc.inject.TranslatingInjector;
import org.jboss.msc.service.BatchBuilder;
import org.jboss.vfs.VirtualFile;

/**
 * Deployment item responsible for installing a service to manage the life-cycle of a deployment module.
 *
 * @author John E. Bailey
 */
public class DeploymentModuleItem implements DeploymentItem {

    private final String deploymentName;
    private final ModuleConfig moduleConfig;
    private final VirtualFile deploymentRoot;

    public DeploymentModuleItem(String deploymentName, ModuleConfig moduleConfig, VirtualFile deploymentRoot) {
        this.deploymentName = deploymentName;
        this.moduleConfig = moduleConfig;
        this.deploymentRoot = deploymentRoot;
    }

    @Override
    public void install(DeploymentItemContext context) {
        final BatchBuilder batchBuilder = context.getBatchBuilder();

        final DeploymentModuleService deploymentModuleService = new DeploymentModuleService(moduleConfig);
        batchBuilder.addService(DeploymentModuleService.SERVICE_NAME.append(deploymentName), deploymentModuleService)
            .addDependency(DeploymentModuleLoaderProvider.SERVICE_NAME, DeploymentModuleLoaderProvider.class,
                new TranslatingInjector<DeploymentModuleLoaderProvider, DeploymentModuleLoader>(
                    new DeploymentModuleLoaderProviderTranslator(deploymentRoot),
                    deploymentModuleService.getDeploymentModuleLoaderInjector()
            )
        );
    }
}
