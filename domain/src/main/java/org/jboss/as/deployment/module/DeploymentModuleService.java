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

package org.jboss.as.deployment.module;

import org.jboss.modules.Module;
import org.jboss.modules.ModuleDependencySpec;
import org.jboss.modules.ModuleLoadException;
import org.jboss.modules.ModuleSpec;
import org.jboss.modules.PathFilters;
import org.jboss.msc.inject.Injector;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;

import java.io.IOException;

/**
 * Service responsible for managing the life-cycle of a deployment module.
 *  
 * @author John E. Bailey
 */
public class DeploymentModuleService implements Service<Module> {
    public static final ServiceName SERVICE_NAME = ServiceName.JBOSS.append("deployment","module");

    private final ModuleConfig moduleConfig;
    private InjectedValue<DeploymentModuleLoader> deploymentModuleLoader = new InjectedValue<DeploymentModuleLoader>();

    public DeploymentModuleService(final ModuleConfig moduleConfig) {
        this.moduleConfig = moduleConfig;
    }

    @Override
    public void start(StartContext context) throws StartException {
        final ModuleConfig moduleConfig = this.moduleConfig;
        final ModuleSpec.Builder specBuilder = ModuleSpec.build(moduleConfig.getIdentifier());
        for(ModuleConfig.ResourceRoot resource : moduleConfig.getResources()) {
            try {
                specBuilder.addResourceRoot(new VFSResourceLoader(specBuilder.getIdentifier(), resource.getRoot(), resource.getRootName()));
            } catch(IOException e) {
                throw new StartException("Failed to create VFSResourceLoader for root [" + resource.getRootName()+ "]", e);
            }
        }
        final ModuleConfig.Dependency[] dependencies = moduleConfig.getDependencies();
        for(ModuleConfig.Dependency dependency : dependencies) {
            specBuilder.addModuleDependency(
                    ModuleDependencySpec.build(dependency.getIdentifier())
                    .setExportFilter(dependency.isExport() ? PathFilters.acceptAll() : PathFilters.rejectAll())
                    .setOptional(dependency.isOptional())
                    .create()
            );
        }
        final ModuleSpec moduleSpec = specBuilder.create();
        final DeploymentModuleLoader deploymentModuleLoader = this.deploymentModuleLoader.getValue();
        deploymentModuleLoader.addModuleSpec(moduleSpec);
    }

    @Override
    public void stop(StopContext context) {
        final DeploymentModuleLoader deploymentModuleLoader = this.deploymentModuleLoader.getValue();
        deploymentModuleLoader.removeModule(moduleConfig.getIdentifier());
    }

    @Override
    public Module getValue() throws IllegalStateException {
        try {
            final DeploymentModuleLoader deploymentModuleLoader = this.deploymentModuleLoader.getValue();
            return deploymentModuleLoader.loadModule(moduleConfig.getIdentifier());
        } catch(ModuleLoadException e) {
            throw new IllegalStateException("Unable to load module value for module " + moduleConfig.getIdentifier());
        }
    }

    public Injector<DeploymentModuleLoader> getDeploymentModuleLoaderInjector() {
        return deploymentModuleLoader;
    }
}
