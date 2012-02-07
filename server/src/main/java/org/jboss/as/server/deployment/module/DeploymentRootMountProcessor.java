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

import java.io.Closeable;
import java.io.IOException;

import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentMountProvider;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.server.deployment.MountExplodedMarker;
import org.jboss.as.server.deployment.MountType;
import org.jboss.vfs.VFS;
import org.jboss.vfs.VFSUtils;
import org.jboss.vfs.VirtualFile;

/**
 * Deployment processor responsible for mounting and attaching the resource root for this deployment.
 *
 * @author John Bailey
 */
public class DeploymentRootMountProcessor implements DeploymentUnitProcessor {

    public void deploy(DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        if(deploymentUnit.getAttachment(Attachments.DEPLOYMENT_ROOT) != null) {
            return;
        }
        final DeploymentMountProvider deploymentMountProvider = deploymentUnit.getAttachment(Attachments.SERVER_DEPLOYMENT_REPOSITORY);
        if(deploymentMountProvider == null) {
            throw new DeploymentUnitProcessingException("No deployment repository available.");
        }

        final String deploymentName = deploymentUnit.getName();
        final VirtualFile deploymentContents = deploymentUnit.getAttachment(Attachments.DEPLOYMENT_CONTENTS);

        // internal deployments do not have any contents, so there is nothing to mount
        if (deploymentContents == null)
            return;

        final VirtualFile deploymentRoot;
        final MountHandle mountHandle;
        if (deploymentContents.isDirectory()) {
            // use the contents directly
            deploymentRoot = deploymentContents;
            // nothing was mounted
            mountHandle = null;
        } else {
            // The mount point we will use for the repository file
            deploymentRoot = VFS.getChild("content/" + deploymentName);

            boolean failed = false;
            Closeable handle = null;
            try {
                final boolean mountExploded = MountExplodedMarker.isMountExploded(deploymentUnit);
                final MountType type;
                if(mountExploded) {
                    type = MountType.EXPANDED;
                } else if (deploymentName.endsWith(".xml")) {
                    type = MountType.REAL;
                } else {
                    type = MountType.ZIP;
                }
                handle = deploymentMountProvider.mountDeploymentContent(deploymentContents, deploymentRoot, type);
                mountHandle = new MountHandle(handle);
            } catch (IOException e) {
                failed = true;
                throw new DeploymentUnitProcessingException("Failed to mount deployment content", e);
            } finally {
                if(failed) {
                    VFSUtils.safeClose(handle);
                }
            }
        }
        final ResourceRoot resourceRoot = new ResourceRoot(deploymentRoot, mountHandle);
        ModuleRootMarker.mark(resourceRoot);
        deploymentUnit.putAttachment(Attachments.DEPLOYMENT_ROOT, resourceRoot);
        deploymentUnit.putAttachment(Attachments.MODULE_SPECIFICATION, new ModuleSpecification());
    }

    public void undeploy(DeploymentUnit context) {
        final ResourceRoot resourceRoot = context.removeAttachment(Attachments.DEPLOYMENT_ROOT);
        if (resourceRoot != null) {
            final Closeable mountHandle = resourceRoot.getMountHandle();
            VFSUtils.safeClose(mountHandle);
        }
    }
}
