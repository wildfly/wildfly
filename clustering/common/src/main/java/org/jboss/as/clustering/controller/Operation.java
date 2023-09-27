/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.clustering.controller;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.ExpressionResolver;
import org.jboss.as.controller.OperationDefinition;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.registry.OperationEntry;
import org.jboss.dmr.ModelNode;

/**
 * Interface to be implemented by operation enumerations.
 *
 * @param <C> operation context
 * @author Paul Ferraro
 * @author Radoslav Husar
 */
public interface Operation<C> extends Definable<OperationDefinition> {

    final AttributeDefinition[] NO_ATTRIBUTES = new AttributeDefinition[0];

    default String getName() {
        return this.getDefinition().getName();
    }

    default boolean isReadOnly() {
        return this.getDefinition().getFlags().contains(OperationEntry.Flag.READ_ONLY);
    }

    default AttributeDefinition[] getParameters() {
        return NO_ATTRIBUTES;
    }

    /**
     * Execute against the specified context.
     *
     * @param expressionResolver an expression resolver
     * @param operation original operation model to resolve parameters from
     * @param context an execution context
     * @return the execution result (possibly null).
     */
    ModelNode execute(ExpressionResolver expressionResolver, ModelNode operation, C context) throws OperationFailedException;
}
