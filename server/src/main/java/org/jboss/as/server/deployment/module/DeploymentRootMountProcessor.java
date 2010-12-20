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
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.server.deployment.api.ServerDeploymentRepository;
import org.jboss.vfs.VFS;
import org.jboss.vfs.VFSUtils;
import org.jboss.vfs.VirtualFile;

/**
 * @author John Bailey
 */
public class DeploymentRootMountProcessor implements DeploymentUnitProcessor {

    public void deploy(DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final ServerDeploymentRepository serverDeploymentRepository = phaseContext.getDeploymentUnit().getAttachment(Attachments.SERVER_DEPLOYMENT_REPOSITORY);
        if(serverDeploymentRepository == null) {
            throw new DeploymentUnitProcessingException("No deployment repository available.");
        }
        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        final String deploymentName = deploymentUnit.getName();
        final String deploymentRuntimeName = deploymentUnit.getAttachment(Attachments.RUNTIME_NAME);
        final byte[] deploymentHash = deploymentUnit.getAttachment(Attachments.DEPLOYMENT_HASH);

        // The mount point we will use for the repository file
        final VirtualFile deploymentRoot = VFS.getChild("content/" + deploymentRuntimeName);

        boolean failed = false;
        Closeable handle = null;
        try {
            handle = serverDeploymentRepository.mountDeploymentContent(deploymentName, deploymentRuntimeName, deploymentHash, deploymentRoot);
            final MountHandle mountHandle = new MountHandle(handle);
            final ResourceRoot resourceRoot = new ResourceRoot(deploymentRoot, mountHandle, false);
            deploymentUnit.putAttachment(Attachments.DEPLOYMENT_ROOT, resourceRoot);
            deploymentUnit.putAttachment(Attachments.DEPLOYMENT_ROOT_MOUNT_HANDLE, mountHandle);
        } catch (IOException e) {
            failed = true;
            throw new DeploymentUnitProcessingException("Failed to mount deployment content", e);
        } finally {
            if(failed) {
                VFSUtils.safeClose(handle);
            }
        }
    }

    public void undeploy(DeploymentUnit context) {
        final ResourceRoot resourceRoot = context.getAttachment(Attachments.DEPLOYMENT_ROOT);
        if (resourceRoot != null) {
            final Closeable mountHandle = resourceRoot.getMountHandle();
            VFSUtils.safeClose(mountHandle);
        }
    }
}
