/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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

package org.jboss.as.ejb3.subsystem;

import java.util.concurrent.atomic.AtomicBoolean;

import org.jboss.as.controller.AbstractWriteAttributeHandler;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.dmr.ModelNode;

/**
 * Write handler for the default missing method permission attribute
 *
 * @author Stuart Douglas
 */
class EJBDefaultMissingMethodPermissionsWriteHandler extends AbstractWriteAttributeHandler<Void> {

    private final AttributeDefinition attributeDefinition;
    private final AtomicBoolean denyAccessByDefault;

    EJBDefaultMissingMethodPermissionsWriteHandler(final AttributeDefinition attributeDefinition, final AtomicBoolean denyAccessByDefault) {
        super(attributeDefinition);
        this.attributeDefinition = attributeDefinition;
        this.denyAccessByDefault = denyAccessByDefault;
    }

    @Override
    protected boolean applyUpdateToRuntime(OperationContext context, ModelNode operation, String attributeName,
                                           ModelNode resolvedValue, ModelNode currentValue, HandbackHolder<Void> handbackHolder) throws OperationFailedException {
        final ModelNode model = context.readResource(PathAddress.EMPTY_ADDRESS).getModel();
        updateDefaultMethodPermissionsDenyAccess(context, model);

        return false;
    }

    @Override
    protected void revertUpdateToRuntime(OperationContext context, ModelNode operation, String attributeName,
                                         ModelNode valueToRestore, ModelNode valueToRevert, Void handback) throws OperationFailedException {
        final ModelNode restored = context.readResource(PathAddress.EMPTY_ADDRESS).getModel().clone();
        restored.get(attributeName).set(valueToRestore);
        updateDefaultMethodPermissionsDenyAccess(context, restored);
    }

    private void updateDefaultMethodPermissionsDenyAccess(final OperationContext context, final ModelNode model) throws OperationFailedException {

        final ModelNode modelNode = this.attributeDefinition.resolveModelAttribute(context, model);
        final boolean value = modelNode.asBoolean();
        this.denyAccessByDefault.set(value);
    }

}
