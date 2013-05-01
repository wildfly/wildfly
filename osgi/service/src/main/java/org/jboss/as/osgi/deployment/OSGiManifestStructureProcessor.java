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

import org.jboss.as.osgi.OSGiConstants;
import org.jboss.as.osgi.OSGiConstants.DeploymentType;
import org.jboss.as.osgi.service.BundleLifecycleIntegration;
import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.server.deployment.module.ResourceRoot;
import org.jboss.osgi.deployment.deployer.Deployment;
import org.jboss.osgi.framework.spi.IntegrationConstants;
import org.jboss.osgi.metadata.OSGiManifestBuilder;

/**
 * Processes deployments that contain a valid OSGi manifest.
 *
 * If so it attaches the {@link Manifest} under key {@link Attachments#OSGI_MANIFEST}
 *
 * @author Thomas.Diesler@jboss.com
 * @since 02-Dec-2010
 */
public class OSGiManifestStructureProcessor implements DeploymentUnitProcessor {

    @Override
    public void deploy(DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {

        DeploymentUnit depUnit = phaseContext.getDeploymentUnit();
        Manifest manifest = depUnit.getAttachment(Attachments.OSGI_MANIFEST);
        if (manifest != null)
            return;

        // Check if we already have a bundle {@link Deployment}
        Deployment dep = BundleLifecycleIntegration.getDeployment(depUnit.getName());
        if (dep != null) {
            manifest = dep.getAttachment(IntegrationConstants.MANIFEST_KEY);
        }

        ResourceRoot deploymentRoot = depUnit.getAttachment(Attachments.DEPLOYMENT_ROOT);
        if (manifest == null && deploymentRoot != null) {
            manifest = deploymentRoot.getAttachment(Attachments.MANIFEST);
        }

        // Check whether this is an OSGi manifest
        if (OSGiManifestBuilder.isValidBundleManifest(manifest)) {
            depUnit.putAttachment(Attachments.OSGI_MANIFEST, manifest);
            depUnit.putAttachment(OSGiConstants.DEPLOYMENT_TYPE_KEY, DeploymentType.Bundle);
        }
    }

    @Override
    public void undeploy(DeploymentUnit deploymentUnit) {
        deploymentUnit.removeAttachment(Attachments.OSGI_MANIFEST);
    }
}
