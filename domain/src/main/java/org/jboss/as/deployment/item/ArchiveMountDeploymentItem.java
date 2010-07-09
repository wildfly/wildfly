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

package org.jboss.as.deployment.item;

import org.jboss.msc.service.BatchBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.value.Values;
import org.jboss.vfs.VirtualFile;

/**
 * DeploymentItem used to manage the mounting of a VFS archive.
 *
 * @author John E. Bailey
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public final class ArchiveMountDeploymentItem implements DeploymentItem {
    public static final ServiceName ARCHIVE_MOUNT_DEPLOYMENT_SERVICE = ServiceName.JBOSS.append("mount", "service");

    private final VirtualFile root;

    private static final long serialVersionUID = 4335426754596642638L;

    public ArchiveMountDeploymentItem(VirtualFile root) {
        this.root = root;
    }

    public void install(final BatchBuilder builder) {
        final ServiceName serviceName = ARCHIVE_MOUNT_DEPLOYMENT_SERVICE.append(root.getPathName());
        builder.addServiceValueIfNotExist(serviceName, Values.immediateValue(new ArchiveMountDeploymentService(root)))
            .setInitialMode(ServiceController.Mode.ON_DEMAND);
    }

    public VirtualFile getRoot() {
        return root;
    }
}
