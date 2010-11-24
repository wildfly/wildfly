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

import static org.jboss.as.deployment.attachment.VirtualFileAttachment.getVirtualFileAttachment;

import java.io.IOException;
import java.util.jar.Manifest;

import org.jboss.as.deployment.attachment.ManifestAttachment;
import org.jboss.as.deployment.unit.DeploymentUnitContext;
import org.jboss.as.deployment.unit.DeploymentUnitProcessingException;
import org.jboss.as.deployment.unit.DeploymentUnitProcessor;
import org.jboss.vfs.VFSUtils;
import org.jboss.vfs.VirtualFile;

/**
 * Deployment unit processor that attaches the deployment root manifest to the context.
 *
 * It does nothing if the manifest is already attached or there is no manifest in the deployment root file.
 *
 * @author Thomas.Diesler@jboss.com
 * @since 14-Oct-2010
 */
public class ManifestAttachmentProcessor implements DeploymentUnitProcessor {

    /**
     * Process the deployment root for the manifest.
     *
     * @param context the deployment unit context
     * @throws DeploymentUnitProcessingException
     */
    public void processDeployment(DeploymentUnitContext context) throws DeploymentUnitProcessingException {

        // Do nothing if the manifest is already available
        Manifest manifest = ManifestAttachment.getManifestAttachment(context);
        if (manifest != null)
            return;

        final VirtualFile deploymentRoot = getVirtualFileAttachment(context);
        try {
            manifest = VFSUtils.getManifest(deploymentRoot);
            if (manifest != null)
                ManifestAttachment.attachManifest(context, manifest);
        } catch (IOException e) {
            throw new DeploymentUnitProcessingException("Failed to get manifest for deployment " + deploymentRoot, e);
        }
    }
}
