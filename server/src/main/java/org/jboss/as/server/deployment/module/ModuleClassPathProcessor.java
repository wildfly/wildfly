/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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

import java.util.List;

import org.jboss.as.server.deployment.AttachmentList;
import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.modules.ModuleIdentifier;
import org.jboss.modules.ModuleLoader;

/**
 * The processor which adds {@code MANIFEST.MF} {@code Class-Path} entries to the module configuration.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 * @author Stuart Douglas
 */
public final class ModuleClassPathProcessor implements DeploymentUnitProcessor {

    public void deploy(final DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        final ModuleSpecification moduleSpecification = deploymentUnit.getAttachment(Attachments.MODULE_SPECIFICATION);

        final ModuleLoader moduleLoader = deploymentUnit.getAttachment(Attachments.SERVICE_MODULE_LOADER);
        final AttachmentList<ModuleIdentifier> entries = deploymentUnit.getAttachment(Attachments.CLASS_PATH_ENTRIES);
        if (entries != null) {
            for (ModuleIdentifier entry : entries) {
                //class path items are always exported to make transitive dependencies work
                moduleSpecification.addLocalDependency(new ModuleDependency(moduleLoader, entry, false, true, true));
            }
        }

        final List<AdditionalModuleSpecification> additionalModules = deploymentUnit.getAttachment(Attachments.ADDITIONAL_MODULES);
        if (additionalModules != null) {
            for (AdditionalModuleSpecification additionalModule : additionalModules) {
                final AttachmentList<ModuleIdentifier> dependencies = additionalModule.getAttachment(Attachments.CLASS_PATH_ENTRIES);
                if (dependencies == null || dependencies.isEmpty()) {
                    continue;
                }
                // additional modules export any class-path entries
                // this means that a module that references the additional module
                // gets access to the transitive closure of its call-path entries
                for (ModuleIdentifier entry : dependencies) {
                    additionalModule.addLocalDependency(new ModuleDependency(moduleLoader, entry, false, true, true));
                }
                // add a dependency on the top ear itself for good measure
                additionalModule.addLocalDependency(new ModuleDependency(moduleLoader, deploymentUnit.getAttachment(Attachments.MODULE_IDENTIFIER), false, false, true));
            }
        }
    }

    public void undeploy(final DeploymentUnit context) {
    }
}
