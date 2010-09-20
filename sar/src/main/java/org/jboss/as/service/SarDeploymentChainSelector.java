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

package org.jboss.as.service;

import org.jboss.as.deployment.chain.DeploymentChainProvider;
import org.jboss.vfs.VirtualFile;

/**
 * Deployment chain selector which determines whether the sar deployment chain should handle this deployment.
 *
 * @author John E. Bailey
 */
public class SarDeploymentChainSelector implements DeploymentChainProvider.Selector {
    private static final String ARCHIVE_EXTENSION = ".sar";

    /**
     * Determine where this deployment is supported by sar deployer chain.
     *
     * @param virtualFile The deployment file
     * @return true if this is s service deployment, and false if not
     */
    public boolean supports(final VirtualFile virtualFile) {
        return virtualFile.getName().toLowerCase().endsWith(ARCHIVE_EXTENSION);
    }
}
