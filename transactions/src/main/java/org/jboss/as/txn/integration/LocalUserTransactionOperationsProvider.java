/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.txn.integration;

import org.jboss.tm.usertx.UserTransactionOperationsProvider;
import org.wildfly.transaction.client.LocalUserTransaction;

/**
 * Implementation of user transaction operations provider bound to
 * {@link LocalUserTransaction}.
 *
 * @author Ondra Chaloupka <ochaloup@redhat.com>
 */
public class LocalUserTransactionOperationsProvider implements UserTransactionOperationsProvider {

    @Override
    public boolean getAvailability() {
        return LocalUserTransaction.getInstance().isAvailable();
    }

    @Override
    public void setAvailability(boolean available) {
        LocalUserTransaction.getInstance().setAvailability(available);
    }

}
