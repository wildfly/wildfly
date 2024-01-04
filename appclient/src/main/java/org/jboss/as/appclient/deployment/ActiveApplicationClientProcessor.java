/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.appclient.deployment;

import org.jboss.as.appclient.logging.AppClientLogger;
import org.jboss.as.ee.structure.DeploymentType;
import org.jboss.as.ee.structure.DeploymentTypeMarker;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;

import java.util.ArrayList;
import java.util.List;

/**
 * Processor that determines which application client should be started. This may be specified on the command line,
 * or alternatively if there is only one app client in the deployment it will be used.
 *
 * @author Stuart Douglas
 */
public class ActiveApplicationClientProcessor implements DeploymentUnitProcessor {

    /**
     * The deployment name passed in at startup. May be null.
     */
    private final String deploymentName;


    public ActiveApplicationClientProcessor(final String deploymentName) {
        this.deploymentName = deploymentName;
    }


    @Override
    public void deploy(final DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        if (!DeploymentTypeMarker.isType(DeploymentType.EAR, deploymentUnit)) {
            if(deploymentUnit.getParent() == null ) {
                deploymentUnit.putAttachment(AppClientAttachments.START_APP_CLIENT, true);
            }
            return;
        }
        final List<DeploymentUnit> appClients = new ArrayList<DeploymentUnit>();
        for (DeploymentUnit subDeployment : deploymentUnit.getAttachmentList(org.jboss.as.server.deployment.Attachments.SUB_DEPLOYMENTS)) {
            if (DeploymentTypeMarker.isType(DeploymentType.APPLICATION_CLIENT, subDeployment)) {
                if (deploymentName != null && deploymentName.equals(subDeployment.getName())) {
                    subDeployment.putAttachment(AppClientAttachments.START_APP_CLIENT, true);
                    return;
                }
                appClients.add(subDeployment);
            }
        }
        if(deploymentName != null && ! deploymentName.isEmpty()) {
            throw AppClientLogger.ROOT_LOGGER.cannotFindAppClient(deploymentName);
        }
        if (appClients.size() == 1) {
            appClients.get(0).putAttachment(AppClientAttachments.START_APP_CLIENT, true);
        } else if (appClients.isEmpty()) {
            throw AppClientLogger.ROOT_LOGGER.cannotFindAppClient();
        } else {
            throw AppClientLogger.ROOT_LOGGER.multipleAppClientsFound();
        }

    }

}
