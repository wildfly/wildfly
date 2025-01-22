/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.persistence.jipijapa.hibernate7;

import java.net.URISyntaxException;
import java.net.URL;

import org.hibernate.boot.archive.internal.StandardArchiveDescriptorFactory;
import org.hibernate.boot.archive.spi.ArchiveDescriptor;
import org.jboss.vfs.VFS;


/**
 * In Hibernate terms, the ArchiveDescriptorFactory contract is used to plug in handling for how to deal
 * with archives in various systems.  For JBoss, that means its VirtualFileSystem API.
 *
 * @author Steve Ebersole
 */
public class VirtualFileSystemArchiveDescriptorFactory extends StandardArchiveDescriptorFactory {
    static final VirtualFileSystemArchiveDescriptorFactory INSTANCE = new VirtualFileSystemArchiveDescriptorFactory();

    private VirtualFileSystemArchiveDescriptorFactory() {
    }

    @Override
    public ArchiveDescriptor buildArchiveDescriptor(URL url, String entryBase) {
        try {
            return new VirtualFileSystemArchiveDescriptor( VFS.getChild( url.toURI() ), entryBase );
        }
        catch (URISyntaxException e) {
            throw new IllegalArgumentException( e );
        }
    }
}
