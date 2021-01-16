/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2020 Red Hat, Inc., and individual contributors
 * as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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

    @Override
    public void undeploy(DeploymentUnit deploymentUnit) {

    }
}
