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
import org.jboss.as.osgi.service.BundleLifecycleIntegration;
import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.osgi.deployment.deployer.Deployment;
import org.jboss.osgi.metadata.OSGiMetaData;
import org.jboss.osgi.metadata.OSGiMetaDataBuilder;

/**
 * Processes deployments that contain a valid OSGi manifest.
 *
 * If so it attaches the {@link OSGiMetaData}.
 *
 * @author Thomas.Diesler@jboss.com
 * @since 30-Nov-2012
 */
public class OSGiMetaDataStructureProcessor implements DeploymentUnitProcessor {

    @Override
    public void deploy(DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {

        DeploymentUnit depUnit = phaseContext.getDeploymentUnit();
        if (depUnit.hasAttachment(OSGiConstants.OSGI_METADATA_KEY))
            return;

        // Check if we already have a bundle {@link Deployment}
        Deployment dep = BundleLifecycleIntegration.getDeployment(depUnit.getName());
        if (dep != null) {
            OSGiMetaData metadata = dep.getAttachment(OSGiMetaData.class);
            if (metadata != null) {
                depUnit.putAttachment(OSGiConstants.OSGI_METADATA_KEY, metadata);
                return;
            }
        }

        Manifest manifest = depUnit.getAttachment(Attachments.OSGI_MANIFEST);
        if (manifest != null) {
            OSGiMetaData metadata = OSGiMetaDataBuilder.load(manifest);
            depUnit.putAttachment(OSGiConstants.OSGI_METADATA_KEY, metadata);
        }
    }

    @Override
    public void undeploy(DeploymentUnit deploymentUnit) {
        deploymentUnit.removeAttachment(OSGiConstants.OSGI_METADATA_KEY);
    }
}
