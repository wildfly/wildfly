/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.infinispan.tx;

import jakarta.transaction.TransactionManager;

import org.infinispan.commons.tx.lookup.TransactionManagerLookup;

/**
 * @author Paul Ferraro
 */
public class TransactionManagerProvider implements TransactionManagerLookup {

    private final TransactionManager tm;

    public TransactionManagerProvider(TransactionManager tm) {
        this.tm = tm;
    }

    /**
     * {@inheritDoc}
     * @see org.infinispan.transaction.lookup.TransactionManagerLookup#getTransactionManager()
     */
    @Override
    public TransactionManager getTransactionManager() {
        return this.tm;
    }
}
