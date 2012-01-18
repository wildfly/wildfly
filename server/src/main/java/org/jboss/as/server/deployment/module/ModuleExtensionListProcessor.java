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

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.jboss.as.server.ServerLogger;
import org.jboss.as.server.deployment.AttachmentList;
import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.server.deployment.DeploymentUtils;
import org.jboss.as.server.deployment.Services;
import org.jboss.as.server.moduleservice.ExtensionIndex;
import org.jboss.as.server.moduleservice.ServiceModuleLoader;
import org.jboss.modules.ModuleIdentifier;
import org.jboss.modules.ModuleLoader;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;

/**
 * A processor which adds extension-list resource roots.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public final class ModuleExtensionListProcessor implements DeploymentUnitProcessor {

    public ModuleExtensionListProcessor() {
    }

    /** {@inheritDoc} */
    public void deploy(final DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        final ModuleSpecification moduleSpecification = deploymentUnit.getAttachment(Attachments.MODULE_SPECIFICATION);
        final ModuleLoader moduleLoader = deploymentUnit.getAttachment(Attachments.SERVICE_MODULE_LOADER);
        final ServiceController<?> controller = phaseContext.getServiceRegistry().getRequiredService(Services.JBOSS_DEPLOYMENT_EXTENSION_INDEX);
        final ExtensionIndex index = (ExtensionIndex) controller.getValue();
        final List<ResourceRoot> allResourceRoots = DeploymentUtils.allResourceRoots(deploymentUnit);
        final Set<ServiceName> nextPhaseDeps = new HashSet<ServiceName>();
        for (ResourceRoot resourceRoot : allResourceRoots) {
            final AttachmentList<ExtensionListEntry> entries = resourceRoot.getAttachment(Attachments.EXTENSION_LIST_ENTRIES);
            if (entries != null) {

                for (ExtensionListEntry entry : entries) {
                    final ModuleIdentifier extension = index.findExtension(entry.getName(), entry.getSpecificationVersion(),
                            entry.getImplementationVersion(), entry.getImplementationVendorId());

                    if (extension != null) {
                        moduleSpecification.addLocalDependency(new ModuleDependency(moduleLoader, extension, false, false, true));
                        nextPhaseDeps.add(ServiceModuleLoader.moduleSpecServiceName(extension));
                        nextPhaseDeps.add(ServiceModuleLoader.moduleSpecServiceName(extension));
                    } else {
                        ServerLogger.DEPLOYMENT_LOGGER.cannotFindExtensionListEntry(entry, resourceRoot);
                    }
                }
            }
        }

        final List<AdditionalModuleSpecification> additionalModules = deploymentUnit.getAttachment(Attachments.ADDITIONAL_MODULES);
        if (additionalModules != null) {
            for (AdditionalModuleSpecification additionalModule : additionalModules) {
                for (ResourceRoot resourceRoot : additionalModule.getResourceRoots()) {
                    final AttachmentList<ExtensionListEntry> entries = resourceRoot
                            .getAttachment(Attachments.EXTENSION_LIST_ENTRIES);
                    if (entries != null) {

                        for (ExtensionListEntry entry : entries) {
                            final ModuleIdentifier extension = index.findExtension(entry.getName(), entry
                                    .getSpecificationVersion(), entry.getImplementationVersion(), entry
                                    .getImplementationVendorId());
                            if (extension != null) {
                                moduleSpecification.addLocalDependency(new ModuleDependency(moduleLoader, extension, false, false,
                                        true));
                                nextPhaseDeps.add(ServiceModuleLoader.moduleSpecServiceName(extension));
                            } else {
                                ServerLogger.DEPLOYMENT_LOGGER.cannotFindExtensionListEntry(entry, resourceRoot);
                            }
                        }
                    }
                }
            }
        }
        for (ServiceName dep : nextPhaseDeps) {
            phaseContext.addToAttachmentList(Attachments.NEXT_PHASE_DEPS, dep);
        }

    }

    /** {@inheritDoc} */
    public void undeploy(final DeploymentUnit context) {
    }
}
