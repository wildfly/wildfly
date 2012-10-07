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

package org.jboss.as.osgi.web;

import java.io.Closeable;
import java.io.IOException;
import java.util.Locale;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

import org.jboss.as.server.ServerMessages;
import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentMountProvider;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.server.deployment.MountExplodedMarker;
import org.jboss.as.server.deployment.MountType;
import org.jboss.as.server.deployment.module.DeploymentRootMountProcessor;
import org.jboss.as.server.deployment.module.ModuleRootMarker;
import org.jboss.as.server.deployment.module.MountHandle;
import org.jboss.as.server.deployment.module.ResourceRoot;
import org.jboss.vfs.VFS;
import org.jboss.vfs.VFSUtils;
import org.jboss.vfs.VirtualFile;

/**
 * Processor that remounts a deployment exploded.
 *
 * @author Thomas.Diesler@jboss.com
 * @since  11-Sep-2012
 *
 * @see {@link DeploymentRootMountProcessor}
 */
public class RemountDeploymentRootProcessor implements DeploymentUnitProcessor {

    public void deploy(final DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        DeploymentUnit depUnit = phaseContext.getDeploymentUnit();
        Manifest manifest = depUnit.getAttachment(Attachments.OSGI_MANIFEST);
        if (manifest == null || MountExplodedMarker.isMountExploded(depUnit))
            return;

        boolean remountExploded = false;

        // Check for WAB with *.jar extension
        String deploymentName = depUnit.getName().toLowerCase(Locale.ENGLISH);
        if (deploymentName.endsWith(".jar")) {
            Attributes mainAttributes = manifest.getMainAttributes();
            if (mainAttributes.getValue(WebExtension.WEB_CONTEXT_PATH) != null) {
                remountExploded = true;
            }
        }

        if (!remountExploded)
            return;

        // Close the already mounted root
        ResourceRoot resourceRoot = depUnit.getAttachment(Attachments.DEPLOYMENT_ROOT);
        Closeable handle = resourceRoot.getMountHandle();
        VFSUtils.safeClose(handle);

        VirtualFile deploymentContents = depUnit.getAttachment(Attachments.DEPLOYMENT_CONTENTS);
        DeploymentMountProvider deploymentMountProvider = depUnit.getAttachment(Attachments.SERVER_DEPLOYMENT_REPOSITORY);
        VirtualFile deploymentRoot = VFS.getChild("content/" + deploymentName);

        MountHandle mountHandle;
        try {
            handle = deploymentMountProvider.mountDeploymentContent(deploymentContents, deploymentRoot, MountType.EXPANDED);
            mountHandle = new MountHandle(handle);
        } catch (IOException e) {
            VFSUtils.safeClose(handle);
            throw ServerMessages.MESSAGES.deploymentMountFailed(e);
        }

        resourceRoot = new ResourceRoot(deploymentRoot, mountHandle);
        resourceRoot.putAttachment(Attachments.MANIFEST, manifest);
        ModuleRootMarker.mark(resourceRoot);

        depUnit.putAttachment(Attachments.DEPLOYMENT_ROOT, resourceRoot);
    }

    public void undeploy(final DeploymentUnit depUnit) {
    }
}
