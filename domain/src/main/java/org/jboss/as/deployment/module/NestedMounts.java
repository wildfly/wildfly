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

import java.io.Closeable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.jboss.as.deployment.AttachmentKey;
import org.jboss.vfs.VirtualFile;

/**
 * A grouping of nested mount handles.
 *
 * @author Jason T. Greene
 */
public final class NestedMounts implements Iterable<NestedMounts.Entry> {
    public static AttachmentKey<NestedMounts> ATTACHMENT_KEY = new AttachmentKey<NestedMounts>(NestedMounts.class);
    private final List<Entry> mounts;

    public NestedMounts(int initialCapacity) {
        mounts = new ArrayList<Entry>(initialCapacity);
    }

    public static final class Entry {
        private VirtualFile file;
        private MountHandle mount;

        public Entry(VirtualFile file, MountHandle mount) {
            this.file = file;
            this.mount = mount;
        }

        public VirtualFile file() {
            return file;
        }

        public MountHandle mount() {
            return mount;
        }
    }

    public Entry get(int i) {
        return mounts.get(i);
    }

    public void add(VirtualFile file, MountHandle mount) {
        mounts.add(new Entry(file, mount));
    }

    public Iterator<Entry> iterator() {
        return mounts.iterator();
    }

    public int size() {
        return mounts.size();
    }

    public Closeable[] getClosables() {
        int size = mounts.size();
        Closeable[] closables = new Closeable[size];
        for (int i = 0; i < size; i++) {
            closables[i] = mounts.get(i).mount();
        }

        return closables;
    }

}
