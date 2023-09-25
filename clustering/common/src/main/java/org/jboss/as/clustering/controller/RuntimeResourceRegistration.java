/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.controller;

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;

/**
 * Encapsulates logic for runtime resource registration.
 * @author Paul Ferraro
 */
public interface RuntimeResourceRegistration {
    /**
     * Registers runtime resources as part of an add operation.
     * @param context an operation context
     */
    void register(OperationContext context) throws OperationFailedException;

    /**
     * Removes runtime resources created during {@link #register(OperationContext)}.
     * @param context an operation context
     */
    void unregister(OperationContext context) throws OperationFailedException;
}
