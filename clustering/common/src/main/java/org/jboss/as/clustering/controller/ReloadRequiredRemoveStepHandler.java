/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.controller;

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.registry.OperationEntry;
import org.jboss.dmr.ModelNode;

/**
 * @author Paul Ferraro
 */
public class ReloadRequiredRemoveStepHandler extends RemoveStepHandler {

    public ReloadRequiredRemoveStepHandler(RemoveStepHandlerDescriptor descriptor) {
        super(descriptor, OperationEntry.Flag.RESTART_ALL_SERVICES);
    }

    @Override
    protected boolean requiresRuntime(OperationContext context) {
        return context.isDefaultRequiresRuntime();
    }

    @Override
    protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model) {
        context.reloadRequired();
    }

    @Override
    protected void recoverServices(OperationContext context, ModelNode operation, ModelNode model) {
        context.revertReloadRequired();
    }
}
