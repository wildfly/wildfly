/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.persistence.jipijapa.hibernate7;

import org.hibernate.boot.archive.spi.ArchiveContext;
import org.hibernate.boot.archive.spi.ArchiveDescriptor;
import org.hibernate.boot.archive.spi.ArchiveEntry;
import org.hibernate.boot.archive.spi.InputStreamAccess;
import org.jboss.vfs.VirtualFile;


/**
 * Representation of an archive in the JBoss VirtualFileSystem API in terms of how to walk entries.
 *
 * @author Steve Ebersole
 */
public class VirtualFileSystemArchiveDescriptor implements ArchiveDescriptor {
    private final VirtualFile root;

    public VirtualFileSystemArchiveDescriptor(VirtualFile archiveRoot, String entryBase) {
        if ( entryBase != null && entryBase.length() > 0 && ! "/".equals( entryBase ) ) {
            this.root = archiveRoot.getChild( entryBase );
        }
        else {
            this.root = archiveRoot;
        }
    }

    public VirtualFile getRoot() {
        return root;
    }

    @Override
    public void visitArchive(ArchiveContext archiveContext) {
        processVirtualFile( root, null, archiveContext );
    }

    private void processVirtualFile(VirtualFile virtualFile, String path, ArchiveContext archiveContext) {
        if ( path == null ) {
            path = "";
        }
        else {
            if ( !path.endsWith( "/'" ) ) {
                path = path + "/";
            }
        }

        for ( VirtualFile child : virtualFile.getChildren() ) {
            if ( !child.exists() ) {
                // should never happen conceptually, but...
                continue;
            }

            if ( child.isDirectory() ) {
                processVirtualFile( child, path + child.getName(), archiveContext );
                continue;
            }

            final String name = child.getPathName();
            final String relativeName = path + child.getName();
            final InputStreamAccess inputStreamAccess = new VirtualFileInputStreamAccess( name, child );

            final ArchiveEntry entry = new ArchiveEntry() {
                @Override
                public String getName() {
                    return name;
                }

                @Override
                public String getNameWithinArchive() {
                    return relativeName;
                }

                @Override
                public InputStreamAccess getStreamAccess() {
                    return inputStreamAccess;
                }
            };

            archiveContext.obtainArchiveEntryHandler( entry ).handleEntry( entry, archiveContext );
        }
    }
}
