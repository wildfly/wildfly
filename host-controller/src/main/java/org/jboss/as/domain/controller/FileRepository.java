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

package org.jboss.as.domain.controller;

import java.io.File;


/**
 * A repository used to retrieve files in the domain directory structure.
 *
 * @author John Bailey
 */
public interface FileRepository {
    /**
     * Get a file relative to the repository root.
     *
     * @param relativePath Relative path to the file
     * @return The file at that path, or null if it is not found
     */
    File getFile(final String relativePath);


    /**
     * Get a file relative to the configuration root.
     *
     * @param relativePath Relative path to the file
     * @return The file at that path, or null if it is not found
     */
    File getConfigurationFile(final String relativePath);

    /**
     * Get the files associated with a given deployment.
     *
     * @param deploymentHash the hash of the deploymentUnit

     * @return the files associated with the deployment, or <code>null</code> if it is not found
     */
    File[] getDeploymentFiles(final byte[] deploymentHash);

    /**
     * Gets the directory under which files associated with a given deployment
     * would be found.
     *
     * @param deploymentHash the hash of the deploymentUnit

     * @return the directory. Will not be <code>null</code>, even if the
     *         deployment is unknown
     */
    File getDeploymentRoot(byte[] deploymentHash);
}
