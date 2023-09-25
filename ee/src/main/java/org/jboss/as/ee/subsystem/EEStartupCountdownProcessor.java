/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.ee.subsystem;

import org.jboss.as.ee.component.Attachments;
import org.jboss.as.ee.component.EEModuleDescription;
import org.jboss.as.ee.component.deployers.StartupCountdown;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;

public class EEStartupCountdownProcessor implements DeploymentUnitProcessor {
    @Override
    public void deploy(DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        final EEModuleDescription moduleDescription = deploymentUnit.getAttachment(Attachments.EE_MODULE_DESCRIPTION);
        final int startupBeansCount = moduleDescription.getStartupBeansCount();
        if (deploymentUnit.getParent() == null) {
            deploymentUnit.putAttachment(Attachments.STARTUP_COUNTDOWN, new StartupCountdown(startupBeansCount));
        } else {
            final StartupCountdown countdown = deploymentUnit.getParent().getAttachment(Attachments.STARTUP_COUNTDOWN);
            // copy ref to child deployment
            deploymentUnit.putAttachment(Attachments.STARTUP_COUNTDOWN, countdown);
            countdown.countUp(startupBeansCount);
        }
    }
}
