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

package org.jboss.as.connector.deployers.ra.processors;

import static org.jboss.as.connector.logging.ConnectorLogger.ROOT_LOGGER;

import java.io.File;
import java.util.List;
import java.util.Locale;

import org.jboss.as.connector.logging.ConnectorLogger;
import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
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

            if (libs != null && libs.size() > 0) {
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

    public void undeploy(final DeploymentUnit context) {
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
