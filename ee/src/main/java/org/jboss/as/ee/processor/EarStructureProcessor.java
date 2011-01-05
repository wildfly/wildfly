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

package org.jboss.as.ee.processor;

import java.io.Closeable;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import static org.jboss.as.ee.processor.EarDeploymentMarker.markDeployment;
import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.server.deployment.module.MountHandle;
import org.jboss.as.server.deployment.module.ResourceRoot;
import org.jboss.as.server.deployment.module.TempFileProviderService;
import org.jboss.vfs.VFS;
import org.jboss.vfs.VFSUtils;
import org.jboss.vfs.VirtualFile;
import org.jboss.vfs.VisitorAttributes;
import org.jboss.vfs.util.SuffixMatchFilter;

/**
 * Deployment processor responsible for detecting EAR deployments and putting setting up the basic structure.
 *
 * @author John Bailey
 */
public class EarStructureProcessor implements DeploymentUnitProcessor {
    private static final String EAR_EXTENSION = ".ear";
    private static final Set<String> CHILD_ARCHIVE_EXTENSIONS = new HashSet<String>();

    static {
        CHILD_ARCHIVE_EXTENSIONS.add(".jar");
        CHILD_ARCHIVE_EXTENSIONS.add(".war");
        CHILD_ARCHIVE_EXTENSIONS.add(".sar");
    }

    private static Closeable NO_OP_CLOSEABLE = new Closeable() {
        public void close() throws IOException {
            // NO-OP
        }
    };

    public void deploy(DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();

        final ResourceRoot resourceRoot = phaseContext.getDeploymentUnit().getAttachment(Attachments.DEPLOYMENT_ROOT);
        final VirtualFile virtualFile = resourceRoot.getRoot();

        // Make sure this is an EAR deployment
        if (!virtualFile.getName().toLowerCase().endsWith(EAR_EXTENSION)) {
            return;
        }

        //  Let other processors know this is an EAR deployment
        markDeployment(deploymentUnit);

        // Process all the children
        try {
            final List<VirtualFile> childArchives = virtualFile.getChildren(new SuffixMatchFilter(CHILD_ARCHIVE_EXTENSIONS, VisitorAttributes.RECURSE_LEAVES_ONLY));

            for (final VirtualFile child : childArchives) {
                final Closeable closable;
                if (child.isFile()) {
                    closable = VFS.mountZip(child, child, TempFileProviderService.provider());
                } else {
                    closable = NO_OP_CLOSEABLE;
                }
                final MountHandle mountHandle = new MountHandle(closable);
                final ResourceRoot childResource = new ResourceRoot(child, mountHandle, false);
                deploymentUnit.addToAttachmentList(Attachments.EAR_CHILD_ROOTS, childResource);
            }
        } catch (IOException e) {
            throw new DeploymentUnitProcessingException("Failed to process children for EAR [" + virtualFile + "]", e);
        }
    }

    public void undeploy(DeploymentUnit context) {
        final List<ResourceRoot> childRoots = context.removeAttachment(Attachments.EAR_CHILD_ROOTS);
        if(childRoots != null) {
            for(ResourceRoot childRoot : childRoots) {
                VFSUtils.safeClose(childRoot.getMountHandle());
            }
        }
    }
}
