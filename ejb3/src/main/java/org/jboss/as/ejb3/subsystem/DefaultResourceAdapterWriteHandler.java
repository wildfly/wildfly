/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.ejb3.subsystem;

import java.util.concurrent.atomic.AtomicReference;

import org.jboss.as.controller.AbstractWriteAttributeHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.dmr.ModelNode;

class DefaultResourceAdapterWriteHandler extends AbstractWriteAttributeHandler<Void> {

    private final AtomicReference<String> defaultResourceAdapterName;

    DefaultResourceAdapterWriteHandler(final AtomicReference<String> defaultResourceAdapterName) {
        super(EJB3SubsystemRootResourceDefinition.DEFAULT_RESOURCE_ADAPTER_NAME);
        this.defaultResourceAdapterName = defaultResourceAdapterName;
    }

    @Override
    protected boolean applyUpdateToRuntime(OperationContext context, ModelNode operation, String attributeName,
                                           ModelNode resolvedValue, ModelNode currentValue, HandbackHolder<Void> handbackHolder) throws OperationFailedException {
        final ModelNode model = context.readResource(PathAddress.EMPTY_ADDRESS).getModel();
        updateDefaultAdapterName(context, model);

        return false;
    }

    @Override
    protected void revertUpdateToRuntime(OperationContext context, ModelNode operation, String attributeName,
                                         ModelNode valueToRestore, ModelNode valueToRevert, Void handback) throws OperationFailedException {
        final ModelNode restored = context.readResource(PathAddress.EMPTY_ADDRESS).getModel().clone();
        restored.get(attributeName).set(valueToRestore);
        updateDefaultAdapterName(context, restored);
    }

    private void updateDefaultAdapterName(final OperationContext context, final ModelNode model) throws OperationFailedException {
        final ModelNode adapterNameNode = EJB3SubsystemRootResourceDefinition.DEFAULT_RESOURCE_ADAPTER_NAME.resolveModelAttribute(context, model);
        final String adapterName = adapterNameNode.isDefined() ? adapterNameNode.asString() : null;
        this.defaultResourceAdapterName.set(adapterName);
    }
}
