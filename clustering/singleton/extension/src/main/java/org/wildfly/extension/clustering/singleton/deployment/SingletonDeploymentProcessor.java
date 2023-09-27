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
import org.jboss.msc.service.LifecycleEvent;
import org.jboss.msc.service.LifecycleListener;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceController.Mode;
import org.wildfly.clustering.singleton.SingletonPolicy;
import org.wildfly.extension.clustering.singleton.SingletonLogger;

/**
 * DUP that attaches the singleton DeploymentUnitPhaseBuilder if a deployment policy is attached.
 * @author Paul Ferraro
 */
@SuppressWarnings("removal")
public class SingletonDeploymentProcessor implements DeploymentUnitProcessor, LifecycleListener {

    public static final AttachmentKey<SingletonPolicy> POLICY_KEY = AttachmentKey.create(SingletonPolicy.class);

    @Override
    public void deploy(DeploymentPhaseContext context) throws DeploymentUnitProcessingException {
        DeploymentUnit unit = context.getDeploymentUnit();
        if (unit.getParent() == null) {
            SingletonPolicy policy = context.getAttachment(POLICY_KEY);
            if (policy != null) {
                CapabilityServiceSupport support = unit.getAttachment(Attachments.CAPABILITY_SERVICE_SUPPORT);
                // Ideally, we would just install the next phase using the singleton policy, however deployment unit phases do not currently support restarts
                // Restart the deployment using the attached phase builder, but only if a builder was not already attached
                if (unit.putAttachment(Attachments.DEPLOYMENT_UNIT_PHASE_BUILDER, new SingletonDeploymentUnitPhaseBuilder(support, policy)) == null) {
                    SingletonLogger.ROOT_LOGGER.singletonDeploymentDetected(policy);
                    ServiceController<?> controller = context.getServiceRegistry().getRequiredService(unit.getServiceName());
                    controller.addListener(this);
                    controller.setMode(Mode.NEVER);
                }
            }
        }
    }

    @Override
    public void handleEvent(ServiceController<?> controller, LifecycleEvent event) {
        if (event == LifecycleEvent.DOWN) {
            controller.setMode(Mode.ACTIVE);
            controller.removeListener(this);
        }
    }
}
