/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.iiop.openjdk.deployment;

import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;


/**
 * Processor responsible for marking a deployment as using IIOP
 *
 * @author Stuart Douglas
 */
public class IIOPMarkerProcessor implements DeploymentUnitProcessor{
    @Override
    public void deploy(DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        //for now we mark all subsystems as using IIOP if the IIOP subsystem is installed
        IIOPDeploymentMarker.mark(deploymentUnit);
    }
}
