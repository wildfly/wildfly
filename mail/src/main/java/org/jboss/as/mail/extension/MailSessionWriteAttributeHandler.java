/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.mail.extension;

import java.util.List;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.ReloadRequiredWriteAttributeHandler;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.registry.Resource;
import org.jboss.dmr.ModelNode;
import org.wildfly.common.function.ExceptionBiConsumer;

/**
 * Write attribute operation handler for attributes of the session resource.
 * @author Paul Ferraro
 */
class MailSessionWriteAttributeHandler extends ReloadRequiredWriteAttributeHandler {

    private final ExceptionBiConsumer<OperationContext, ModelNode, OperationFailedException> remover;
    private final ExceptionBiConsumer<OperationContext, ModelNode, OperationFailedException> installer;

    MailSessionWriteAttributeHandler(AttributeDefinition attribute, ExceptionBiConsumer<OperationContext, ModelNode, OperationFailedException> remover, ExceptionBiConsumer<OperationContext, ModelNode, OperationFailedException> installer) {
        super(List.of(attribute));
        this.remover = remover;
        this.installer = installer;
    }

    @Override
    protected boolean applyUpdateToRuntime(OperationContext context, ModelNode operation, String attributeName, ModelNode resolvedValue, ModelNode currentValue, HandbackHolder<Void> handback) throws OperationFailedException {
        boolean updated = super.applyUpdateToRuntime(context, operation, attributeName, resolvedValue, currentValue, handback);
        if (updated) {
            PathAddress address = context.getCurrentAddress();
            if (context.isResourceServiceRestartAllowed() && this.getAttributeDefinition(attributeName).getFlags().contains(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES) && context.markResourceRestarted(address, this)) {
                this.remover.accept(context, context.getOriginalRootResource().navigate(address).getModel());
                this.installer.accept(context, Resource.Tools.readModel(context.readResource(PathAddress.EMPTY_ADDRESS)));
                // Returning false prevents going into reload required state
                return false;
            }
        }
        return updated;
    }

    @Override
    protected void revertUpdateToRuntime(OperationContext context, ModelNode operation, String attributeName, ModelNode valueToRestore, ModelNode resolvedValue, Void handback) throws OperationFailedException {
        PathAddress address = context.getCurrentAddress();
        if (context.isResourceServiceRestartAllowed() && this.getAttributeDefinition(attributeName).getFlags().contains(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES) && context.revertResourceRestarted(address, this)) {
            this.remover.accept(context, context.readResource(PathAddress.EMPTY_ADDRESS, false).getModel());
            this.installer.accept(context, Resource.Tools.readModel(context.getOriginalRootResource().navigate(address)));
        }
    }
}
