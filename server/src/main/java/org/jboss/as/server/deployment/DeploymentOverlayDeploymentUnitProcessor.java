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
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jboss.as.repository.ContentRepository;
import org.jboss.as.server.ServerLogger;
import org.jboss.as.server.ServerMessages;
import org.jboss.as.server.deployment.module.ResourceRoot;
import org.jboss.as.server.deployment.module.TempFileProviderService;
import org.jboss.as.server.deploymentoverlay.DeploymentOverlayIndex;
import org.jboss.vfs.VFS;
import org.jboss.vfs.VirtualFile;

/**
 * Deployment unit processor that adds content overrides to the VFS filesystem
 *
 * @author Stuart Douglas
 */
public class DeploymentOverlayDeploymentUnitProcessor implements DeploymentUnitProcessor {

    private final ContentRepository contentRepository;

    private static final AttachmentKey<AttachmentList<Closeable>> MOUNTED_FILES = AttachmentKey.createList(Closeable.class);

    public DeploymentOverlayDeploymentUnitProcessor(final ContentRepository contentRepository) {
        this.contentRepository = contentRepository;
    }

    @Override
    public void deploy(final DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        final ResourceRoot deploymentRoot = deploymentUnit.getAttachment(Attachments.DEPLOYMENT_ROOT);

        Map<String, MountedDeploymentOverlay> mounts = new HashMap<String, MountedDeploymentOverlay>();
        deploymentUnit.putAttachment(Attachments.DEPLOYMENT_OVERLAY_LOCATIONS, mounts);

        //resource roots require special handling
        final Map<String, ResourceRoot> resourceRootMap = new HashMap<String, ResourceRoot>();

        final AttachmentList<ResourceRoot> rootList = deploymentUnit.getAttachment(Attachments.RESOURCE_ROOTS);

        if (rootList != null) {
            for (ResourceRoot root : rootList) {
                resourceRootMap.put(root.getRoot().getPathNameRelativeTo(deploymentRoot.getRoot()), root);
            }
        }
        DeploymentOverlayIndex overlays = deploymentUnit.getAttachment(Attachments.DEPLOYMENT_OVERLAY_INDEX);
        if(overlays == null) {
            return;
        }
        //exploded is true if this is a zip deployment that has been mounted exploded
        final boolean exploded = MountExplodedMarker.isMountExploded(deploymentUnit) && !ExplodedDeploymentMarker.isExplodedDeployment(deploymentUnit);
        final Set<String> paths = new HashSet<String>();
        for (final Map.Entry<String, byte[]> entry : overlays.getOverlays(deploymentUnit.getName()).entrySet()) {

                String path = entry.getKey();
                if(path.startsWith("/")) {
                    path = path.substring(1);
                }
                try {
                    if (!paths.contains(path)) {
                        VirtualFile mountPoint = deploymentRoot.getRoot().getChild(path);

                        paths.add(path);
                        VirtualFile content = contentRepository.getContent(entry.getValue());
                        if (exploded) {
                            //for exploded deployments we simply copy the file

                            copyFile(content.getPhysicalFile(), mountPoint.getPhysicalFile());

                        } else {
                            VirtualFile parent = mountPoint.getParent();
                            List<VirtualFile> createParents = new ArrayList<VirtualFile>();
                            while (!parent.exists()) {
                                createParents.add(parent);
                                parent = parent.getParent();
                            }
                            Collections.reverse(createParents);
                            for(VirtualFile file : createParents) {
                                Closeable closable = VFS.mountTemp(file, TempFileProviderService.provider());
                                deploymentUnit.addToAttachmentList(MOUNTED_FILES, closable);
                            }
                            Closeable handle = VFS.mountReal(content.getPhysicalFile(), mountPoint);
                            MountedDeploymentOverlay mounted = new MountedDeploymentOverlay(handle, content.getPhysicalFile(),  mountPoint, TempFileProviderService.provider());
                            deploymentUnit.addToAttachmentList(MOUNTED_FILES, mounted);
                            mounts.put(path, mounted);

                        }
                    }
                } catch (IOException e) {
                    throw ServerMessages.MESSAGES.deploymentOverlayFailed(e, entry.getKey(), path);
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
