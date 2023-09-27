/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.jpa.processor;

import org.jboss.as.jpa.config.PersistenceProviderDeploymentHolder;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.server.deployment.DeploymentUtils;
import org.jipijapa.plugin.spi.Platform;

/**
 * complete installation of persistence unit service and persistence providers in deployment
 *
 * @author Scott Marlow
 */
public class PersistenceCompleteInstallProcessor implements DeploymentUnitProcessor {

    private final Platform platform;

    public PersistenceCompleteInstallProcessor(Platform platform) {
        this.platform = platform;
    }

    @Override
    public void deploy(DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        if (deploymentHasPersistenceProvider(phaseContext.getDeploymentUnit())) {
            // finish registration of persistence provider
            PersistenceProviderHandler.finishDeploy(phaseContext);
        }

        // only install PUs with property Configuration.JPA_CONTAINER_CLASS_TRANSFORMER = false (since they weren't started before)
        // this allows @DataSourceDefinition to work (which don't start until the Install phase)
        PersistenceUnitServiceHandler.deploy(phaseContext, false, platform);

    }

    @Override
    public void undeploy(DeploymentUnit context) {
        PersistenceUnitServiceHandler.undeploy(context); // always uninstall persistent unit services from here
    }

    private static boolean deploymentHasPersistenceProvider(DeploymentUnit deploymentUnit) {
        deploymentUnit = DeploymentUtils.getTopDeploymentUnit(deploymentUnit);
        PersistenceProviderDeploymentHolder persistenceProviderDeploymentHolder =  deploymentUnit.getAttachment(JpaAttachments.DEPLOYED_PERSISTENCE_PROVIDER);
        return (persistenceProviderDeploymentHolder != null && persistenceProviderDeploymentHolder.getProviders() != null ? persistenceProviderDeploymentHolder.getProviders().size() > 0: false);
    }

}
