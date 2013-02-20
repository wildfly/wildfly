/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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

package org.jboss.as.undertow.deployment;

import java.io.File;
import java.io.IOException;

import io.undertow.servlet.api.ResourceLoader;
import org.jboss.vfs.VirtualFile;

/**
 * @author Stuart Douglas
 */
public class DeploymentResourceLoader implements ResourceLoader {

    private final VirtualFile deploymentRoot;

    public DeploymentResourceLoader(final VirtualFile deploymentRoot) {
        this.deploymentRoot = deploymentRoot;
    }

    @Override
    public File getResource(final String resource) {

        try {
            final File child = deploymentRoot.getPhysicalFile();
            File ret = new File(child.getAbsolutePath() + resource);
            if(ret.exists()) {
                return ret;
            } else {
                return null;
            }
        } catch (IOException e) {
            return null;
        }
    }
}
