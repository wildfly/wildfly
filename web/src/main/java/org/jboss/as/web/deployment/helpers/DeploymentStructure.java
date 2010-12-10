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

package org.jboss.as.web.deployment.helpers;

import java.io.Closeable;
import java.io.IOException;

import org.jboss.as.server.deployment.AttachmentKey;
import org.jboss.as.server.deployment.module.MountHandle;
import org.jboss.vfs.VirtualFile;

/**
 * The deployment structure
 *
 * @author Emanuel Muckenhuber
 */
public class DeploymentStructure {

    public static final AttachmentKey<DeploymentStructure> ATTACHMENT_KEY = AttachmentKey.create(DeploymentStructure.class);
    private final ClassPathEntry[] entries;

    public DeploymentStructure(final ClassPathEntry[] entries) {
        this.entries = entries;
    }

    public ClassPathEntry[] getEntries() {
        return entries;
    }

    public static class ClassPathEntry implements Closeable {
        private final String name;
        private final VirtualFile root;
        private final Closeable closeable;
        private final MountHandle mountHandle;

        public ClassPathEntry(final VirtualFile root, final Closeable closeable) {
            this(root.getName(), root, closeable);
        }

        public ClassPathEntry(final String name, final VirtualFile root, final Closeable closeable) {
            this.name = name;
            this.root = root;
            this.closeable = closeable;
            this.mountHandle = new MountHandle(closeable);
        }

        public ClassPathEntry(final String name, final VirtualFile root, final MountHandle handle) {
            this.name = name;
            this.root = root;
            this.closeable = null;
            this.mountHandle = new MountHandle(null);
        }

        public String getName() {
            return name;
        }

        public VirtualFile getRoot() {
            return root;
        }

        public MountHandle getMountHandle() {
            return mountHandle;
        }

        /** {@inheritDoc} */
        public void close() throws IOException {
            if(closeable != null) {
                closeable.close();
            }
        }
    }

}
