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

package org.jboss.as.deployment.module;

import org.jboss.msc.service.Service;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.vfs.TempFileProvider;
import org.jboss.vfs.VFS;
import org.jboss.vfs.VFSUtils;
import org.jboss.vfs.VirtualFile;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;

/**
 * A service which mounts an archive on the VFS.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public final class VFSMountService implements Service<Void> {

    public static final Method TEMP_FILE_PROVIDER_SETTER;
    static {
        try {
            TEMP_FILE_PROVIDER_SETTER = VFSMountService.class.getMethod("setTempFileProvider", TempFileProvider.class);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    // Configuration properties
    private String path;
    private TempFileProvider tempFileProvider;
    private boolean exploded;
    // Service state
    private volatile Closeable handle;

    /**
     * Construct a new instance.
     */
    public VFSMountService() {
    }

    /**
     * Construct a new instance.
     *
     * @param path the path to mount at
     */
    public VFSMountService(final String path) {
        this.path = path;
    }

    /**
     * Construct a new instance.
     *
     * @param path the path to mount at
     * @param exploded {@code true} if the mount should be fully exploded
     */
    public VFSMountService(final String path, final boolean exploded) {
        this.path = path;
        this.exploded = exploded;
    }

    /** {@inheritDoc} */
    public void start(final StartContext context) throws StartException {
        if(tempFileProvider == null) throw new StartException("A TempFileProvider is required");
        if(path == null) throw new StartException("A path to mount is required");
        try {
            final VirtualFile virtualFile = VFS.getChild(path);
            final File file = virtualFile.getPhysicalFile();
            if (file.isDirectory()) {
                handle = null;
                return;
            }
            if (exploded) {
                handle = VFS.mountZipExpanded(virtualFile, virtualFile, tempFileProvider);
            } else {
                handle = VFS.mountZip(virtualFile, virtualFile, tempFileProvider);
            }
        } catch (IOException e) {
            throw new StartException(e);
        }
    }

    /** {@inheritDoc} */
    public void stop(final StopContext context) {
        VFSUtils.safeClose(handle);
        handle = null;
    }

    /** {@inheritDoc} */
    public Void getValue() throws IllegalStateException {
        return null;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public void setTempFileProvider(TempFileProvider tempFileProvider) {
        this.tempFileProvider = tempFileProvider;
    }

    public void setExploded(boolean exploded) {
        this.exploded = exploded;
    }
}