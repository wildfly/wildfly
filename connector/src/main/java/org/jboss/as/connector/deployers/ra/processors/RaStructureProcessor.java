/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.connector.deployers.ra.processors;

import java.io.Closeable;
import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.jboss.as.connector.logging.ConnectorLogger;
import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.server.deployment.MountedDeploymentOverlay;
import org.jboss.as.server.deployment.module.ModuleRootMarker;
import org.jboss.as.server.deployment.module.ModuleSpecification;
import org.jboss.as.server.deployment.module.MountHandle;
import org.jboss.as.server.deployment.module.ResourceRoot;
import org.jboss.as.server.deployment.module.TempFileProviderService;
import org.jboss.vfs.VFS;
import org.jboss.vfs.VFSUtils;
import org.jboss.vfs.VirtualFile;
import org.jboss.vfs.VisitorAttributes;
import org.jboss.vfs.util.SuffixMatchFilter;

/**
 * Deployment processor used to determine the structure of RAR deployments.
 *
 * @author John Bailey
 */
public class RaStructureProcessor implements DeploymentUnitProcessor {
    private static final String RAR_EXTENSION = ".rar";
    private static final String JAR_EXTENSION = ".jar";

    private static final SuffixMatchFilter CHILD_ARCHIVE_FILTER = new SuffixMatchFilter(JAR_EXTENSION, VisitorAttributes.RECURSE_LEAVES_ONLY);

     private static Closeable NO_OP_CLOSEABLE = new Closeable() {
        public void close() throws IOException {
            // NO-OP
        }
    };

    public void deploy(DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        final ResourceRoot resourceRoot = deploymentUnit.getAttachment(Attachments.DEPLOYMENT_ROOT);
        if(resourceRoot == null) {
            return;
        }
        final VirtualFile deploymentRoot = resourceRoot.getRoot();
        if (deploymentRoot == null || !deploymentRoot.exists()) {
            return;
        }

        final String deploymentRootName = deploymentRoot.getName().toLowerCase(Locale.ENGLISH);
        if (!deploymentRootName.endsWith(RAR_EXTENSION)) {
            return;
        }


        final ModuleSpecification moduleSpecification = deploymentUnit.getAttachment(Attachments.MODULE_SPECIFICATION);
        moduleSpecification.setPublicModule(true);

        //this violates the spec, but everyone expects it to work
        ModuleRootMarker.mark(resourceRoot, true);
        Map<String, MountedDeploymentOverlay> overlays = deploymentUnit.getAttachment(Attachments.DEPLOYMENT_OVERLAY_LOCATIONS);

        try {
            final List<VirtualFile> childArchives = deploymentRoot.getChildren(CHILD_ARCHIVE_FILTER);

            for (final VirtualFile child : childArchives) {

                String relativeName = child.getPathNameRelativeTo(deploymentRoot);
                MountedDeploymentOverlay overlay = overlays.get(relativeName);
                Closeable closable = NO_OP_CLOSEABLE;
                if(overlay != null) {
                    overlay.remountAsZip(false);
                } else if(child.isFile()) {
                    closable = VFS.mountZip(child.getPhysicalFile(), child, TempFileProviderService.provider());
                }
                final MountHandle mountHandle = MountHandle.create(closable);
                final ResourceRoot childResource = new ResourceRoot(child, mountHandle);
                ModuleRootMarker.mark(childResource);
                deploymentUnit.addToAttachmentList(Attachments.RESOURCE_ROOTS, childResource);
                resourceRoot.addToAttachmentList(Attachments.INDEX_IGNORE_PATHS, child.getPathNameRelativeTo(deploymentRoot));
            }
        } catch (IOException e) {
            throw ConnectorLogger.ROOT_LOGGER.failedToProcessRaChild(e, deploymentRoot);
        }
    }

    public void undeploy(DeploymentUnit context) {
        final List<ResourceRoot> childRoots = context.removeAttachment(Attachments.RESOURCE_ROOTS);
        if(childRoots != null) {
            for(ResourceRoot childRoot : childRoots) {
                VFSUtils.safeClose(childRoot.getMountHandle());
            }
        }
    }
}
