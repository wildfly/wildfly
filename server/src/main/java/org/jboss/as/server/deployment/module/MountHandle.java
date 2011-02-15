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

package org.jboss.as.server.deployment.module;

import java.io.Closeable;

import org.jboss.vfs.VFSUtils;

/**
 * Wrapper object to hold onto and close a VFS mount handle.
 *
 * If the provided mount handle is null then no action will be taken.
 *
 * @author John E. Bailey
 * @author Jason T. Greene
 * @author Stuart Douglas
 */
public class MountHandle implements Closeable {
    private final Closeable handle;

    /**
     * Construct new instance with the mount handle to close.
     *
     * @param handle The mount handle to close
     */
    public MountHandle(final Closeable handle) {
        this.handle = handle;
    }

    /**
     * Forcefully close this handle. Use with caution.
     */
    public void close() {
        if (handle != null) {
            VFSUtils.safeClose(handle);
        }
    }

    @Override
    protected void finalize() throws Throwable {
        if (handle != null) {
            VFSUtils.safeClose(handle);
        }
        super.finalize();
    }
}
