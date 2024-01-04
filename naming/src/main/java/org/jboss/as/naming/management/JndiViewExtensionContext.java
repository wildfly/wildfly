/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.naming.management;

import javax.naming.Context;
import javax.naming.NamingException;
import org.jboss.as.controller.OperationContext;
import org.jboss.dmr.ModelNode;

/**
 * Context providing required information for JndiView extensions.
 *
 * @author John Bailey
 */
public interface JndiViewExtensionContext {
    /**
     * Get the operation context.
     *
     * @return The operation context.
     */
    OperationContext getOperationContext();

    /**
     * Get the operation result.
     *
     * @return The operation result.
     */
    ModelNode getResult();

    /**
     * Add all the entries from the provided context into the provided model node.
     *
     * @param current The current result node.
     * @param context The current naming context.
     */
    void addEntries(final ModelNode current, final Context context) throws NamingException;
}
