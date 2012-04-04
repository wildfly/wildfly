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

import java.util.ArrayList;
import java.util.List;

import org.jboss.as.server.ServerMessages;
import org.jboss.as.server.deployment.SimpleAttachable;
import org.jboss.vfs.VirtualFile;

/**
 * @author John E. Bailey
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public final class ResourceRoot extends SimpleAttachable {


    private final String rootName;
    private final VirtualFile root;
    private final MountHandle mountHandle;
    private final List<FilterSpecification> exportFilters = new ArrayList<FilterSpecification>();
    private boolean usePhysicalCodeSource;

    public ResourceRoot(final VirtualFile root, final MountHandle mountHandle) {
        this(root.getName(), root, mountHandle);
    }

    public ResourceRoot(final String rootName, final VirtualFile root, final MountHandle mountHandle) {
        this.rootName = rootName;
        this.root = root;
        this.mountHandle = mountHandle;
    }

    public String getRootName() {
        return rootName;
    }

    public VirtualFile getRoot() {
        return root;
    }

    public MountHandle getMountHandle() {
        return mountHandle;
    }

    public List<FilterSpecification> getExportFilters() {
        return exportFilters;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("ResourceRoot [");
        if (root != null)
            builder.append("root=").append(root);
        builder.append("]");
        return builder.toString();
    }

    public void setUsePhysicalCodeSource(final boolean usePhysicalCodeSource) {
        this.usePhysicalCodeSource = usePhysicalCodeSource;
    }

    public boolean isUsePhysicalCodeSource() {
        return usePhysicalCodeSource;
    }

    /**
     * Merges information from the resource root into this resource root
     *
     * @param additionalResourceRoot The root to merge
     */
    public void merge(final ResourceRoot additionalResourceRoot) {
        if(!additionalResourceRoot.getRoot().equals(root)) {
            throw ServerMessages.MESSAGES.cannotMergeResourceRoot(root, additionalResourceRoot.getRoot());
        }
        usePhysicalCodeSource = additionalResourceRoot.usePhysicalCodeSource;
        this.exportFilters.addAll(additionalResourceRoot.getExportFilters());
    }
}
