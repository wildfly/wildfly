/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.controller;

import java.util.function.UnaryOperator;

import org.jboss.as.clustering.logging.ClusteringLogger;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.dmr.ModelNode;

/**
 * An {@link OperationStepHandler} decorator that fails when not running in admin-only mode.
 * @author Paul Ferraro
 */
public enum AdminOnlyOperationStepHandlerTransformer implements UnaryOperator<OperationStepHandler> {
    INSTANCE;

    @Override
    public OperationStepHandler apply(OperationStepHandler handler) {
        return new OperationStepHandler() {
            @Override
            public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
                if (context.isNormalServer()) {
                    throw ClusteringLogger.ROOT_LOGGER.operationNotSupportedInNormalServerMode(context.getCurrentAddress().toCLIStyleString(), context.getCurrentOperationName());
                }
                handler.execute(context, operation);
            }
        };
    }
}
