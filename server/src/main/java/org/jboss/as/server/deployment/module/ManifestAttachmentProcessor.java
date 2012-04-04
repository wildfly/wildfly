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

import java.io.IOException;
import java.util.List;
import java.util.jar.Manifest;

import org.jboss.as.server.ServerMessages;
import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.server.deployment.DeploymentUtils;
import org.jboss.vfs.VFSUtils;
import org.jboss.vfs.VirtualFile;

/**
 * Deployment unit processor that attaches the deployment root manifest to the context.
 *
 * It does nothing if the manifest is already attached or there is no manifest in the deployment root file.
 *
 * @author Thomas.Diesler@jboss.com
 * @author Stuart Douglas
 * @since 14-Oct-2010
 */
public class ManifestAttachmentProcessor implements DeploymentUnitProcessor {

    /**
     * Process the deployment root for the manifest.
     *
     * @param phaseContext the deployment unit context
     * @throws DeploymentUnitProcessingException
     */
    public void deploy(DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {

        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        List<ResourceRoot> resourceRoots = DeploymentUtils.allResourceRoots(deploymentUnit);
        for (ResourceRoot resourceRoot : resourceRoots) {
            if (IgnoreMetaInfMarker.isIgnoreMetaInf(resourceRoot)) {
                continue;
            }
            Manifest manifest = resourceRoot.getAttachment(Attachments.MANIFEST);
            if (manifest != null)
                continue;

            final VirtualFile deploymentRoot = resourceRoot.getRoot();
            try {
                manifest = VFSUtils.getManifest(deploymentRoot);
                if (manifest != null)
                    resourceRoot.putAttachment(Attachments.MANIFEST, manifest);
            } catch (IOException e) {
                throw ServerMessages.MESSAGES.failedToGetManifest(deploymentRoot, e);
            }
        }
    }

    public void undeploy(final DeploymentUnit context) {
        List<ResourceRoot> resourceRoots = DeploymentUtils.allResourceRoots(context);
        for (ResourceRoot resourceRoot : resourceRoots) {
            resourceRoot.removeAttachment(Attachments.MANIFEST);
        }
    }
}
