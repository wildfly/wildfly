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
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceRegistry;
import org.jboss.msc.service.ServiceController.Mode;
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
 * If so, it creates an {@link BundleInstallProvider}.
 *
 * @author Thomas.Diesler@jboss.com
 * @since 20-Sep-2010
 */
public class BundleInstallProcessor implements DeploymentUnitProcessor {

    @Override
    public void deploy(final DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {

        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        final String contextName = deploymentUnit.getName();

        // Check if we already have an OSGi deployment
        Deployment deployment = OSGiDeploymentAttachment.getDeployment(deploymentUnit);

        // Check if {@link InstallHandlerIntegration} provided the {@link Deployment}
        if (deployment == null) {
            ServiceRegistry serviceRegistry = phaseContext.getServiceRegistry();
            ServiceController<Deployment> controller = DeploymentHolderService.getDeployment(serviceRegistry, contextName);
            if (controller != null) {
                deployment = controller.getValue();
                controller.setMode(Mode.REMOVE);
            }
        }

        // Check for attached BundleInfo
        BundleInfo info = BundleInfoAttachment.getBundleInfo(deploymentUnit);
        if (deployment == null && info != null) {
            deployment = DeploymentFactory.createDeployment(info);
            deployment.addAttachment(BundleInfo.class, info);
            deployment.setAutoStart(true);
            OSGiDeploymentAttachment.attachDeployment(deploymentUnit, deployment);
        }

        // Check for attached OSGiMetaData
        OSGiMetaData metadata = OSGiMetaDataAttachment.getOSGiMetaData(deploymentUnit);
        if (deployment == null && metadata != null) {
            String symbolicName = metadata.getBundleSymbolicName();
            Version version = metadata.getBundleVersion();
            VirtualFile virtualFile = deploymentUnit.getAttachment(Attachments.DEPLOYMENT_ROOT).getRoot();
            deployment = DeploymentFactory.createDeployment(AbstractVFS.adapt(virtualFile), contextName, symbolicName, version);
            deployment.setAutoStart(true);
            deployment.addAttachment(OSGiMetaData.class, metadata);
            OSGiDeploymentAttachment.attachDeployment(deploymentUnit, deployment);
        }

        // Check for attached XModule
        XModule resModule = XModuleAttachment.getXModuleAttachment(deploymentUnit);
        if (deployment == null && resModule != null) {
            String symbolicName = resModule.getName();
            Version version = resModule.getVersion();
            VirtualFile virtualFile = deploymentUnit.getAttachment(Attachments.DEPLOYMENT_ROOT).getRoot();
            deployment = DeploymentFactory.createDeployment(AbstractVFS.adapt(virtualFile), contextName, symbolicName, version);
            deployment.setAutoStart(true);
            deployment.addAttachment(XModule.class, resModule);
            OSGiDeploymentAttachment.attachDeployment(deploymentUnit, deployment);
        }

        // Create the {@link BundleInstallProvider}
        if (deployment != null) {
            // Prevent garbage collection of the MountHandle which will close the file
            // MountHandle mount = deploymentUnit.getAttachment(Attachments.DEPLOYMENT_ROOT).getMountHandle();
            // deployment.addAttachment(MountHandle.class, mount);
            BundleInstallService.addService(phaseContext, deployment);
        }
    }

    @Override
    public void undeploy(final DeploymentUnit deploymentUnit) {
        BundleInstallService.removeService(deploymentUnit);
    }
}
