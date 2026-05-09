/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.microprofile.health.deployment;

import static org.jboss.as.weld.Capabilities.WELD_CAPABILITY_NAME;

import org.jboss.as.controller.capability.CapabilityServiceSupport;
import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.weld.WeldCapability;
import org.jboss.modules.Module;
import org.wildfly.extension.microprofile.health.MicroProfileHealthReporter;

/**
 * DUP that registers the MicroProfile Health CDI extension.
 */
public class DeploymentProcessor implements DeploymentUnitProcessor {

    @Override
    public void deploy(DeploymentPhaseContext phaseContext) {
        DeploymentUnit unit = phaseContext.getDeploymentUnit();
        CapabilityServiceSupport support = unit.getAttachment(Attachments.CAPABILITY_SERVICE_SUPPORT);
        try {
            WeldCapability weldCapability = support.getCapabilityRuntimeAPI(WELD_CAPABILITY_NAME, WeldCapability.class);

            if (weldCapability.isPartOfWeldDeployment(unit)) {
                Module module = unit.getAttachment(Attachments.MODULE);
                MicroProfileHealthReporter reporter = unit.getAttachment(MicroProfileHealthReporter.ATTACHMENT_KEY);

                weldCapability.registerExtensionInstance(new CDIExtension(reporter, module), unit);
            }
        } catch (CapabilityServiceSupport.NoSuchCapabilityException e) {
            throw new IllegalStateException(e);
        }
    }
}