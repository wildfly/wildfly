/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.controller;

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.dmr.ModelNode;

/**
 * Remove operation handler that leverages a {@link ResourceServiceBuilderFactory} to restart a parent resource..
 * @author Paul Ferraro
 */
public class RestartParentResourceRemoveStepHandler extends RemoveStepHandler {

    private final OperationStepHandler handler;

    public RestartParentResourceRemoveStepHandler(ResourceServiceConfiguratorFactory parentFactory, RemoveStepHandlerDescriptor descriptor) {
        this(parentFactory, descriptor, null);
    }

    public RestartParentResourceRemoveStepHandler(ResourceServiceConfiguratorFactory parentFactory, RemoveStepHandlerDescriptor descriptor, ResourceServiceHandler handler) {
        super(descriptor, handler);
        this.handler = new RestartParentResourceStepHandler<>(parentFactory);
    }

    @Override
    public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
        super.execute(context, operation);

        this.handler.execute(context, operation);
    }
}
