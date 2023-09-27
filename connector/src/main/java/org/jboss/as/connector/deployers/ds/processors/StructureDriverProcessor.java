/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.connector.deployers.ds.processors;

import static org.jboss.as.connector.logging.ConnectorLogger.DEPLOYER_JDBC_LOGGER;

import java.util.List;

import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.server.deployment.DeploymentUtils;
import org.jboss.as.server.deployment.module.ResourceRoot;
import org.jboss.vfs.VirtualFile;

/**
 * Detects a JDBC driver at an early stage.
 * Currently this does nothig
 *
 * @author Jason T. Greene
 * @author Thomas.Diesler@jboss.com
 */
public final class StructureDriverProcessor implements DeploymentUnitProcessor {

    @Override
    public void deploy(final DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        List<ResourceRoot> resourceRoots = DeploymentUtils.allResourceRoots(deploymentUnit);
        for (ResourceRoot resourceRoot : resourceRoots) {
            final VirtualFile deploymentRoot = resourceRoot.getRoot();
            if (deploymentRoot.getChild("META-INF/services/java.sql.Driver").exists())  {
                DEPLOYER_JDBC_LOGGER.debugf("SQL driver detected: %s", deploymentUnit.getName());
                break;
            }
        }
    }
}
