/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.controller;

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.registry.Resource;
import org.jboss.dmr.ModelNode;

/**
 * @author Paul Ferraro
 */
public class ReloadRequiredAddStepHandler extends AddStepHandler {

    public ReloadRequiredAddStepHandler(AddStepHandlerDescriptor descriptor) {
        super(descriptor);
    }

    @Override
    protected boolean requiresRuntime(OperationContext context) {
        return !context.isBooting() && context.isDefaultRequiresRuntime();
    }

    @Override
    protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model) {
        context.reloadRequired();
    }

    @Override
    protected void rollbackRuntime(OperationContext context, ModelNode operation, Resource resource) {
        context.revertReloadRequired();
    }
}
