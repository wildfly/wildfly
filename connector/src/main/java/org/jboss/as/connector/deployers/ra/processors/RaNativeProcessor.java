/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.connector.deployers.ra.processors;

import static org.jboss.as.connector.logging.ConnectorLogger.ROOT_LOGGER;

import java.io.File;
import java.util.List;
import java.util.Locale;

import org.jboss.as.connector.logging.ConnectorLogger;
import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.vfs.VirtualFile;
import org.jboss.vfs.VirtualFileFilter;

/**
 * Load native libraries for .rar deployments
 *
 * @author <a href="mailto:jesper.pedersen@jboss.org">Jesper Pedersen</a>
 */
public class RaNativeProcessor implements DeploymentUnitProcessor {

    /**
     * Construct a new instance.
     */
    public RaNativeProcessor() {
    }

    /**
     * Process a deployment for standard ra deployment files. Will parse the xml
     * file and attach a configuration discovered during processing.
     * @param phaseContext the deployment unit context
     * @throws DeploymentUnitProcessingException
     */
    @Override
    public void deploy(DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final VirtualFile deploymentRoot = phaseContext.getDeploymentUnit().getAttachment(Attachments.DEPLOYMENT_ROOT).getRoot();

        process(deploymentRoot);
    }

    public static void process(VirtualFile deploymentRoot) throws DeploymentUnitProcessingException {
        if (deploymentRoot == null || !deploymentRoot.exists())
            return;

        final String deploymentRootName = deploymentRoot.getName().toLowerCase(Locale.ENGLISH);
        if (!deploymentRootName.endsWith(".rar")) {
            return;
        }

        try {
            List<VirtualFile> libs = deploymentRoot.getChildrenRecursively(new LibraryFilter());

            if (libs != null && !libs.isEmpty()) {
                for (VirtualFile vf : libs) {
                    String fileName = vf.getName().toLowerCase(Locale.ENGLISH);
                    ROOT_LOGGER.tracef("Processing library: %s", fileName);

                    try {
                        File f = vf.getPhysicalFile();
                        System.load(f.getAbsolutePath());

                        ROOT_LOGGER.debugf("Loaded library: %s", f.getAbsolutePath());

                    } catch (Throwable t) {
                        ROOT_LOGGER.debugf("Unable to load library: %s", fileName);
                    }
                }
            }
        } catch (Exception e) {
            throw ConnectorLogger.ROOT_LOGGER.failedToLoadNativeLibraries(e);
        }
    }

    private static class LibraryFilter implements VirtualFileFilter {

        public boolean accepts(VirtualFile vf) {
            if (vf == null)
                return false;

            if (vf.isFile()) {
                String fileName = vf.getName().toLowerCase(Locale.ENGLISH);
                if (fileName.endsWith(".a") || fileName.endsWith(".so") || fileName.endsWith(".dll")) {
                    return true;
                }
            }

            return false;
        }
    }
}
