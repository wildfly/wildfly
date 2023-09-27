/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.controller;

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.dmr.ModelNode;

/**
 * Encapsulates the execution of a contextual executable.
 * @param <C> the execution context
 * @param <E> the contextual executable
 * @author Paul Ferraro
 */
public interface Executor<C, E extends Executable<C>> {
    /**
     * Executes the specified executable against the specified operation context.
     * @param context an operation context
     * @param executable the contextual executable object
     * @return the result of the execution (possibly null).
     * @throws OperationFailedException if execution fails
     */
    ModelNode execute(OperationContext context, E executable) throws OperationFailedException;
}
