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

package org.jboss.as.server.operations;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;

import org.jboss.as.controller.AbstractWriteAttributeHandler;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.operations.common.ProcessEnvironmentSystemPropertyUpdater;
import org.jboss.dmr.ModelNode;

/**
 * Handles changes to the value of a system property.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public class SystemPropertyValueWriteAttributeHandler extends AbstractWriteAttributeHandler<SystemPropertyValueWriteAttributeHandler.SysPropValue> {

    private final ProcessEnvironmentSystemPropertyUpdater systemPropertyUpdater;

    public SystemPropertyValueWriteAttributeHandler(ProcessEnvironmentSystemPropertyUpdater systemPropertyUpdater, AttributeDefinition valueAttribute) {
        super(valueAttribute);
        this.systemPropertyUpdater = systemPropertyUpdater;
    }

    @Override
    protected boolean requiresRuntime(OperationContext context) {
        return systemPropertyUpdater != null;
    }

    @Override
    protected boolean applyUpdateToRuntime(OperationContext context, ModelNode operation, String attributeName,
                                           ModelNode resolvedValue, ModelNode currentValue, HandbackHolder<SysPropValue> handbackHolder) throws OperationFailedException {

        final String name = PathAddress.pathAddress(operation.get(OP_ADDR)).getLastElement().getValue();
        String setValue = resolvedValue.isDefined() ? resolvedValue.asString() : null;
        // This method will only be called if systemPropertyUpdater != null (see requiresRuntime())
        final boolean applyToRuntime = systemPropertyUpdater.isRuntimeSystemPropertyUpdateAllowed(name, setValue, context.isBooting());

        if (applyToRuntime) {
            final String oldValue = SecurityActions.getSystemProperty(name);
            if (setValue != null) {
                SecurityActions.setSystemProperty(name, setValue);
            } else {
                SecurityActions.clearSystemProperty(name);
            }
            systemPropertyUpdater.systemPropertyUpdated(name, setValue);

            handbackHolder.setHandback(new SysPropValue(name, oldValue));
        }

        return !applyToRuntime;
    }

    @Override
    protected void revertUpdateToRuntime(OperationContext context, ModelNode operation, String attributeName,
                                         ModelNode valueToRestore, ModelNode valueToRevert, SysPropValue handback) throws OperationFailedException {
        if (handback != null) {
            if (handback.value != null) {
                SecurityActions.setSystemProperty(handback.name, handback.value);
            } else {
                SecurityActions.clearSystemProperty(handback.name);
            }

            systemPropertyUpdater.systemPropertyUpdated(handback.name, handback.value);

        }
    }

    public static class SysPropValue {
        private final String name;
        private final String value;

        private SysPropValue(String name, String value) {
            this.name = name;
            this.value = value;
        }
    }
}
