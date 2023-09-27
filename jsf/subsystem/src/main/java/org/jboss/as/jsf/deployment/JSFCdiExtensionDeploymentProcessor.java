/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.jsf.deployment;

import static org.jboss.as.weld.Capabilities.WELD_CAPABILITY_NAME;

import org.jboss.as.controller.capability.CapabilityServiceSupport;
import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.weld.WeldCapability;

/**
 * {@link DeploymentUnitProcessor} which registers CDI extensions. Currently only registers
 * {@link JSFPassivatingViewScopedCdiExtension} to workaround WFLY-3044 / JAVASERVERFACES-3191.
 *
 * @author <a href="http://www.radoslavhusar.com/">Radoslav Husar</a>
 * @version Apr 10, 2014
 * @since 8.0.1
 */
public class JSFCdiExtensionDeploymentProcessor implements DeploymentUnitProcessor {

    public void deploy(DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        final DeploymentUnit parent = deploymentUnit.getParent() == null ? deploymentUnit : deploymentUnit.getParent();
        if(JsfVersionMarker.isJsfDisabled(deploymentUnit)) {
            return;
        }

        final CapabilityServiceSupport support = deploymentUnit.getAttachment(Attachments.CAPABILITY_SERVICE_SUPPORT);
        if (support.hasCapability(WELD_CAPABILITY_NAME)) {
            support.getOptionalCapabilityRuntimeAPI(WELD_CAPABILITY_NAME, WeldCapability.class).get()
                    .registerExtensionInstance(new JSFPassivatingViewScopedCdiExtension(), parent);
        }
    }
}