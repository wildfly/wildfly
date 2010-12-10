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

import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.server.deployment.module.MountHandle;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.msc.service.ServiceRegistry;
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

    private ServiceRegistry serviceRegistry;

    public OSGiAttachmentsDeploymentProcessor(ServiceRegistry serviceRegistry) {
        this.serviceRegistry = serviceRegistry;
    }

    @Override
    public void deploy(final DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final DeploymentUnit deploymentUnitContext = phaseContext.getDeploymentUnit();

        // Check if we already have an OSGi deployment
        Deployment deployment = OSGiDeploymentAttachment.getAttachment(deploymentUnitContext);

        String location = InstallBundleInitiatorService.getLocation(serviceRegistry, deploymentUnitContext.getName());
        VirtualFile virtualFile = phaseContext.getAttachment(Attachments.DEPLOYMENT_ROOT);



        // Check for attached BundleInfo
        BundleInfo info = BundleInfoAttachment.getBundleInfoAttachment(deploymentUnitContext);
        if (deployment == null && info != null) {
            deployment = DeploymentFactory.createDeployment(info);
            deployment.addAttachment(BundleInfo.class, info);
            OSGiDeploymentAttachment.attachDeployment(deploymentUnitContext, deployment);
        }

        // Check for attached OSGiMetaData
        OSGiMetaData metadata = OSGiMetaDataAttachment.getOSGiMetaDataAttachment(deploymentUnitContext);
        if (deployment == null && metadata != null) {
            String symbolicName = metadata.getBundleSymbolicName();
            Version version = metadata.getBundleVersion();
            deployment = DeploymentFactory.createDeployment(AbstractVFS.adapt(virtualFile), location, symbolicName, version);
            deployment.addAttachment(OSGiMetaData.class, metadata);
            OSGiDeploymentAttachment.attachDeployment(deploymentUnitContext, deployment);
        }

        // Check for attached XModule
        XModule resModule = XModuleAttachment.getXModuleAttachment(deploymentUnitContext);
        if (deployment == null && resModule != null) {
            String symbolicName = resModule.getName();
            Version version = resModule.getVersion();
            deployment = DeploymentFactory.createDeployment(AbstractVFS.adapt(virtualFile), location, symbolicName, version);
            deployment.addAttachment(XModule.class, resModule);
            OSGiDeploymentAttachment.attachDeployment(deploymentUnitContext, deployment);
        }

        // Create the {@link OSGiDeploymentService}
        if (deployment != null) {

            // Prevent garbage collection of the MountHandle which will close the file
            MountHandle mount = phaseContext.getAttachment(MountHandle.ATTACHMENT_KEY);
            deployment.addAttachment(MountHandle.class, mount);

            // Mark the bundle to start automatically
            deployment.setAutoStart(true);

            OSGiDeploymentService.addService(phaseContext);
        }
    }

    public void undeploy(DeploymentUnit context) {
        OSGiDeploymentService.removeService(context);
    }
}
