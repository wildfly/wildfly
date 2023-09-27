/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.jpa.transaction;

import jakarta.transaction.TransactionManager;
import jakarta.transaction.TransactionSynchronizationRegistry;

import org.jboss.as.jpa.spi.JtaManager;
import org.wildfly.transaction.client.ContextTransactionManager;

/**
 * passes the TM and TSR into the persistence provider integration classes
 *
 * @author Scott Marlow
 */
public final class JtaManagerImpl implements JtaManager {

    private final TransactionSynchronizationRegistry transactionSynchronizationRegistry;

    public JtaManagerImpl(TransactionSynchronizationRegistry transactionSynchronizationRegistry) {
        this.transactionSynchronizationRegistry = transactionSynchronizationRegistry;
    }

    @Override
    public TransactionSynchronizationRegistry getSynchronizationRegistry() {
        return transactionSynchronizationRegistry;
    }

    @Override
    public TransactionManager locateTransactionManager() {
        return ContextTransactionManager.getInstance();
    }
}
