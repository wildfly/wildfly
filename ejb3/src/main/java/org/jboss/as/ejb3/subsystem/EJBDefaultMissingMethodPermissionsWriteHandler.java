/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
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
