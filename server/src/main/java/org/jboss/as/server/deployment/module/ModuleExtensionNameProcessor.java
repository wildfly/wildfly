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
import org.jboss.as.server.deployment.Services;
import org.jboss.as.server.moduleservice.ExtensionIndex;
import org.jboss.modules.ModuleIdentifier;
import org.jboss.msc.service.ServiceController;

/**
 * A processor which stores extension information for deployed libraries into the {@link org.jboss.as.server.moduleservice.ExtensionIndexService}.
 *
 * @author Stuart Douglas
 */
public final class ModuleExtensionNameProcessor implements DeploymentUnitProcessor {

    /** {@inheritDoc} */
    public void deploy(final DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();

        final ExtensionInfo extensionInfo = deploymentUnit.getAttachment(Attachments.EXTENSION_INFORMATION);
        if (extensionInfo == null) {
            return;
        }
        final ServiceController<?> extensionIndexController = phaseContext.getServiceRegistry().getRequiredService(
                Services.JBOSS_DEPLOYMENT_EXTENSION_INDEX);
        final ExtensionIndex extensionIndexService = (ExtensionIndex) extensionIndexController.getValue();
        final ModuleIdentifier moduleIdentifier = deploymentUnit.getAttachment(Attachments.MODULE_IDENTIFIER);
        extensionIndexService.addDeployedExtension(moduleIdentifier, extensionInfo);
    }

    /** {@inheritDoc} */
    public void undeploy(final DeploymentUnit deploymentUnit) {
        final ExtensionInfo extensionInfo = deploymentUnit.getAttachment(Attachments.EXTENSION_INFORMATION);
        if (extensionInfo == null) {
            return;
        }
        // we need to remove the extension on undeploy
        final ServiceController<?> extensionIndexController = deploymentUnit.getServiceRegistry().getRequiredService(
                Services.JBOSS_DEPLOYMENT_EXTENSION_INDEX);
        final ExtensionIndex extensionIndexService = (ExtensionIndex) extensionIndexController.getValue();
        final ModuleIdentifier moduleIdentifier = deploymentUnit.getAttachment(Attachments.MODULE_IDENTIFIER);

        extensionIndexService.removeDeployedExtension(extensionInfo.getName(), moduleIdentifier);

    }
}
