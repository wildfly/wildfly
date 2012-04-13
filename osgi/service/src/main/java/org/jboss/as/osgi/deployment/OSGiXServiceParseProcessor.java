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

import java.io.IOException;

import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.osgi.metadata.OSGiMetaData;
import org.jboss.osgi.metadata.OSGiMetaDataBuilder;
import org.jboss.vfs.VirtualFile;

import static org.jboss.as.osgi.OSGiMessages.MESSAGES;

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

        // Check if we already have an OSGi deployment
        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        OSGiMetaData metadata = OSGiMetaDataAttachment.getOSGiMetaData(deploymentUnit);
        if (metadata != null)
            return;

        // Get the OSGi XService properties
        VirtualFile virtualFile = deploymentUnit.getAttachment(Attachments.DEPLOYMENT_ROOT).getRoot();
        VirtualFile xserviceFile = virtualFile.getChild(XSERVICE_PROPERTIES_NAME);
        if (xserviceFile.exists() == false)
            return;

        try {
            metadata = OSGiMetaDataBuilder.load(xserviceFile.openStream());
            OSGiMetaDataAttachment.attachOSGiMetaData(deploymentUnit, metadata);
        } catch (IOException ex) {
            throw MESSAGES.cannotParseOSGiMetadata(ex, xserviceFile);
        }
    }

    @Override
    public void undeploy(final DeploymentUnit deploymentUnit) {
        OSGiMetaDataAttachment.detachOSGiMetaData(deploymentUnit);
    }
}
