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

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

import org.hornetq.api.core.management.HornetQServerControl;
import org.hornetq.core.server.HornetQServer;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.server.operations.ServerWriteAttributeOperationHandler;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceRegistry;

/**
 * Write attribute handler for attributes that update HornetQServerControl.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public class HornetQServerControlWriteHandler extends ServerWriteAttributeOperationHandler {

    public static final HornetQServerControlWriteHandler INSTANCE = new HornetQServerControlWriteHandler();

    private static final EnumSet<AttributeAccess.Flag> RESTART_NONE = EnumSet.of(AttributeAccess.Flag.RESTART_NONE);
    private static final EnumSet<AttributeAccess.Flag> RESTART_ALL = EnumSet.of(AttributeAccess.Flag.RESTART_ALL_SERVICES);

    private final Map<String, AttributeDefinition> attributes = new HashMap<String, AttributeDefinition>();
    private final Map<String, AttributeDefinition> runtimeAttributes = new HashMap<String, AttributeDefinition>();
    private HornetQServerControlWriteHandler() {
        for (AttributeDefinition attr : CommonAttributes.SIMPLE_ROOT_RESOURCE_ATTRIBUTES) {
            attributes.put(attr.getName(), attr);
        }
        for (AttributeDefinition attr : CommonAttributes.SIMPLE_ROOT_RESOURCE_WRITE_ATTRIBUTES) {
            runtimeAttributes.put(attr.getName(), attr);
        }
    }

    public void registerAttributes(final ManagementResourceRegistration registry) {
        for (AttributeDefinition attr : CommonAttributes.SIMPLE_ROOT_RESOURCE_ATTRIBUTES) {
            String attrName = attr.getName();
            EnumSet<AttributeAccess.Flag> flags = runtimeAttributes.containsKey(attrName) ? RESTART_NONE : RESTART_ALL;
            registry.registerReadWriteAttribute(attrName, null, this, flags);
        }
    }

    @Override
    protected void validateValue(String name, ModelNode value) throws OperationFailedException {
        AttributeDefinition attr = attributes.get(name);
        attr.getValidator().validateParameter(name, value);
    }

    @Override
    protected void validateResolvedValue(String name, ModelNode value) throws OperationFailedException {
        AttributeDefinition attr = attributes.get(name);
        attr.getValidator().validateResolvedParameter(name, value);
    }

    @Override
    protected boolean applyUpdateToRuntime(OperationContext context, ModelNode operation, String attributeName, ModelNode newValue, ModelNode currentValue) throws OperationFailedException {
        AttributeDefinition attr = runtimeAttributes.get(attributeName);
        if (attr == null) {
            // Not a runtime attribute; restart required
            return true;
        } else {
            ServiceRegistry registry = context.getServiceRegistry(true);
            ServiceController<?> hqService = registry.getService(MessagingServices.JBOSS_MESSAGING);
            if (hqService == null) {
                // The service isn't installed, so the work done in the Stage.MODEL part is all there is to it
                return false;
            } else if (hqService.getState() != ServiceController.State.UP) {
                // Service is installed but not up?
                //throw new IllegalStateException(String.format("Cannot apply attribue %s to runtime; service %s is not in state %s, it is in state %s",
                //            attributeName, MessagingServices.JBOSS_MESSAGING, ServiceController.State.UP, hqService.getState()));
                // No, don't barf; just let the update apply to the model and put the server in a reload-required state
                return true;
            } else {
                applyOperationToHornetQService(operation, attributeName, hqService);
                return false;

            }

        }
    }

    @Override
    protected void revertUpdateToRuntime(final OperationContext context, final ModelNode operation,
                                         final String attributeName, final ModelNode valueToRestore,
                                         final ModelNode valueToRevert) throws OperationFailedException {

        if (runtimeAttributes.containsKey(attributeName)) {
            ServiceRegistry registry = context.getServiceRegistry(true);
            ServiceController<?> hqService = registry.getService(MessagingServices.JBOSS_MESSAGING);
            if (hqService != null && hqService.getState() == ServiceController.State.UP) {
                // Create and execute a write-attribute operation that uses the valueToRestore
                ModelNode revertOp = operation.clone();
                revertOp.get(attributeName).set(valueToRestore);
                applyOperationToHornetQService(revertOp, attributeName, hqService);
            }
        }
    }

    private void applyOperationToHornetQService(ModelNode operation, String attributeName, ServiceController<?> hqService) {
        HornetQServerControl serverControl = HornetQServer.class.cast(hqService.getValue()).getHornetQServerControl();
        try {
            if (attributeName.equals(CommonAttributes.FAILOVER_ON_SHUTDOWN.getName()))  {
                serverControl.setFailoverOnServerShutdown(CommonAttributes.FAILOVER_ON_SHUTDOWN.validateResolvedOperation(operation).asBoolean());
            } else if (attributeName.equals(CommonAttributes.MESSAGE_COUNTER_SAMPLE_PERIOD.getName())) {
                serverControl.setMessageCounterSamplePeriod(CommonAttributes.MESSAGE_COUNTER_SAMPLE_PERIOD.validateResolvedOperation(operation).asLong());
            } else if (attributeName.equals(CommonAttributes.MESSAGE_COUNTER_MAX_DAY_HISTORY.getName())) {
                serverControl.setMessageCounterMaxDayCount(CommonAttributes.MESSAGE_COUNTER_MAX_DAY_HISTORY.validateResolvedOperation(operation).asInt());
            } else if (attributeName.equals(CommonAttributes.MESSAGE_COUNTER_ENABLED.getName())) {
                boolean enabled = CommonAttributes.MESSAGE_COUNTER_ENABLED.validateResolvedOperation(operation).asBoolean();
                if (enabled) {
                    serverControl.enableMessageCounters();
                } else {
                    serverControl.disableMessageCounters();
                }
            } else {
                // Bug! Someone added the attribute to the set but did not implement
                throw new UnsupportedOperationException(String.format("Runtime handling for %s is not implemented", attributeName));
            }

        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }

}
