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
package org.jboss.as.provision.parser;

import org.jboss.as.controller.AbstractWriteAttributeHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.dmr.ModelNode;

/**
 * @author Thomas.Diesler@jboss.com
 * @since 03-May-2013
 */
public class ProvisionFrameworkPropertyWrite extends AbstractWriteAttributeHandler<Object> {

    private final SubsystemState subsystemState;

    ProvisionFrameworkPropertyWrite(SubsystemState subsystemState) {
        super(ProvisionPropertyResource.VALUE);
        this.subsystemState = subsystemState;
    }

    @Override
    protected boolean requiresRuntime(OperationContext context) {
        return context.isNormalServer();
    }

    @Override
    protected boolean applyUpdateToRuntime(OperationContext context, ModelNode operation, String attributeName, ModelNode resolvedValue, ModelNode currentValue, HandbackHolder<Object> handbackHolder) throws OperationFailedException {
        return doUpdate(context, operation, resolvedValue);
    }

    @Override
    protected void revertUpdateToRuntime(OperationContext context, ModelNode operation, String attributeName, ModelNode valueToRestore, ModelNode valueToRevert, Object handback) throws OperationFailedException {
        doUpdate(context, operation, valueToRestore);
    }

    private boolean doUpdate(OperationContext context, ModelNode operation, ModelNode value) {
        String propName = operation.get(ModelDescriptionConstants.OP_ADDR).asObject().get(ModelConstants.PROPERTY).asString();
        String propValue = value.asString();
        subsystemState.setProperty(propName, propValue);
        return true;
    }
}
