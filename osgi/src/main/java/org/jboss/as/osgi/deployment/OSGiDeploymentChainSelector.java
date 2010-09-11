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

package org.jboss.as.osgi.deployment;

import java.util.jar.JarFile;
import java.util.jar.Manifest;

import org.jboss.as.deployment.attachment.ManifestAttachment;
import org.jboss.as.deployment.attachment.VirtualFileAttachment;
import org.jboss.as.deployment.chain.DeploymentChainProvider;
import org.jboss.as.deployment.unit.DeploymentUnitContext;
import org.jboss.logging.Logger;
import org.jboss.osgi.spi.util.BundleInfo;
import org.jboss.vfs.VirtualFile;

/**
 * Deployment chain selector which determines whether the OSGi deployment chain should handle this deployment.
 *
 * @author Thomas.Diesler@jboss.com
 */
public class OSGiDeploymentChainSelector implements DeploymentChainProvider.Selector {

    private static final Logger log = Logger.getLogger("org.jboss.as.deployment");

    private static final String ARCHIVE_EXTENSION = ".jar";

    /**
     * Determine where this deployment is supported by OSGi deployer chain.
     *
     * @param virtualFile The deployment file
     * @return true if this is s service deployment, and false if not
     */
    public boolean supports(final DeploymentUnitContext context) {

        VirtualFile virtualFile = VirtualFileAttachment.getVirtualFileAttachment(context);
        boolean jarMatch = virtualFile.getName().toLowerCase().endsWith(ARCHIVE_EXTENSION);
        boolean pathMatch = virtualFile.getChild(JarFile.MANIFEST_NAME).exists();
        if (!jarMatch || !pathMatch)
            return false;

        Manifest manifest = ManifestAttachment.getManifestAttachment(context);

        // Check whether this is a valid OSGi deployment
        return BundleInfo.isValidateBundleManifest(manifest);
    }
}
