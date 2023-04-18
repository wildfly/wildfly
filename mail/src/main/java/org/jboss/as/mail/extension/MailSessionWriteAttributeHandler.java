/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2023, Red Hat, Inc., and individual contributors
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
