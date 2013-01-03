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

import java.util.List;
import java.util.jar.Manifest;

import org.jboss.as.ee.structure.DeploymentType;
import org.jboss.as.ee.structure.DeploymentTypeMarker;
import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.server.deployment.SubDeploymentMarker;
import org.jboss.as.server.deployment.module.IgnoreMetaInfMarker;
import org.jboss.as.server.deployment.module.ManifestAttachmentProcessor;
import org.jboss.as.server.deployment.module.ResourceRoot;
import org.jboss.osgi.metadata.OSGiManifestBuilder;

/**
 * Processor that marks bundle sub deployments in ear deployments.
 *
 * @author Thomas.Diesler
 * @since 02-Jul-2012
 */
public class BundleSubDeploymentMarkingProcessor implements DeploymentUnitProcessor {

    @Override
    public void deploy(DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final DeploymentUnit depUnit = phaseContext.getDeploymentUnit();
        if (!DeploymentTypeMarker.isType(DeploymentType.EAR, depUnit)) {
            return;
        }
        ResourceRoot deploymentRoot = depUnit.getAttachment(Attachments.DEPLOYMENT_ROOT);
        List<ResourceRoot> potentialSubDeployments = depUnit.getAttachmentList(Attachments.RESOURCE_ROOTS);
        for (ResourceRoot resourceRoot : potentialSubDeployments) {
            if (IgnoreMetaInfMarker.isIgnoreMetaInf(resourceRoot))
                continue;

            // Don't accept archives in the 'lib' directory
            String pathName = resourceRoot.getRoot().getPathNameRelativeTo(deploymentRoot.getRoot());
            if (pathName.startsWith("lib/"))
                continue;

            // Check if this sub deployment has a valid OSGi manifest - if so mark it
            Manifest manifest = ManifestAttachmentProcessor.getManifest(resourceRoot);
            if (OSGiManifestBuilder.isValidBundleManifest(manifest)) {
                SubDeploymentMarker.mark(resourceRoot);
            }
        }
    }

    @Override
    public void undeploy(DeploymentUnit context) {

    }
}
