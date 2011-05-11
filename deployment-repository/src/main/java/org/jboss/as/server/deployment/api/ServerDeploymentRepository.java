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

package org.jboss.as.server.deployment.api;

import org.jboss.msc.service.ServiceName;
import org.jboss.vfs.VirtualFile;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;

/**
 * A server-level repository for deployment content.
 *
 * @author Brian Stansberry
 */
public interface ServerDeploymentRepository {

    /**
     * Standard ServiceName under which a service controller for an instance of
     * {@code Service<ServerDeploymentRepository> would be registered.
     */
    ServiceName SERVICE_NAME = ServiceName.JBOSS.append("deployment-repository");

    /**
     * Add an external local file reference.
     *
     * @param file the local file
     * @return the hash of the content that will be used as an internal identifier
     *         for the content. Will not be <code>null</code>
     */
    byte[] addExternalFileReference(File file) throws IOException;

    /**
     * Requests that the content with the given unique name and hash be mounted
     * in VFS at the given {@code mountPoint}.
     *
     * @param name unique name for the content as provided by the end user. Cannot be <code>null</code>
     * @param runtimeName the name the deployment file should be known as to the runtime. Cannot be <code>null</code>
     * @param deploymentContents the deployment contents. Cannot be <code>null</code>
     * @param mountPoint VFS location where the content should be mounted. Cannot be <code>null</code>
     * @return {@link java.io.Closeable} that can be used to close the mount
     *
     * @throws IOException
     */
    Closeable mountDeploymentContent(String name, String runtimeName, VirtualFile deploymentContents, VirtualFile mountPoint) throws IOException;

    /**
     * Requests that the content with the given unique name and hash be mounted
     * in VFS at the given {@code mountPoint}.
     *
     * @param name unique name for the content as provided by the end user. Cannot be <code>null</code>
     * @param runtimeName the name the deployment file should be known as to the runtime. Cannot be <code>null</code>
     * @param deploymentContents the deployment contents. Cannot be <code>null</code>
     * @param mountPoint VFS location where the content should be mounted. Cannot be <code>null</code>
     * @param mountExpanded
     * @return {@link java.io.Closeable} that can be used to close the mount
     *
     * @throws IOException
     */
    Closeable mountDeploymentContent(String name, String runtimeName, VirtualFile deploymentContents, VirtualFile mountPoint, boolean mountExpanded) throws IOException;
}
