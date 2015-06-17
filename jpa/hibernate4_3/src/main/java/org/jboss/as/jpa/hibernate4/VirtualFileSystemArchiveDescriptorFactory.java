/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2015, Red Hat, Inc., and individual contributors
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
package org.jboss.as.jpa.hibernate4;

import static org.jipijapa.JipiLogger.JPA_LOGGER;

import java.net.URISyntaxException;
import java.net.URL;

import org.jboss.vfs.VFS;

import org.hibernate.jpa.boot.archive.internal.StandardArchiveDescriptorFactory;
import org.hibernate.jpa.boot.archive.spi.ArchiveDescriptor;

/**
 * In Hibernate terms, the ArchiveDescriptorFactory contract is used to plug in handling for how to deal
 * with archives in various systems.  For JBoss, that means its VirtualFileSystem API.
 *
 * @author Steve Ebersole
 */
public class VirtualFileSystemArchiveDescriptorFactory extends StandardArchiveDescriptorFactory {
    public static final VirtualFileSystemArchiveDescriptorFactory INSTANCE = new VirtualFileSystemArchiveDescriptorFactory();

    @Override
    public ArchiveDescriptor buildArchiveDescriptor(URL url, String entryBase) {
        try {
            return new VirtualFileSystemArchiveDescriptor( VFS.getChild( url.toURI() ), entryBase );
        }
        catch (URISyntaxException e) {
            throw JPA_LOGGER.uriSyntaxException(e);
        }
    }
}
