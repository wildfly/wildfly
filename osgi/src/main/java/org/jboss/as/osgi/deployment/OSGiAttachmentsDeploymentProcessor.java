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

package org.jboss.as.osgi.deployment;

import org.jboss.as.deployment.DeploymentPhases;
import org.jboss.as.deployment.attachment.VirtualFileAttachment;
import org.jboss.as.deployment.module.MountHandle;
import org.jboss.as.deployment.unit.DeploymentUnitContext;
import org.jboss.as.deployment.unit.DeploymentUnitProcessingException;
import org.jboss.as.deployment.unit.DeploymentUnitProcessor;
import org.jboss.osgi.deployment.deployer.Deployment;
import org.jboss.osgi.deployment.deployer.DeploymentFactory;
import org.jboss.osgi.metadata.OSGiMetaData;
import org.jboss.osgi.resolver.XModule;
import org.jboss.osgi.spi.util.BundleInfo;
import org.jboss.osgi.vfs.AbstractVFS;
import org.jboss.vfs.VirtualFile;
import org.osgi.framework.Version;

/**
 * Processes deployments that have OSGi metadata attached.
 *
 * If so, it creates an {@link OSGiDeploymentService}.
 *
 * @author Thomas.Diesler@jboss.com
 * @since 20-Sep-2010
 */
public class OSGiAttachmentsDeploymentProcessor implements DeploymentUnitProcessor {

    public static final long PRIORITY = DeploymentPhases.INSTALL_SERVICES.plus(100L);

    @Override
    public void processDeployment(DeploymentUnitContext context) throws DeploymentUnitProcessingException {

        // Check if we already have an OSGi deployment
        Deployment deployment = DeploymentAttachment.getDeploymentAttachment(context);

        // Check for attached BundleInfo
        BundleInfo info = BundleInfoAttachment.getBundleInfoAttachment(context);
        if (deployment == null && info != null) {
            deployment = DeploymentFactory.createDeployment(info);
            deployment.addAttachment(BundleInfo.class, info);
            DeploymentAttachment.attachDeployment(context, deployment);
        }

        // Check for attached OSGiMetaData
        OSGiMetaData metadata = OSGiMetaDataAttachment.getOSGiMetaDataAttachment(context);
        if (deployment == null && metadata != null) {
            VirtualFile virtualFile = VirtualFileAttachment.getVirtualFileAttachment(context);
            String location = virtualFile.getPathName();
            String symbolicName = metadata.getBundleSymbolicName();
            Version version = metadata.getBundleVersion();
            deployment = DeploymentFactory.createDeployment(AbstractVFS.adapt(virtualFile), location, symbolicName, version);
            deployment.addAttachment(OSGiMetaData.class, metadata);
            DeploymentAttachment.attachDeployment(context, deployment);
        }

        // Check for attached XModule
        XModule resModule = XModuleAttachment.getXModuleAttachment(context);
        if (deployment == null && resModule != null) {
            VirtualFile virtualFile = VirtualFileAttachment.getVirtualFileAttachment(context);
            String location = virtualFile.getPathName();
            String symbolicName = resModule.getName();
            Version version = resModule.getVersion();
            deployment = DeploymentFactory.createDeployment(AbstractVFS.adapt(virtualFile), location, symbolicName, version);
            deployment.addAttachment(XModule.class, resModule);
            DeploymentAttachment.attachDeployment(context, deployment);
        }

        // Create the {@link OSGiDeploymentService}
        if (deployment != null) {

            // Prevent garbage collection of the MountHandle which will close the file
            MountHandle mount = context.getAttachment(MountHandle.ATTACHMENT_KEY);
            deployment.addAttachment(MountHandle.class, mount);

            OSGiDeploymentService.addService(context);
        }
    }
}
