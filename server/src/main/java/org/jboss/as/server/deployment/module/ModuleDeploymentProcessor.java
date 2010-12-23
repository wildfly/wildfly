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

package org.jboss.as.server.deployment.module;

import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.modules.Module;
import org.jboss.modules.ModuleIdentifier;
import org.jboss.modules.ModuleLoadException;

/**
 * Processor responsible for creating a module for the deployment and attach it to the deployment.
 *
 * @author John E. Bailey
 * @author Jason T. Greene
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public class ModuleDeploymentProcessor implements DeploymentUnitProcessor {

    /**
     * Create a  module from the attached module config and attache the built module..
     *
     * @param phaseContext the deployment unit context
     * @throws DeploymentUnitProcessingException
     *
     */
    public void deploy(DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        final DeploymentModuleLoader deploymentModuleLoader = deploymentUnit.getAttachment(Attachments.DEPLOYMENT_MODULE_LOADER);
        if (deploymentModuleLoader == null) {
            return;
        }
        final ModuleIdentifier moduleIdentifier = ModuleIdentifier.create("deployment." + deploymentUnit.getName());
        try {
            final Module module = deploymentModuleLoader.loadModule(moduleIdentifier);
            deploymentModuleLoader.relinkModule(module);
            deploymentUnit.putAttachment(Attachments.MODULE, module);
        } catch (ModuleLoadException e) {
            throw new DeploymentUnitProcessingException("Failed to load module: " + moduleIdentifier, e);
        }
    }

    public void undeploy(DeploymentUnit context) {
        final DeploymentModuleLoader deploymentModuleLoader = context.getAttachment(Attachments.DEPLOYMENT_MODULE_LOADER);
        if (deploymentModuleLoader == null) {
            return;
        }
        final Module module = context.getAttachment(Attachments.MODULE);
        if (module != null) {
            deploymentModuleLoader.removeModule(module);
            context.removeAttachment(Attachments.MODULE);
        }
    }
}
