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
package org.jboss.as.threads;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceController;


/**
 *
 * @author Alexey Loubyansky
 */
public class ThreadFactoryWriteAttributeHandler extends ThreadsWriteAttributeOperationHandler {

    public static final ThreadFactoryWriteAttributeHandler INSTANCE = new ThreadFactoryWriteAttributeHandler();

    private static final EnumSet<AttributeAccess.Flag> RESTART_NONE = EnumSet.of(AttributeAccess.Flag.RESTART_NONE);
    private static final EnumSet<AttributeAccess.Flag> RESTART_ALL = EnumSet.of(AttributeAccess.Flag.RESTART_ALL_SERVICES);

    private final Map<String, AttributeDefinition> attributes = new HashMap<String, AttributeDefinition>();
    private final Map<String, AttributeDefinition> runtimeAttributes = new HashMap<String, AttributeDefinition>();

    private ThreadFactoryWriteAttributeHandler() {
        for(AttributeDefinition attr : ThreadFactoryAdd.ATTRIBUTES) {
            attributes.put(attr.getName(), attr);
        }
        for(AttributeDefinition attr : ThreadFactoryAdd.RW_ATTRIBUTES) {
            runtimeAttributes.put(attr.getName(), attr);
        }
    }

    public void registerAttributes(final ManagementResourceRegistration registry) {
        for(AttributeDefinition attr : ThreadFactoryAdd.ATTRIBUTES) {
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
    protected boolean applyUpdateToRuntime(final OperationContext context, final ModelNode operation,
                                           final String attributeName, final ModelNode newValue,
                                           final ModelNode currentValue) throws OperationFailedException {

        AttributeDefinition attr = runtimeAttributes.get(attributeName);
        if (attr == null) {
            // Not a runtime attribute; restart required
            return true;
        }
        else {
            final ServiceController<?> service = getService(context, operation);
            if (service == null) {
                // The service isn't installed, so the work done in the Stage.MODEL part is all there is to it
                return false;
            } else if (service.getState() != ServiceController.State.UP) {
                // Service is installed but not up?
                //throw new IllegalStateException(String.format("Cannot apply attribue %s to runtime; service %s is not in state %s, it is in state %s",
                //            attributeName, MessagingServices.JBOSS_MESSAGING, ServiceController.State.UP, hqService.getState()));
                // No, don't barf; just let the update apply to the model and put the server in a reload-required state
                return true;
            } else {
                // Actually apply the update
                applyOperation(operation, attributeName, service);
                return false;
            }

        }
    }

    @Override
    protected void revertUpdateToRuntime(final OperationContext context, final ModelNode operation,
                                         final String attributeName, final ModelNode valueToRestore,
                                         final ModelNode valueToRevert) throws OperationFailedException {

        if (runtimeAttributes.containsKey(attributeName)) {
            final ServiceController<?> service = getService(context, operation);
            if (service != null && service.getState() == ServiceController.State.UP) {
                // Create and execute a write-attribute operation that uses the valueToRestore
                ModelNode revertOp = operation.clone();
                revertOp.get(attributeName).set(valueToRestore);
                applyOperation(revertOp, attributeName, service);
            }
        }
    }

    private void applyOperation(ModelNode operation, String attributeName, ServiceController<?> service) {

        System.out.println("applyOperation " + attributeName);
        final ThreadFactoryService tf = (ThreadFactoryService) service.getService();
        try {
            if (CommonAttributes.GROUP_NAME.equals(attributeName)) {
                final ModelNode value = PoolAttributeDefinitions.GROUP_NAME.validateResolvedOperation(operation);
                tf.setThreadGroupName(value.isDefined() ? value.asString() : null);
            } else if(CommonAttributes.PRIORITY.equals(attributeName)) {
                final ModelNode value = PoolAttributeDefinitions.PRIORITY.validateResolvedOperation(operation);
                tf.setPriority(value.isDefined() ? value.asInt() : -1);
            } else {
                System.out.println("   not supported");
            }
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    protected ServiceController<?> getService(final OperationContext context, final ModelNode operation) {
        final String name = Util.getNameFromAddress(operation.require(OP_ADDR));
        return context.getServiceRegistry(true).getService(ThreadsServices.threadFactoryName(name));
    }
}
