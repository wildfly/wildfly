/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.webservices.deployers;

import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.webservices.config.ServerConfigFactoryImpl;
import org.jboss.as.webservices.config.ServerConfigImpl;
import org.jboss.as.webservices.deployers.deployment.WSDeploymentBuilder;
import org.jboss.as.webservices.util.WSAttachmentKeys;

/**
 * This deployer initializes JBossWS deployment meta data.
 *
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 * @author <a href="mailto:alessio.soldano@jboss.com">Alessio Soldano</a>
 */
public final class WSModelDeploymentProcessor extends TCCLDeploymentProcessor implements DeploymentUnitProcessor {

    @Override
    public void internalDeploy(final DeploymentPhaseContext phaseContext) {
        final DeploymentUnit unit = phaseContext.getDeploymentUnit();
        WSDeploymentBuilder.getInstance().build(unit);

        if (isWebServiceDeployment(unit)) { //note, this check works only after the WSDeploymentBuilder above has run
            ServerConfigImpl config = (ServerConfigImpl)ServerConfigFactoryImpl.getConfig();
            config.incrementWSDeploymentCount();
        }
    }

    @Override
    public void internalUndeploy(final DeploymentUnit context) {
        if (isWebServiceDeployment(context)) {
            ServerConfigImpl config = (ServerConfigImpl)ServerConfigFactoryImpl.getConfig();
            config.decrementWSDeploymentCount();
        }

        // Cleans up reference established by AbstractDeploymentModelBuilder#propagateAttachments
        context.removeAttachment(WSAttachmentKeys.DEPLOYMENT_KEY);
    }

    private static boolean isWebServiceDeployment(final DeploymentUnit unit) {
        return unit.getAttachment(WSAttachmentKeys.DEPLOYMENT_KEY) != null;
    }
}
