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

import java.io.IOException;
import java.io.InputStream;

import org.jboss.vfs.VirtualFile;

import org.hibernate.jpa.boot.spi.InputStreamAccess;
import org.hibernate.jpa.boot.spi.NamedInputStream;

/**
 * InputStreamAccess provides Hibernate with lazy, on-demand access to InputStreams for the various
 * types of resources found during archive scanning.
 *
 * @author Steve Ebersole
 */
public class VirtualFileInputStreamAccess implements InputStreamAccess {
    private final String name;
    private final VirtualFile virtualFile;

    public VirtualFileInputStreamAccess(String name, VirtualFile virtualFile) {
        this.name = name;
        this.virtualFile = virtualFile;
    }

    @Override
    public String getStreamName() {
        return name;
    }

    @Override
    public InputStream accessInputStream() {
        try {
            return virtualFile.openStream();
        }
        catch (IOException e) {
            throw JPA_LOGGER.cannotOpenVFSStream(e, virtualFile.getName());
        }
    }

    @Override
    public NamedInputStream asNamedInputStream() {
        return new NamedInputStream( getStreamName(), accessInputStream() );
    }
}
