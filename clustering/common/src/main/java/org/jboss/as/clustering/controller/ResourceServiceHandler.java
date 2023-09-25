/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.controller;

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.dmr.ModelNode;

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
}
