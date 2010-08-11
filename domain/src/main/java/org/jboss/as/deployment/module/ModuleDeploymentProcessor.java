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

import org.jboss.as.deployment.AttachmentKey;
import org.jboss.as.deployment.DeploymentPhases;
import org.jboss.as.deployment.unit.DeploymentUnitContext;
import org.jboss.as.deployment.unit.DeploymentUnitProcessingException;
import org.jboss.as.deployment.unit.DeploymentUnitProcessor;
import org.jboss.modules.Module;
import org.jboss.modules.ModuleDependencySpec;
import org.jboss.modules.ModuleIdentifier;
import org.jboss.modules.ModuleLoadException;
import org.jboss.modules.ModuleSpec;
import org.jboss.modules.PathFilters;

import java.io.IOException;

/**
 * Processor responsible for creating a module for the deployment and attach it to the deployment. 
 *
 * @author John E. Bailey
 */
public class ModuleDeploymentProcessor implements DeploymentUnitProcessor {
    public static final long PRIORITY = DeploymentPhases.MODULARIZE.plus(102L);
    public static final AttachmentKey<Module> MODULE_ATTACHMENT_KEY = new AttachmentKey<Module>(Module.class);


    /**
     * Create a  module from the attached module config and attache the built module.. 
     *
     * @param context the deployment unit context
     * @throws DeploymentUnitProcessingException
     */
    public void processDeployment(DeploymentUnitContext context) throws DeploymentUnitProcessingException {
        if(context.getAttachment(MODULE_ATTACHMENT_KEY) != null)
            return; // Skip it if someone else attached a module

        final ModuleConfig moduleConfig = context.getAttachment(ModuleConfig.ATTACHMENT_KEY);
        if(moduleConfig == null)
            return; // Skip it if no module is attached

        final ModuleIdentifier moduleIdentifier = moduleConfig.getIdentifier();
        final ModuleSpec.Builder specBuilder = ModuleSpec.build(moduleIdentifier);
        for(ModuleConfig.ResourceRoot resource : moduleConfig.getResources()) {
            try {
                specBuilder.addResourceRoot(new VFSResourceLoader(specBuilder.getIdentifier(), resource.getRoot(), resource.getRootName()));
            } catch(IOException e) {
                throw new DeploymentUnitProcessingException("Failed to create VFSResourceLoader for root [" + resource.getRootName()+ "]", e, null);
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
        final DeploymentModuleLoader deploymentModuleLoader = context.getAttachment(DeploymentModuleLoaderProcessor.ATTACHMENT_KEY);
        deploymentModuleLoader.addModuleSpec(moduleSpec);

        try {
            final Module module = deploymentModuleLoader.loadModule(moduleIdentifier);
            context.putAttachment(MODULE_ATTACHMENT_KEY, module);
        } catch (ModuleLoadException e) {
            throw new DeploymentUnitProcessingException("Failed to load module: " + moduleIdentifier, e, null);
        }
    }
}
