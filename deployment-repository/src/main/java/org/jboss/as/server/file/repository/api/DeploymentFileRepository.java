/*
* JBoss, Home of Professional Open Source.
* Copyright 2011, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.as.server.file.repository.api;

import java.io.File;

/**
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
public interface DeploymentFileRepository {

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

    /**
     * Deletes a deployment from the local file system
     *
     * @param deploymentHash the hash of the deployment unit
     */
    void deleteDeployment(byte[] deploymentHash);

}
