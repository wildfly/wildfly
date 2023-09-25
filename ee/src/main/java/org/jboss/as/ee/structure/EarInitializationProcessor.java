/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.ee.structure;

import java.util.Locale;

import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;

/**
 * Processor responsible for detecting and marking EAR file deployments.
 *
 * @author John Bailey
 */
public class EarInitializationProcessor implements DeploymentUnitProcessor {
    private static final String EAR_EXTENSION = ".ear";

    public void deploy(DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();


        // Make sure this is an EAR deployment
        String deploymentName = deploymentUnit.getName().toLowerCase(Locale.ENGLISH);
        if (deploymentName.endsWith(EAR_EXTENSION)) {
            //  Let other processors know this is an EAR deployment
            DeploymentTypeMarker.setType(DeploymentType.EAR, deploymentUnit);
        }
    }
}
