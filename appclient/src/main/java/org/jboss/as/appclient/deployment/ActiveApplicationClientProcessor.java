/*
 * JBoss, Home of Professional Open Source
 * Copyright 2010, Red Hat Inc., and individual contributors as indicated
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
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
package org.jboss.as.appclient.deployment;

import org.jboss.as.ee.structure.DeploymentType;
import org.jboss.as.ee.structure.DeploymentTypeMarker;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;

import java.util.ArrayList;
import java.util.List;

import static org.jboss.as.appclient.logging.AppClientMessages.MESSAGES;

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
            throw MESSAGES.cannotFindAppClient(deploymentName);
        }
        if (appClients.size() == 1) {
            appClients.get(0).putAttachment(AppClientAttachments.START_APP_CLIENT, true);
        } else if (appClients.isEmpty()) {
            throw MESSAGES.cannotFindAppClient();
        } else {
            throw MESSAGES.multipleAppClientsFound();
        }

    }

    @Override
    public void undeploy(final DeploymentUnit context) {

    }
}
