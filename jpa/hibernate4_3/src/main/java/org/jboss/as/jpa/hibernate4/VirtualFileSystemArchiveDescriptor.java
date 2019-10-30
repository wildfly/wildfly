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

import org.jboss.vfs.VirtualFile;

import org.hibernate.jpa.boot.archive.spi.ArchiveContext;
import org.hibernate.jpa.boot.archive.spi.ArchiveDescriptor;
import org.hibernate.jpa.boot.archive.spi.ArchiveEntry;
import org.hibernate.jpa.boot.spi.InputStreamAccess;

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
