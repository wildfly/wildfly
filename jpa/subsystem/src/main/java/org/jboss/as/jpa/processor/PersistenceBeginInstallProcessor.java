/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.jpa.processor;

import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jipijapa.plugin.spi.Platform;

/**
 * begin installation of persistence unit service and persistence providers in deployment
 *
 * @author Scott Marlow
 */
public class PersistenceBeginInstallProcessor implements DeploymentUnitProcessor {

    private final Platform platform;

    public PersistenceBeginInstallProcessor(Platform platform) {
        this.platform = platform;
    }
    /**
     * {@inheritDoc}
     */
    public void deploy(final DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {

        // deploy any persistence providers found in deployment
        PersistenceProviderHandler.deploy(phaseContext, platform);

        // start each PU service (except the PUs with property Configuration.JPA_CONTAINER_CLASS_TRANSFORMER = false)
        PersistenceUnitServiceHandler.deploy(phaseContext, true, platform);
    }

    /**
     * {@inheritDoc}
     */
    public void undeploy(final DeploymentUnit deploymentUnit) {
        PersistenceProviderHandler.undeploy(deploymentUnit);
    }

}

