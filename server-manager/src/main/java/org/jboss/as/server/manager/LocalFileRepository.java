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

package org.jboss.as.server.manager;

import java.io.File;


/**
 * A repository used to retrieve files in the domain directory structure.
 *
 * @author John Bailey
 */
public class LocalFileRepository implements FileRepository {
    private final File repositoryRoot;
    private final File deploymentRoot;
    private final File configurationRoot;

    public LocalFileRepository(final ServerManagerEnvironment environment) {
        this.repositoryRoot = environment.getDomainBaseDir();
        this.deploymentRoot = environment.getDomainDeploymentDir();
        this.configurationRoot = environment.getDomainConfigurationDir();
    }

    /** {@inheritDoc} */
    public File getFile(final String relativePath) {
        return new File(repositoryRoot, relativePath);
    }

    /** {@inheritDoc} */
    public File getConfigurationFile(String relativePath) {
        return new File(configurationRoot, relativePath);
    }

    /** {@inheritDoc} */
    public File getDeploymentFile(String relativePath) {
        return new File(deploymentRoot, relativePath);
    }
}
