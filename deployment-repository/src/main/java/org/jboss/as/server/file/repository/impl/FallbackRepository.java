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

package org.jboss.as.server.file.repository.impl;

import java.io.File;

import org.jboss.as.server.file.repository.api.HostFileRepository;



/**
 * A repository that attempts to search two repositories
 *
 * @author Jason T. Greene
 */
public class FallbackRepository implements HostFileRepository {
    private final HostFileRepository primary;
    private final HostFileRepository secondary;

    public FallbackRepository(final HostFileRepository primary, final HostFileRepository secondary) {
        this.primary = primary;
        this.secondary = secondary;
    }

    /** {@inheritDoc} */
    @Override
    public File getFile(final String relativePath) {
        File file = primary.getFile(relativePath);
        return file != null ? file : secondary.getFile(relativePath);
    }

    /** {@inheritDoc} */
    @Override
    public File getConfigurationFile(String relativePath) {
        File file = primary.getConfigurationFile(relativePath);
        return file != null ? file : secondary.getConfigurationFile(relativePath);
    }

    /** {@inheritDoc} */
    @Override
    public File[] getDeploymentFiles(byte[] hash) {
        File[] files = primary.getDeploymentFiles(hash);
        return files != null ? files : secondary.getDeploymentFiles(hash);
    }

    /** {@inheritDoc} */
    @Override
    public File getDeploymentRoot(byte[] hash) {
        File file = primary.getDeploymentRoot(hash);
        return (file != null && file.exists()) ? file : secondary.getDeploymentRoot(hash);
    }
}
