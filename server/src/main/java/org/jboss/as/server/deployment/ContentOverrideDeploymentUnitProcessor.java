/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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

package org.jboss.as.server.deployment;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.jboss.as.server.ServerLogger;
import org.jboss.as.server.ServerMessages;
import org.jboss.as.server.deployment.module.FilterSpecification;
import org.jboss.as.server.deployment.module.MountHandle;
import org.jboss.as.server.deployment.module.ResourceRoot;
import org.jboss.as.server.deployment.module.TempFileProviderService;
import org.jboss.as.server.deploymentoverlay.service.ContentService;
import org.jboss.as.server.deploymentoverlay.service.DeploymentOverlayIndexService;
import org.jboss.as.server.deploymentoverlay.service.DeploymentOverlayService;
import org.jboss.vfs.VFS;
import org.jboss.vfs.VirtualFile;

/**
 * Deployment unit processor that adds content overrides to the VFS filesystem
 *
 * @author Stuart Douglas
 */
public class ContentOverrideDeploymentUnitProcessor implements DeploymentUnitProcessor {

    private final DeploymentOverlayIndexService indexService;

    private static final AttachmentKey<AttachmentList<Closeable>> MOUNTED_FILES = AttachmentKey.createList(Closeable.class);

    public ContentOverrideDeploymentUnitProcessor(final DeploymentOverlayIndexService indexService) {
        this.indexService = indexService;
    }

    @Override
    public void deploy(final DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        if (deploymentUnit.getParent() != null) {
            return;
        }
        final ResourceRoot deploymentRoot = deploymentUnit.getAttachment(Attachments.DEPLOYMENT_ROOT);

        //resource roots require special handling
        final Map<String, ResourceRoot> resourceRootMap = new HashMap<String, ResourceRoot>();

        final AttachmentList<ResourceRoot> rootList = deploymentUnit.getAttachment(Attachments.RESOURCE_ROOTS);

        if (rootList != null) {
            for (ResourceRoot root : rootList) {
                resourceRootMap.put(root.getRoot().getPathNameRelativeTo(deploymentRoot.getRoot()), root);
            }
        }

        //exploded is true if this is a zip deployment that has been mounted exploded
        final boolean exploded = MountExplodedMarker.isMountExploded(deploymentUnit) && !ExplodedDeploymentMarker.isExplodedDeployment(deploymentUnit);
        final Set<String> paths = new HashSet<String>();
        for (final DeploymentOverlayService deploymentOverlay : indexService.getOverrides(deploymentUnit.getName())) {
            for (final ContentService override : deploymentOverlay.getContentServices()) {

                try {
                    if (!paths.contains(override.getPath())) {
                        VirtualFile mountPoint = deploymentRoot.getRoot().getChild(override.getPath());
                        if (resourceRootMap.containsKey(override.getPath())) {
                            ResourceRoot existingRoot = resourceRootMap.get(override.getPath());
                            //unmount the existing root
                            close(existingRoot.getMountHandle());

                            //and remove it from the root list
                            if(rootList != null) {
                                rootList.remove(existingRoot);
                            }

                            //mount our override
                            Closeable handle = VFS.mountZip(override.getContentHash(), mountPoint, TempFileProviderService.provider());
                            deploymentUnit.addToAttachmentList(MOUNTED_FILES, handle);

                            //now recreate the root
                            ResourceRoot newRoot = new ResourceRoot(override.getContentHash().getName(), mountPoint, new MountHandle(handle));
                            for(AttachmentKey<?> key : existingRoot.attachmentKeys()) {
                                newRoot.putAttachment((AttachmentKey<Object>) key, existingRoot.getAttachment(key));
                            }
                            for(FilterSpecification filter : existingRoot.getExportFilters()) {
                                newRoot.getExportFilters().add(filter);
                            }
                            deploymentUnit.addToAttachmentList(Attachments.RESOURCE_ROOTS, newRoot);
                        } else {
                            paths.add(override.getPath());
                            if (exploded) {
                                //for exploded deployments we simply copy the file
                                copyFile(override.getContentHash().getPhysicalFile(), mountPoint.getPhysicalFile());

                            } else {
                                Closeable handle = VFS.mountReal(override.getContentHash().getPhysicalFile(), mountPoint);
                                deploymentUnit.addToAttachmentList(MOUNTED_FILES, handle);

                            }
                        }
                    }
                } catch (IOException e) {
                    throw ServerMessages.MESSAGES.deploymentOverlayFailed(e, deploymentOverlay.getName(), override.getPath());
                }
            }
        }
    }

    @Override
    public void undeploy(final DeploymentUnit context) {
        for (Closeable closable : context.getAttachmentList(MOUNTED_FILES)) {
            try {
                closable.close();
            } catch (IOException e) {
                ServerLogger.DEPLOYMENT_LOGGER.failedToUnmountContentOverride(e);
            }
        }

    }

    public static void copyFile(final File src, final File dest) throws IOException {
        final InputStream in = new BufferedInputStream(new FileInputStream(src));
        try {
            copyFile(in, dest);
        } finally {
            close(in);
        }
    }

    public static void copyFile(final InputStream in, final File dest) throws IOException {
        dest.getParentFile().mkdirs();
        byte[] buff = new byte[1024];
        final OutputStream out = new BufferedOutputStream(new FileOutputStream(dest));
        try {
            int i = in.read(buff);
            while (i > 0) {
                out.write(buff, 0, i);
                i = in.read(buff);
            }
        } finally {
            close(out);
        }
    }


    public static void close(Closeable closeable) {
        try {
            closeable.close();
        } catch (IOException ignore) {
        }
    }
}
