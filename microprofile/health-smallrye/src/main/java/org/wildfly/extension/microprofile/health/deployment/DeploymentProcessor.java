/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2017, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
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

package org.wildfly.extension.microprofile.health.deployment;

import org.jboss.as.controller.capability.CapabilityServiceSupport;
import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.weld.WeldCapability;
import org.jboss.modules.Module;
import org.wildfly.extension.microprofile.health.HealthReporter;
import org.wildfly.extension.microprofile.health.MicroProfileHealthSubsystemDefinition;
import org.wildfly.extension.microprofile.health._private.MicroProfileHealthLogger;
import static org.jboss.as.weld.Capabilities.WELD_CAPABILITY_NAME;

/**
 *
 */
public class DeploymentProcessor implements DeploymentUnitProcessor {

    @Override
    public void deploy(DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        Module module = deploymentUnit.getAttachment(Attachments.MODULE);

        final CapabilityServiceSupport support = deploymentUnit.getAttachment(Attachments.CAPABILITY_SERVICE_SUPPORT);

        final WeldCapability weldCapability;
        try {
            weldCapability = support.getCapabilityRuntimeAPI(WELD_CAPABILITY_NAME, WeldCapability.class);
        } catch (CapabilityServiceSupport.NoSuchCapabilityException e) {
            //We should not be here since the subsystem depends on weld capability. Just in case ...
            throw MicroProfileHealthLogger.LOGGER.deploymentRequiresCapability(
                    deploymentUnit.getName(),
                    WELD_CAPABILITY_NAME);
        }

        boolean isRootDeploymentUnit = deploymentUnit.getParent() == null;
        boolean hasWeld = weldCapability.isPartOfWeldDeployment(deploymentUnit);
        if (hasWeld && isRootDeploymentUnit) {
            // Register extension for the parent deployment module only
            final HealthReporter healthReporter = (HealthReporter) phaseContext.getServiceRegistry().getService(MicroProfileHealthSubsystemDefinition.HEALTH_REPORTER_SERVICE).getValue();

            weldCapability.registerExtensionInstance(new CDIExtension(healthReporter, module), deploymentUnit);
        }

    }

    @Override
    public void undeploy(DeploymentUnit context) {

    }
}
