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

import java.util.jar.Manifest;

import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceRegistry;
import org.jboss.osgi.deployment.deployer.Deployment;
import org.jboss.osgi.spi.BundleInfo;
import org.jboss.osgi.vfs.AbstractVFS;
import org.jboss.vfs.VirtualFile;
import org.osgi.framework.BundleException;

import static org.jboss.as.osgi.OSGiMessages.MESSAGES;

/**
 * Processes deployments that contain a valid OSGi manifest.
 *
 * @author Thomas.Diesler@jboss.com
 * @since 20-Sep-2010
 */
public class OSGiBundleInfoParseProcessor implements DeploymentUnitProcessor {

    @Override
    public void deploy(DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {

        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        final String contextName = deploymentUnit.getName();
        final ServiceRegistry serviceRegistry = phaseContext.getServiceRegistry();

        // Check if we already have a BundleInfo
        BundleInfo info = BundleInfoAttachment.getBundleInfo(deploymentUnit);
        ServiceController<Deployment> deploymentController = DeploymentHolderService.getDeployment(serviceRegistry, contextName);
        if (info != null || deploymentController != null)
            return;

        // Get the manifest from the deployment's virtual file
        Manifest manifest = deploymentUnit.getAttachment(Attachments.OSGI_MANIFEST);
        if (manifest == null)
            return;

        // Construct and attach the {@link BundleInfo}
        try {
            VirtualFile virtualFile = deploymentUnit.getAttachment(Attachments.DEPLOYMENT_ROOT).getRoot();
            info = BundleInfo.createBundleInfo(AbstractVFS.adapt(virtualFile), contextName);
            BundleInfoAttachment.attachBundleInfo(deploymentUnit, info);
        } catch (BundleException ex) {
            throw new DeploymentUnitProcessingException(MESSAGES.cannotCreateBundleDeployment(deploymentUnit));
        }
    }

    @Override
    public void undeploy(final DeploymentUnit deploymentUnit) {
        BundleInfoAttachment.detachBundleInfo(deploymentUnit);
    }
}
