/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
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
}
