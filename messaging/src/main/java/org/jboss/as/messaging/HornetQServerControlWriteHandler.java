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

import java.util.HashMap;
import java.util.Map;

import org.hornetq.api.core.management.HornetQServerControl;
import org.hornetq.core.server.HornetQServer;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
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
        if (attr != null) {
            ServiceRegistry registry = context.getServiceRegistry(true);
            ServiceController<?> hqService = registry.getService(MessagingServices.JBOSS_MESSAGING);
            if (hqService != null && hqService.getState() == ServiceController.State.UP) {

                HornetQServerControl serverControl = HornetQServer.class.cast(hqService.getValue()).getHornetQServerControl();
                try {
                    if (attributeName.equals(CommonAttributes.FAILOVER_ON_SHUTDOWN.getName()))  {
                        serverControl.setFailoverOnServerShutdown(newValue.resolve().asBoolean());
                    } else if (attributeName.equals(CommonAttributes.MESSAGE_COUNTER_SAMPLE_PERIOD.getName())) {
                        serverControl.setMessageCounterSamplePeriod(newValue.resolve().asLong());
                    } else if (attributeName.equals(CommonAttributes.MESSAGE_COUNTER_MAX_DAY_HISTORY.getName())) {
                        serverControl.setMessageCounterMaxDayCount(newValue.resolve().asInt());
                    } else if (attributeName.equals(CommonAttributes.MESSAGE_COUNTER_ENABLED.getName())) {
                        boolean enabled = newValue.resolve().asBoolean();
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

                return false;

            } else {
                throw new IllegalStateException(String.format("Cannot apply attribue %s ti runtime; service %s is not in state %s, it is in state %s",
                            attributeName, MessagingServices.JBOSS_MESSAGING, ServiceController.State.UP, hqService.getState()));
            }

        } else {
            // Not a runtime attribute; restart required
            return true;
        }
    }

}
