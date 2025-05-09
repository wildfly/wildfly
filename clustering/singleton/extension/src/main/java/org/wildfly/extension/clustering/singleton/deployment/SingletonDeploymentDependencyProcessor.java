/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.clustering.singleton.deployment;

import org.jboss.as.controller.capability.CapabilityServiceSupport;
import org.jboss.as.server.deployment.AttachmentKey;
import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.wildfly.clustering.singleton.service.ServiceTargetFactory;

/**
 * DUP that adds a dependency on a configured deployment policy service to the next phase.
 * @author Paul Ferraro
 */
public class SingletonDeploymentDependencyProcessor implements DeploymentUnitProcessor {

    public static final AttachmentKey<SingletonDeploymentConfiguration> CONFIGURATION_KEY = AttachmentKey.create(SingletonDeploymentConfiguration.class);

    @Override
    public void deploy(DeploymentPhaseContext context) throws DeploymentUnitProcessingException {
        DeploymentUnit unit = context.getDeploymentUnit();
        if (unit.getParent() == null) {
            SingletonDeploymentConfiguration config = unit.getAttachment(CONFIGURATION_KEY);
            if (config != null) {
                CapabilityServiceSupport support = unit.getAttachment(Attachments.CAPABILITY_SERVICE_SUPPORT);
                context.addDependency(support.getCapabilityServiceName(ServiceTargetFactory.SERVICE_DESCRIPTOR, config.getPolicy()), SingletonDeploymentProcessor.SERVICE_TARGET_FACTORY_KEY);
            }
        }
    }
}
