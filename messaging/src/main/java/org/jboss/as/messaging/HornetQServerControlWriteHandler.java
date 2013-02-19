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

package org.jboss.as.messaging;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.messaging.CommonAttributes.CLUSTERED;
import static org.jboss.as.messaging.MessagingMessages.MESSAGES;

import java.util.Set;

import org.hornetq.api.core.management.HornetQServerControl;
import org.hornetq.core.server.HornetQServer;
import org.jboss.as.controller.AbstractWriteAttributeHandler;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceRegistry;

/**
 * Write attribute handler for attributes that update HornetQServerControl.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public class HornetQServerControlWriteHandler extends AbstractWriteAttributeHandler<Void> {

    public static final HornetQServerControlWriteHandler INSTANCE = new HornetQServerControlWriteHandler();

    private HornetQServerControlWriteHandler() {
        super(CommonAttributes.SIMPLE_ROOT_RESOURCE_ATTRIBUTES);
    }

    public void registerAttributes(final ManagementResourceRegistration registry, boolean registerRuntimeOnly) {
        for (AttributeDefinition attr : CommonAttributes.SIMPLE_ROOT_RESOURCE_ATTRIBUTES) {
            if (registerRuntimeOnly || !attr.getFlags().contains(AttributeAccess.Flag.STORAGE_RUNTIME)) {
                if (attr.getName().equals(CLUSTERED.getName())) {
                    registry.registerReadWriteAttribute(CLUSTERED,
                            ClusteredAttributeHandlers.READ_HANDLER,
                            ClusteredAttributeHandlers.WRITE_HANDLER);
                } else {
                    registry.registerReadWriteAttribute(attr, null, this);
                }
            }
        }

        // handle deprecate attributes
        registry.registerReadWriteAttribute(CommonAttributes.LIVE_CONNECTOR_REF, null, new DeprecatedAttributeWriteHandler(CommonAttributes.LIVE_CONNECTOR_REF.getName()));
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
            final ServiceName hqServiceName = MessagingServices.getHornetQServiceName(PathAddress.pathAddress(operation.get(OP_ADDR)));
            ServiceController<?> hqService = registry.getService(hqServiceName);
            if (hqService == null) {
                // The service isn't installed, so the work done in the Stage.MODEL part is all there is to it
                return false;
            } else if (hqService.getState() != ServiceController.State.UP) {
                // Service is installed but not up?
                //throw new IllegalStateException(String.format("Cannot apply attribute %s to runtime; service %s is not in state %s, it is in state %s",
                //            attributeName, MessagingServices.JBOSS_MESSAGING, ServiceController.State.UP, hqService.getState()));
                // No, don't barf; just let the update apply to the model and put the server in a reload-required state
                return true;
            } else {
                applyOperationToHornetQService(context, operation, attributeName, hqService);
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
            final ServiceName hqServiceName = MessagingServices.getHornetQServiceName(PathAddress.pathAddress(operation.get(OP_ADDR)));
            ServiceController<?> hqService = registry.getService(hqServiceName);
            if (hqService != null && hqService.getState() == ServiceController.State.UP) {
                // Create and execute a write-attribute operation that uses the valueToRestore
                ModelNode revertOp = operation.clone();
                revertOp.get(attributeName).set(valueToRestore);
                applyOperationToHornetQService(context, revertOp, attributeName, hqService);
            }
        }
    }

    private void applyOperationToHornetQService(final OperationContext context, ModelNode operation, String attributeName, ServiceController<?> hqService) {
        HornetQServerControl serverControl = HornetQServer.class.cast(hqService.getValue()).getHornetQServerControl();
        try {
            if (attributeName.equals(CommonAttributes.FAILOVER_ON_SHUTDOWN.getName()))  {
                serverControl.setFailoverOnServerShutdown(CommonAttributes.FAILOVER_ON_SHUTDOWN.resolveModelAttribute(context, operation).asBoolean());
            } else if (attributeName.equals(CommonAttributes.MESSAGE_COUNTER_SAMPLE_PERIOD.getName())) {
                serverControl.setMessageCounterSamplePeriod(CommonAttributes.MESSAGE_COUNTER_SAMPLE_PERIOD.resolveModelAttribute(context, operation).asLong());
            } else if (attributeName.equals(CommonAttributes.MESSAGE_COUNTER_MAX_DAY_HISTORY.getName())) {
                serverControl.setMessageCounterMaxDayCount(CommonAttributes.MESSAGE_COUNTER_MAX_DAY_HISTORY.resolveModelAttribute(context, operation).asInt());
            } else if (attributeName.equals(CommonAttributes.MESSAGE_COUNTER_ENABLED.getName())) {
                boolean enabled = CommonAttributes.MESSAGE_COUNTER_ENABLED.resolveModelAttribute(context, operation).asBoolean();
                if (enabled) {
                    serverControl.enableMessageCounters();
                } else {
                    serverControl.disableMessageCounters();
                }
            } else {
                // Bug! Someone added the attribute to the set but did not implement
                throw MESSAGES.unsupportedRuntimeAttribute(attributeName);
            }

        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }

    /**
     * The clustered configuration parameter no longer exists for HornetQ configuration (a hornetq server is automatically clustered if it has cluster-connections)
     * but we continue to support it for legacy versions.
     *
     * For AS7 new versions, we compute its value based on the presence of cluster-connection children and ignore any write-attribute operation on it.
     * We only warn the user if he wants to disable the clustered state of the server by setting it to false.
     */
    private static final class ClusteredAttributeHandlers {
        static final OperationStepHandler READ_HANDLER = new OperationStepHandler() {
            @Override
            public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
                Set<String> clusterConnectionNames = context.getResourceRegistration().getChildNames(PathAddress.pathAddress(ClusterConnectionDefinition.PATH));
                boolean clustered = !clusterConnectionNames.isEmpty();
                context.getResult().set(clustered);
                context.stepCompleted();
            }
        };

        static final OperationStepHandler WRITE_HANDLER = new OperationStepHandler() {
            @Override
            public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
                // the real clustered HornetQ state
                Set<String> clusterConnectionNames = context.getResourceRegistration().getChildNames(PathAddress.pathAddress(ClusterConnectionDefinition.PATH));
                boolean clustered = !clusterConnectionNames.isEmpty();
                // whether the user wants the server to be clustered
                boolean wantsClustered = CLUSTERED.resolveModelAttribute(context, operation).asBoolean();
                if (clustered && !wantsClustered) {
                    PathAddress serverAddress = PathAddress.pathAddress(operation.get(OP_ADDR));
                    MessagingLogger.MESSAGING_LOGGER.canNotChangeClusteredAttribute(serverAddress);
                }
                // ignore the operation
                context.stepCompleted();
            }
        };
    }
}
