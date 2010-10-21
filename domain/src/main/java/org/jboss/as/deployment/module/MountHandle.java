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

import org.jboss.as.deployment.AttachmentKey;
import org.jboss.vfs.VFSUtils;

import java.io.Closeable;

/**
 * Wrapper object to hold onto and close a VFS mount handle.
 *
 * @author John E. Bailey
 * @author Jason T. Greene
 */
public class MountHandle implements Closeable {
    public static final AttachmentKey<MountHandle> ATTACHMENT_KEY = new AttachmentKey<MountHandle>(MountHandle.class);

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
        VFSUtils.safeClose(handle);
    }

    @Override
    protected void finalize() throws Throwable {
        VFSUtils.safeClose(handle);
        super.finalize();
    }
}
