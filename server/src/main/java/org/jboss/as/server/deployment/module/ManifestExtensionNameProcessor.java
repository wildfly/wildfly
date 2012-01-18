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

import static java.util.jar.Attributes.Name.EXTENSION_NAME;
import static java.util.jar.Attributes.Name.IMPLEMENTATION_VENDOR_ID;
import static java.util.jar.Attributes.Name.IMPLEMENTATION_VERSION;
import static java.util.jar.Attributes.Name.SPECIFICATION_VERSION;

import java.util.jar.Attributes;
import java.util.jar.Manifest;

import org.jboss.as.server.ServerLogger;
import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.server.deployment.Services;

/**
 * A processor which reads the Extension-Name attribute from a manifest
 *
 * @author Stuart Douglas
 */
public final class ManifestExtensionNameProcessor implements DeploymentUnitProcessor {

    /** {@inheritDoc} */
    public void deploy(final DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();

        // we only want to process top level jar deployments
        if (deploymentUnit.getParent() != null) {
            return;
        }

        final ResourceRoot deploymentRoot = deploymentUnit.getAttachment(Attachments.DEPLOYMENT_ROOT);

        if (!deploymentRoot.getRoot().getName().endsWith(".jar")) {
            return;
        }
        // we are only interested in the root manifest
        // there should not be any additional resource roots for this type of deployment anyway
        final Manifest manifest = deploymentRoot.getAttachment(Attachments.MANIFEST);
        if (manifest == null) {
            return;
        }
        final Attributes mainAttributes = manifest.getMainAttributes();
        final String extensionName = mainAttributes.getValue(EXTENSION_NAME);
        ServerLogger.DEPLOYMENT_LOGGER.debugf("Found Extension-Name manifest entry %s in %s", extensionName, deploymentRoot.getRoot().getPathName());
        if (extensionName == null) {
            // no entry
            return;
        }
        final String implVersion = mainAttributes.getValue(IMPLEMENTATION_VERSION);
        final String implVendorId = mainAttributes.getValue(IMPLEMENTATION_VENDOR_ID);
        final String specVersion = mainAttributes.getValue(SPECIFICATION_VERSION);
        final ExtensionInfo info = new ExtensionInfo(extensionName, specVersion, implVersion, implVendorId);
        deploymentUnit.putAttachment(Attachments.EXTENSION_INFORMATION, info);

        phaseContext.addToAttachmentList(Attachments.NEXT_PHASE_DEPS, Services.JBOSS_DEPLOYMENT_EXTENSION_INDEX);
    }

    /** {@inheritDoc} */
    public void undeploy(final DeploymentUnit context) {
    }
}
