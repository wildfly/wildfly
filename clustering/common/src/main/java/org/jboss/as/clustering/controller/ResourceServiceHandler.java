/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.controller;

import java.util.List;

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.dmr.ModelNode;
import org.wildfly.subsystem.resource.operation.ResourceOperationRuntimeHandler;

/**
 * Handles service installation and removal for use by {@link AddStepHandler} and {@link RemoveStepHandler}.
 * @author Paul Ferraro
 */
public interface ResourceServiceHandler {

    /**
     * Installs runtime services for a resource, configured from the specified model.
     * @param context the context of the add/remove operation
     * @param model the resource model
     * @throws OperationFailedException if service installation fails
     */
    void installServices(OperationContext context, ModelNode model) throws OperationFailedException;

    /**
     * Removes runtime services for a resource.
     * @param context the context of the add/remove operation
     * @param model the resource model
     * @throws OperationFailedException if service installation fails
     */
    void removeServices(OperationContext context, ModelNode model) throws OperationFailedException;

    /**
     * Temporary adapter to the wildfly-subsystem equivalent.
     */
    static ResourceServiceHandler of(ResourceOperationRuntimeHandler... handlers) {
        return of(List.of(handlers));
    }

    /**
     * Temporary adapter to the wildfly-subsystem equivalent.
     */
    static ResourceServiceHandler of(Iterable<? extends ResourceOperationRuntimeHandler> handlers) {
        return new ResourceServiceHandler() {
            @Override
            public void installServices(OperationContext context, ModelNode model) throws OperationFailedException {
                for (ResourceOperationRuntimeHandler handler : handlers) {
                    handler.addRuntime(context, model);
                }
            }

            @Override
            public void removeServices(OperationContext context, ModelNode model) throws OperationFailedException {
                for (ResourceOperationRuntimeHandler handler : handlers) {
                    handler.removeRuntime(context, model);
                }
            }
        };
    }
}
