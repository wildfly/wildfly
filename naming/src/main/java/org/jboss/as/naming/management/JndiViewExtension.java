/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.naming.management;

import org.jboss.as.controller.OperationFailedException;

/**
 * An extension to the JndiViewOperation.  This will be executed along with the normal JndiViewOperation.
 *
 * @author John Bailey
 */
public interface JndiViewExtension {
    /**
     * Execute the extension and provide additional JNDI information in the result.
     *
     * @param context The extension context.
     * @throws OperationFailedException
     */
    void execute(final JndiViewExtensionContext context) throws OperationFailedException;
}
