/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.txn.subsystem;

import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ReloadRequiredRemoveStepHandler;

/**
 * Removes the transaction subsystem root resource.
 *
 * @author Brian Stansberry
 */
class TransactionSubsystemRemove extends ReloadRequiredRemoveStepHandler {

    static final TransactionSubsystemRemove INSTANCE = new TransactionSubsystemRemove();

    private TransactionSubsystemRemove() {
    }

    /**
     * Suppresses removal of the log-store=log-store child, as that remove op handler is a no-op.
     */
    @Override
    protected boolean removeChildRecursively(PathElement child) {
        return !TransactionExtension.LOG_STORE_PATH.equals(child);
    }
}
