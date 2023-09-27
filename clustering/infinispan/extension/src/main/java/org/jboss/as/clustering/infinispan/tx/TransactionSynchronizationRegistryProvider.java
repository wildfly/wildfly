/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.infinispan.tx;

import jakarta.transaction.TransactionSynchronizationRegistry;

import org.infinispan.transaction.lookup.TransactionSynchronizationRegistryLookup;

/**
 * Passes the TransactionSynchronizationRegistry to Infinispan.
 *
 * @author Scott Marlow
 */
public class TransactionSynchronizationRegistryProvider implements TransactionSynchronizationRegistryLookup {

    private final TransactionSynchronizationRegistry tsr;

    public TransactionSynchronizationRegistryProvider(TransactionSynchronizationRegistry tsr) {
        this.tsr = tsr;
    }

    @Override
    public TransactionSynchronizationRegistry getTransactionSynchronizationRegistry() {
        return this.tsr;
    }
}
