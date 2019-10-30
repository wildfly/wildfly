/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2015, Red Hat, Inc., and individual contributors
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
@SuppressWarnings("deprecation")
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
    public void undeploy(DeploymentUnit unit) {
        // We intentionally leave any DEPLOYMENT_UNIT_PHASE_BUILDER attached to the deployment unit
    }

    @Override
    public void handleEvent(ServiceController<?> controller, LifecycleEvent event) {
        if (event == LifecycleEvent.DOWN) {
            controller.setMode(Mode.ACTIVE);
            controller.removeListener(this);
        }
    }
}
