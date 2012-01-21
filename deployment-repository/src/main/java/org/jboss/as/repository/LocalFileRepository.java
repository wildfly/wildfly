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

package org.jboss.as.repository;

import java.io.File;

import org.jboss.msc.service.Service;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;


/**
 * A repository used to retrieve files in the domain directory structure.
 *
 * @author John Bailey
 */
public class LocalFileRepository extends LocalDeploymentFileRepository implements HostFileRepository, Service<HostFileRepository> {

    private final File repositoryRoot;
    private final File configurationRoot;

    public LocalFileRepository(final File repositoryRoot, final File deploymentRoot, final File configurationRoot) {
        super(deploymentRoot);
        this.repositoryRoot = repositoryRoot;
        this.configurationRoot = configurationRoot;
    }

    /** {@inheritDoc} */
    @Override
    public File getFile(final String relativePath) {
        return new File(repositoryRoot, relativePath);
    }

    /** {@inheritDoc} */
    @Override
    public File getConfigurationFile(String relativePath) {
        return new File(configurationRoot, relativePath);
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

    @Override
    public void start(StartContext context) throws StartException {
        // no-op
    }

    @Override
    public void stop(StopContext context) {
        // no-op
    }

    @Override
    public HostFileRepository getValue() throws IllegalStateException, IllegalArgumentException {
        return this;
    }
}
