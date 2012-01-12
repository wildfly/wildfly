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
package org.jboss.as.server.file.repository.impl;

import java.io.File;

import org.jboss.as.server.file.repository.api.DeploymentFileRepository;

/**
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
public class LocalDeploymentFileRepository implements DeploymentFileRepository {
    final File deploymentRoot;

    public LocalDeploymentFileRepository(final File deploymentRoot) {
        this.deploymentRoot = deploymentRoot;
    }

    /** {@inheritDoc} */
    @Override
    public File[] getDeploymentFiles(byte[] hash) {
        return getDeploymentRoot(hash).listFiles();
    }

    /** {@inheritDoc} */
    @Override
    public File getDeploymentRoot(byte[] hash) {
        if (hash == null || hash.length == 0) {
            return deploymentRoot;
        }
        String hex = HashUtil.bytesToHexString(hash);
        File first = new File(deploymentRoot, hex.substring(0,2));
        return new File(first, hex.substring(2));
    }

}
