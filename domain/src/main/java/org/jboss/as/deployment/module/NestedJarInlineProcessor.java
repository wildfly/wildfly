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

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.jboss.as.deployment.DeploymentPhases;
import org.jboss.as.deployment.unit.DeploymentUnitContext;
import org.jboss.as.deployment.unit.DeploymentUnitProcessingException;
import org.jboss.as.deployment.unit.DeploymentUnitProcessor;
import org.jboss.logging.Logger;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceListener;
import org.jboss.msc.service.StartException;
import org.jboss.vfs.VFS;
import org.jboss.vfs.VirtualFile;
import org.jboss.vfs.VirtualFileVisitor;
import org.jboss.vfs.VisitorAttributes;

/**
 * Processor responsible for discovering nested jars and mounting/attaching them to the deployment
 *
 * @author Jason T. Greene
 */
public class NestedJarInlineProcessor implements DeploymentUnitProcessor {
    private static final Logger log = Logger.getLogger("org.jboss.as.deployment");

    public static final long PRIORITY = DeploymentPhases.STRUCTURE.plus(100);


    /**
     * Mounts all nested jars inline with the mount of the deployment jar.
     *
     * @param context the deployment unit context
     * @throws DeploymentUnitProcessingException
     */
    public void processDeployment(DeploymentUnitContext context) throws DeploymentUnitProcessingException {
        final VirtualFile deploymentRoot = getVirtualFileAttachment(context);
        final List<VirtualFile> list = new ArrayList<VirtualFile>(1);
        try {
            deploymentRoot.visit(new VirtualFileVisitor() {
                public void visit(VirtualFile virtualFile) {
                    if (virtualFile.getName().endsWith(".jar")) {
                        list.add(virtualFile);
                    }
                }
                public VisitorAttributes getAttributes() {
                    return VisitorAttributes.RECURSE_LEAVES_ONLY;
                }
            });
        } catch (IOException e) {
            throw new DeploymentUnitProcessingException("Could not mount nested jars in deployment: " + deploymentRoot.getName(), e);
        }

        if (list.size() == 0)
            return;

        final NestedMounts mounts = new NestedMounts(list.size());
        for (VirtualFile file : list)
        try {
            MountHandle handle = new MountHandle(VFS.mountZip(file, file, TempFileProviderService.provider()));
            mounts.add(file, handle);
        } catch (IOException e) {
            log.warnf("Could not mount %s in deployment %s, skipping", file.getPathNameRelativeTo(deploymentRoot), deploymentRoot.getName());
        }

        context.putAttachment(NestedMounts.ATTACHMENT_KEY, mounts);
        context.getBatchServiceBuilder().addListener(new CloseListener(mounts.getClosables()));
    }

    static class CloseListener implements ServiceListener<Void> {
        private Closeable[] closeables;

        CloseListener(Closeable[] closeables) {
            this.closeables = closeables;
        }

        @Override
        public void serviceStopped(ServiceController<? extends Void> controller) {
            if (closeables != null) {
                for (Closeable close : closeables) {
                    try {
                        close.close();
                    } catch (IOException e) {
                        // Munch munch
                    }
                }
                closeables = null;
            }
        }

        public void listenerAdded(ServiceController<? extends Void> controller) {
        }

        public void serviceStarting(ServiceController<? extends Void> controller) {
        }

        public void serviceStarted(ServiceController<? extends Void> controller) {
        }

        public void serviceFailed(ServiceController<? extends Void> controller, StartException reason) {
        }

        public void serviceStopping(ServiceController<? extends Void> controller) {
        }

        public void serviceRemoved(ServiceController<? extends Void> controller) {
        }

    }
}
