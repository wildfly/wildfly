/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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

package org.wildfly.extension.messaging.activemq;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;

import org.apache.activemq.artemis.api.core.management.ActiveMQServerControl;
import org.apache.activemq.artemis.core.server.ActiveMQServer;
import org.jboss.as.controller.AbstractWriteAttributeHandler;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.logging.ControllerLogger;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.registry.ImmutableManagementResourceRegistration;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceRegistry;
import org.wildfly.extension.messaging.activemq.logging.MessagingLogger;

/**
 * Write attribute handler for attributes that update ActiveMQServerControl.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public class ActiveMQServerControlWriteHandler extends AbstractWriteAttributeHandler<Void> {

    public static final ActiveMQServerControlWriteHandler INSTANCE = new ActiveMQServerControlWriteHandler();

    private ActiveMQServerControlWriteHandler() {
        super(ServerDefinition.ATTRIBUTES);
    }

    public void registerAttributes(final ManagementResourceRegistration registry, boolean registerRuntimeOnly) {
        for (AttributeDefinition attr : ServerDefinition.ATTRIBUTES) {
            if (registerRuntimeOnly || !attr.getFlags().contains(AttributeAccess.Flag.STORAGE_RUNTIME)) {
                registry.registerReadWriteAttribute(attr, null, this);
            }
        }
    }

    @Override
    protected boolean applyUpdateToRuntime(final OperationContext context, final ModelNode operation, final String attributeName,
                                           final ModelNode newValue, final ModelNode currentValue,
                                           final HandbackHolder<Void> handbackHolder) throws OperationFailedException {
        AttributeDefinition attr = getAttributeDefinition(attributeName);
        if (attr.getFlags().contains(AttributeAccess.Flag.RESTART_ALL_SERVICES)) {
            // Restart required
            return true;
        } else {

            ServiceRegistry registry = context.getServiceRegistry(true);
            final ServiceName serviceName = MessagingServices.getActiveMQServiceName(PathAddress.pathAddress(operation.get(OP_ADDR)));
            ServiceController<?> service = registry.getService(serviceName);
            if (service == null) {
                // The service isn't installed, so the work done in the Stage.MODEL part is all there is to it
                return false;
            } else if (service.getState() != ServiceController.State.UP) {
                // Service is installed but not up?
                //throw new IllegalStateException(String.format("Cannot apply attribute %s to runtime; service %s is not in state %s, it is in state %s",
                //            attributeName, MessagingServices.JBOSS_MESSAGING, ServiceController.State.UP, service.getState()));
                // No, don't barf; just let the update apply to the model and put the server in a reload-required state
                return true;
            } else {
                if (!ActiveMQActivationService.isActiveMQServerActive(context, operation)) {
                    return false;
                }
                applyOperationToActiveMQService(operation, attributeName, newValue, service);
                return false;
            }
        }
    }

    @Override
    protected void revertUpdateToRuntime(final OperationContext context, final ModelNode operation,
                                         final String attributeName, final ModelNode valueToRestore,
                                         final ModelNode valueToRevert,
                                         final Void handback) throws OperationFailedException {

        AttributeDefinition attr = getAttributeDefinition(attributeName);
        if (!attr.getFlags().contains(AttributeAccess.Flag.RESTART_ALL_SERVICES)) {
            ServiceRegistry registry = context.getServiceRegistry(true);
            final ServiceName serviceName = MessagingServices.getActiveMQServiceName(PathAddress.pathAddress(operation.get(OP_ADDR)));
            ServiceController<?> service = registry.getService(serviceName);
            if (service != null && service.getState() == ServiceController.State.UP) {
                applyOperationToActiveMQService(operation, attributeName, valueToRestore, service);
            }
        }
    }

    private void applyOperationToActiveMQService(ModelNode operation, String attributeName, ModelNode newValue, ServiceController<?> activeMQServiceController) {
        ActiveMQServerControl serverControl = ActiveMQServer.class.cast(activeMQServiceController.getValue()).getActiveMQServerControl();
        if (serverControl == null) {
            PathAddress address = PathAddress.pathAddress(operation.require(OP_ADDR));
            throw ControllerLogger.ROOT_LOGGER.managementResourceNotFound(address);
        }
        try {
            if (attributeName.equals(ServerDefinition.MESSAGE_COUNTER_SAMPLE_PERIOD.getName())) {
                serverControl.setMessageCounterSamplePeriod(newValue.asLong());
            } else if (attributeName.equals(ServerDefinition.MESSAGE_COUNTER_MAX_DAY_HISTORY.getName())) {
                serverControl.setMessageCounterMaxDayCount(newValue.asInt());
            } else if (attributeName.equals(ServerDefinition.STATISTICS_ENABLED.getName())) {
                if (newValue.asBoolean()) {
                    serverControl.enableMessageCounters();
                } else {
                    serverControl.disableMessageCounters();
                }
            } else {
                // Bug! Someone added the attribute to the set but did not implement
                throw MessagingLogger.ROOT_LOGGER.unsupportedRuntimeAttribute(attributeName);
            }

        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }

    private static class MessageCounterEnabledHandler implements OperationStepHandler {

        @Override
        public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
            ModelNode aliased = getAliasedOperation(operation);
            context.addStep(aliased, getHandlerForOperation(context, operation), OperationContext.Stage.MODEL, true);
        }

        private static ModelNode getAliasedOperation(ModelNode operation) {
            ModelNode aliased = operation.clone();
            aliased.get(ModelDescriptionConstants.NAME).set(ServerDefinition.STATISTICS_ENABLED.getName());
            return aliased;
        }

        private static OperationStepHandler getHandlerForOperation(OperationContext context, ModelNode operation) {
            ImmutableManagementResourceRegistration imrr = context.getResourceRegistration();
            return imrr.getOperationHandler(PathAddress.EMPTY_ADDRESS, operation.get(OP).asString());
        }
    }
}
