/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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

package org.jboss.as.ee.metadata.property;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.jboss.as.ee.logging.EeLogger;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.server.deployment.module.ResourceRoot;
import org.jboss.vfs.VFSUtils;
import org.jboss.vfs.VirtualFile;

/**
 * @author John Bailey
 */
public class DeploymentPropertiesProcessor implements DeploymentUnitProcessor {
    private static final String DEPLOYMENT_PROPERTIES = "META-INF/jboss.properties";

    public void deploy(DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        if(deploymentUnit.hasAttachment(Attachments.DEPLOYMENT_PROPERTIES)) {
            return;
        }
        final ResourceRoot deploymentRoot = deploymentUnit.getAttachment(org.jboss.as.server.deployment.Attachments.DEPLOYMENT_ROOT);
        final VirtualFile deploymentFile = deploymentRoot.getRoot();
        final VirtualFile propertiesFile = deploymentFile.getChild(DEPLOYMENT_PROPERTIES);
        if (!propertiesFile.exists()) {
            return;
        }

        Properties properties = new Properties();
        InputStream propertyFileStream = null;
        try {
            propertyFileStream = propertiesFile.openStream();
            properties.load(propertyFileStream);
        } catch (IOException e) {
            throw EeLogger.ROOT_LOGGER.failedToLoadJbossProperties(e);
        } finally {
            VFSUtils.safeClose(propertyFileStream);
        }

        deploymentUnit.putAttachment(Attachments.DEPLOYMENT_PROPERTIES, properties);
    }

    public void undeploy(DeploymentUnit context) {
    }
}
