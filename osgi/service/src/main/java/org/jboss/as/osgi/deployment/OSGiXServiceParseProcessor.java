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

import static org.jboss.as.osgi.OSGiMessages.MESSAGES;

import java.io.IOException;
import java.util.Properties;

import org.jboss.as.osgi.OSGiConstants;
import org.jboss.as.osgi.OSGiConstants.DeploymentType;
import org.jboss.as.osgi.service.BundleLifecycleIntegration;
import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.osgi.deployment.deployer.Deployment;
import org.jboss.osgi.metadata.OSGiMetaData;
import org.jboss.osgi.metadata.OSGiMetaDataBuilder;
import org.jboss.vfs.VirtualFile;

/**
 * Processes deployments that contain META-INF/jbosgi-xservice.properties
 *
 * @author Thomas.Diesler@jboss.com
 * @since 20-Sep-2010
 */
public class OSGiXServiceParseProcessor implements DeploymentUnitProcessor {

    public static final String XSERVICE_PROPERTIES_NAME = "META-INF/jbosgi-xservice.properties";

    @Override
    public void deploy(DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {

        // Check if we already have {@link OSGiMetaData} attached
        // or if we already have a bundle {@link Deployment}
        DeploymentUnit depUnit = phaseContext.getDeploymentUnit();
        OSGiMetaData metadata = depUnit.getAttachment(OSGiConstants.OSGI_METADATA_KEY);
        Deployment dep = BundleLifecycleIntegration.getDeployment(depUnit.getName());
        if (metadata != null || dep != null)
            return;

        // Get the OSGi XService properties
        VirtualFile virtualFile = depUnit.getAttachment(Attachments.DEPLOYMENT_ROOT).getRoot();
        VirtualFile xserviceFile = virtualFile.getChild(XSERVICE_PROPERTIES_NAME);
        if (xserviceFile.exists() == false)
            return;

        try {
            Properties props = new Properties();
            props.load(xserviceFile.openStream());
            metadata = OSGiMetaDataBuilder.load(props);
        } catch (IOException ex) {
            throw MESSAGES.cannotParseOSGiMetadata(ex, xserviceFile);
        }

        depUnit.putAttachment(OSGiConstants.OSGI_METADATA_KEY, metadata);
        depUnit.putAttachment(OSGiConstants.DEPLOYMENT_TYPE_KEY, DeploymentType.Module);
    }

    @Override
    public void undeploy(final DeploymentUnit depUnit) {
        depUnit.removeAttachment(OSGiConstants.OSGI_METADATA_KEY);
    }
}
