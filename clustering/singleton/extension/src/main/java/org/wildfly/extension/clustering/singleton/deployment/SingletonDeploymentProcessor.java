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
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.msc.service.LifecycleEvent;
import org.jboss.msc.service.LifecycleListener;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceController.Mode;
import org.jboss.msc.service.ServiceName;
import org.wildfly.clustering.singleton.service.ServiceTargetFactory;
import org.wildfly.extension.clustering.singleton.SingletonLogger;

/**
 * DUP that adds a dependency on a configured deployment policy service to the next phase.
 * @author Paul Ferraro
 */
public class SingletonDeploymentProcessor implements DeploymentUnitProcessor, LifecycleListener {

    public static final AttachmentKey<SingletonDeploymentConfiguration> CONFIGURATION_KEY = AttachmentKey.create(SingletonDeploymentConfiguration.class);

    @Override
    public void deploy(DeploymentPhaseContext context) {
        DeploymentUnit unit = context.getDeploymentUnit();
        if (unit.getParent() == null) {
            SingletonDeploymentConfiguration config = unit.getAttachment(CONFIGURATION_KEY);
            if (config != null) {
                CapabilityServiceSupport support = unit.getAttachment(Attachments.CAPABILITY_SERVICE_SUPPORT);
                ServiceName name = support.getCapabilityServiceName(ServiceTargetFactory.SERVICE_DESCRIPTOR, config.getPolicy());
                // Ideally, we would just install the next phase using the singleton policy, however deployment unit phases do not currently support restarts
                // Restart the deployment using the attached ServiceTarget transformer, but only if a transformer was not already attached
                if (unit.putAttachment(Attachments.DEPLOYMENT_UNIT_PHASE_SERVICE_INSTALLER_TRANSFORMER, new SingletonDeploymentServiceInstallerTransformer(name)) == null) {
                    SingletonLogger.ROOT_LOGGER.singletonDeploymentDetected(name.getCanonicalName());
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
