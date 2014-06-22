package org.jboss.as.service;

import java.io.Closeable;
import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.server.deployment.MountedDeploymentOverlay;
import org.jboss.as.server.deployment.module.ModuleRootMarker;
import org.jboss.as.server.deployment.module.MountHandle;
import org.jboss.as.server.deployment.module.ResourceRoot;
import org.jboss.as.server.deployment.module.TempFileProviderService;
import org.jboss.as.service.logging.SarLogger;
import org.jboss.vfs.VFS;
import org.jboss.vfs.VFSUtils;
import org.jboss.vfs.VirtualFile;
import org.jboss.vfs.VisitorAttributes;
import org.jboss.vfs.util.SuffixMatchFilter;

/**
 * @author Tomasz Adamski
 */

public class SarStructureProcessor implements DeploymentUnitProcessor {

    private static final String SAR_EXTENSION = ".sar";
    private static final String JAR_EXTENSION = ".jar";

    private static final SuffixMatchFilter CHILD_ARCHIVE_FILTER = new SuffixMatchFilter(JAR_EXTENSION,
            VisitorAttributes.RECURSE_LEAVES_ONLY);

    private static Closeable NO_OP_CLOSEABLE = new Closeable() {
        public void close() throws IOException {
            // NO-OP
        }
    };

    @Override
    public void deploy(DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        final ResourceRoot resourceRoot = deploymentUnit.getAttachment(Attachments.DEPLOYMENT_ROOT);
        if (resourceRoot == null) {
            return;
        }
        final VirtualFile deploymentRoot = resourceRoot.getRoot();
        if (deploymentRoot == null || !deploymentRoot.exists()) {
            return;
        }

        final String deploymentRootName = deploymentRoot.getName().toLowerCase(Locale.ENGLISH);
        if (!deploymentRootName.endsWith(SAR_EXTENSION)) {
            return;
        }

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
                    closable = VFS.mountZip(child, child, TempFileProviderService.provider());
                }
                final MountHandle mountHandle = new MountHandle(closable);
                final ResourceRoot childResource = new ResourceRoot(child, mountHandle);
                ModuleRootMarker.mark(childResource);
                deploymentUnit.addToAttachmentList(Attachments.RESOURCE_ROOTS, childResource);
                resourceRoot.addToAttachmentList(Attachments.INDEX_IGNORE_PATHS, child.getPathNameRelativeTo(deploymentRoot));
            }
        } catch (IOException e) {
            throw SarLogger.ROOT_LOGGER.failedToProcessSarChild(e, deploymentRoot);
        }

    }

    @Override
    public void undeploy(DeploymentUnit context) {
        final List<ResourceRoot> childRoots = context.removeAttachment(Attachments.RESOURCE_ROOTS);
        if (childRoots != null) {
            for (ResourceRoot childRoot : childRoots) {
                VFSUtils.safeClose(childRoot.getMountHandle());
            }
        }
    }

}
