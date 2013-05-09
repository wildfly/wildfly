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
package org.jboss.as.osgi.web;

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
 * Provide OSGi metadata for webbundle:// deployments
 *
 * @author Thomas.Diesler@jboss.com
 * @since 30-Nov-2012
 */
public class WebBundleStructureProcessor implements DeploymentUnitProcessor {

    @Override
    public void deploy(final DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {

        // Check if we already have {@link OSGiMetaData} or a bundle {@link Deployment}
        DeploymentUnit depUnit = phaseContext.getDeploymentUnit();
        OSGiMetaData metadata = depUnit.getAttachment(OSGiConstants.OSGI_METADATA_KEY);
        if (metadata != null)
            return;

        // Check if we already have a bundle {@link Deployment}
        Deployment dep = BundleLifecycleIntegration.getDeployment(depUnit.getName());
        if (dep != null)
            return;

        // Generate the OSGi metadata from a webbundle:// URI
        // [TODO] this should generate OSGiMetaData directly
        Manifest manifest = WebBundleURIParser.parse(depUnit.getName());
        if (manifest != null) {
            metadata = OSGiMetaDataBuilder.load(manifest);
            depUnit.putAttachment(OSGiConstants.OSGI_METADATA_KEY, metadata);
            depUnit.putAttachment(Attachments.OSGI_MANIFEST, manifest);
        }
    }

    @Override
    public void undeploy(final DeploymentUnit depUnit) {
    }
}
