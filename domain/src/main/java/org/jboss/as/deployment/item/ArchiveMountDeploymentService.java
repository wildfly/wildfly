/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat Middleware LLC, and individual contributors
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

package org.jboss.as.deployment.item;

import org.jboss.msc.service.Service;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.vfs.TempFileProvider;
import org.jboss.vfs.VFS;
import org.jboss.vfs.VFSUtils;
import org.jboss.vfs.VirtualFile;

import java.io.Closeable;
import java.io.IOException;
import java.util.concurrent.Executors;

/**
 * A service used for mounting a VFS archive.
 * @author John E. Bailey
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public final class ArchiveMountDeploymentService implements Service<VirtualFile> {
    private final VirtualFile root;
    private Closeable handle;
    private TempFileProvider tempProvider;

    public ArchiveMountDeploymentService(VirtualFile root) {
        this.root = root;
    }

    public void start(final StartContext context) throws StartException {
        try {
            tempProvider = TempFileProvider.create("test", Executors.newScheduledThreadPool(2));
            if(root.isFile())
                handle = VFS.mountZip(root, root, tempProvider);
        } catch(IOException e) {
            VFSUtils.safeClose(handle, tempProvider);
            throw new StartException("Failed to mount archive " + root, e);
        }
    }

    public void stop(final StopContext context) {
        VFSUtils.safeClose(handle, tempProvider);
    }

    public VirtualFile getValue() throws IllegalStateException {
        return root;
    }
}
