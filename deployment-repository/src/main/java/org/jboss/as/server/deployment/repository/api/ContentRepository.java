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

package org.jboss.as.server.deployment.repository.api;

import java.io.IOException;
import java.io.InputStream;

import org.jboss.msc.service.ServiceName;
import org.jboss.vfs.VirtualFile;

/**
 * Repository for deployment content.
 *
 * @author John Bailey
 */
public interface ContentRepository {

    /**
     * Standard ServiceName under which a service controller for an instance of
     * @code Service<ContentRepository> would be registered.
     */
    ServiceName SERVICE_NAME = ServiceName.JBOSS.append("content-repository");

    /**
     * Add the given content to the repository.
     *
     * @param stream stream from which the content can be read. Cannot be <code>null</code>
     * @return the hash of the content that will be used as an internal identifier
     *         for the content. Will not be <code>null</code>
     * @throws IOException
     */
    byte[] addContent(InputStream stream) throws IOException;

    /**
     * Get the content as a virtual file.
     *
     * @param hash the hash. Cannot be {@code null}
     */
    VirtualFile getContent(byte[] hash);

    /**
     * Gets whether content with the given hash is stored in the repository.
     *
     * @param hash the hash. Cannot be {@code null}
     *
     * @return {@code true} if the repository has content with the given hash
     */
    boolean hasContent(byte[] hash);

    /**
     * Remove the given content from the repository.
     *
     * @param hash the hash. Cannot be {@code null}
     */
    void removeContent(byte[] hash);
}
