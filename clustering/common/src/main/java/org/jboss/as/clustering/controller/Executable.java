/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.controller;

import org.jboss.as.controller.OperationFailedException;
import org.jboss.dmr.ModelNode;

/**
 * Generic interface for some object capable of execution.
 * @author Paul Ferraro
 * @param <C> the execution context
 */
public interface Executable<C> {
    /**
     * Execute against the specified context.
     * @param context an execution context
     * @return the execution result (possibly null).
     */
    ModelNode execute(C context) throws OperationFailedException;
}
