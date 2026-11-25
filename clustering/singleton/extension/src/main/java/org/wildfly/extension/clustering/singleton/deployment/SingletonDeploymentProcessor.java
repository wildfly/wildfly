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
import org.wildfly.clustering.singleton.service.ServiceTargetFactory;
import org.wildfly.extension.clustering.singleton.SingletonLogger;

/**
 * DUP that attaches the singleton DeploymentUnitPhaseBuilder if a deployment policy is attached.
 * @author Paul Ferraro
 */
public class SingletonDeploymentProcessor implements DeploymentUnitProcessor, LifecycleListener {

    static final AttachmentKey<ServiceTargetFactory> SERVICE_TARGET_FACTORY_KEY = AttachmentKey.create(ServiceTargetFactory.class);

    @Override
    public void deploy(DeploymentPhaseContext context) throws DeploymentUnitProcessingException {
        DeploymentUnit unit = context.getDeploymentUnit();
        if (unit.getParent() == null) {
            ServiceTargetFactory factory = context.getAttachment(SERVICE_TARGET_FACTORY_KEY);
            if (factory != null) {
                CapabilityServiceSupport support = unit.getAttachment(Attachments.CAPABILITY_SERVICE_SUPPORT);
                // Ideally, we would just install the next phase using the singleton policy, however deployment unit phases do not currently support restarts
                // Restart the deployment using the attached ServiceTarget transformer, but only if a transformer was not already attached
                if (unit.putAttachment(Attachments.DEPLOYMENT_UNIT_PHASE_SERVICE_TARGET_TRANSFORMER, new SingletonDeploymentServiceTargetTransformer(support, factory)) == null) {
                    SingletonLogger.ROOT_LOGGER.singletonDeploymentDetected(factory);
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
