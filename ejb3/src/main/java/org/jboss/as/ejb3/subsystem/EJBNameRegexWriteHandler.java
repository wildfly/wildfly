/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.ejb3.subsystem;

import org.jboss.as.controller.AbstractWriteAttributeHandler;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.dmr.ModelNode;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * A write handler for adjusting the subsystem default for allowing regular expressions to be used
 * in EJB names used in deployment descriptors.
 *
 * @author Stuart
 * @author Richard Achmatowicz
 */
class EJBNameRegexWriteHandler extends AbstractWriteAttributeHandler<Void> {

    private final AttributeDefinition attributeDefinition;
    private final AtomicBoolean defaultAllowEjbRegex;

    public EJBNameRegexWriteHandler(final AttributeDefinition attributeDefinition, final AtomicBoolean defaultAllowEjbRegex) {
        super(attributeDefinition);
        this.attributeDefinition = attributeDefinition;
        this.defaultAllowEjbRegex = defaultAllowEjbRegex;
    }

    @Override
    protected boolean applyUpdateToRuntime(OperationContext context, ModelNode operation, String attributeName,
                                           ModelNode resolvedValue, ModelNode currentValue, HandbackHolder<Void> handbackHolder) throws OperationFailedException {
        final ModelNode model = context.readResource(PathAddress.EMPTY_ADDRESS).getModel();
        updateRegexAllowed(context, model);

        return false;
    }

    @Override
    protected void revertUpdateToRuntime(OperationContext context, ModelNode operation, String attributeName,
                                         ModelNode valueToRestore, ModelNode valueToRevert, Void handback) throws OperationFailedException {
        final ModelNode restored = context.readResource(PathAddress.EMPTY_ADDRESS).getModel().clone();
        restored.get(attributeName).set(valueToRestore);
        updateRegexAllowed(context, restored);
    }

    void updateRegexAllowed(final OperationContext context, final ModelNode model) throws OperationFailedException {

        final ModelNode allowRegexModelNode = this.attributeDefinition.resolveModelAttribute(context, model);
        boolean defaultAllowEjbRegex = allowRegexModelNode.isDefined() ? allowRegexModelNode.get().asBoolean() : false;
        this.defaultAllowEjbRegex.set(defaultAllowEjbRegex);
    }
}
