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

import org.jboss.as.deployment.module.TempFileProviderService;
import org.jboss.as.deployment.module.VFSMountService;
import org.jboss.msc.service.BatchBuilder;
import org.jboss.msc.service.ServiceName;
import org.jboss.vfs.TempFileProvider;
import org.jboss.vfs.VirtualFile;

/**
 * @author John E. Bailey
 */
public class VFSMountDeploymentItem implements DeploymentItem {

    private final String deploymentName;
    private final VirtualFile virtualFile;
    private final boolean expanded;

    public VFSMountDeploymentItem(String deploymentName, VirtualFile virtualFile, boolean expanded) {
        this.deploymentName = deploymentName;
        this.virtualFile = virtualFile;
        this.expanded = expanded;
    }

    @Override
    public void install(DeploymentItemContext context) {
        final BatchBuilder batchBuilder = context.getBatchBuilder();
        final ServiceName serviceName = VFSMountService.SERVICE_NAME.append(deploymentName);
        final VFSMountService mountService = new VFSMountService(virtualFile.getPathName(), expanded);
        batchBuilder.addService(serviceName, mountService)
            .addDependency(TempFileProviderService.SERVICE_NAME, TempFileProvider.class, mountService.getTempFileProviderInjector());

        // Add a batch level dep to make sure this comes before all other services
        batchBuilder.addDependency(serviceName);
    }
}
