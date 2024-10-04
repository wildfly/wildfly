/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.ee.concurrent;

import org.glassfish.enterprise.concurrent.spi.TransactionHandle;
import org.glassfish.enterprise.concurrent.spi.TransactionSetupProvider;
import org.jboss.as.ee.logging.EeLogger;
import org.wildfly.transaction.client.ContextTransactionManager;

import jakarta.enterprise.concurrent.ManagedTask;
import jakarta.transaction.Transaction;
import jakarta.transaction.TransactionManager;

/**
 * The transaction setup provider handles transaction suspend/resume.
 *
 * @author Eduardo Martins
 */
public class TransactionSetupProviderImpl implements TransactionSetupProvider {

    private final transient TransactionManager transactionManager;

    /**
     */
    public TransactionSetupProviderImpl() {
        this.transactionManager = ContextTransactionManager.getInstance();
    }

    @Override
    public TransactionHandle beforeProxyMethod(String transactionExecutionProperty) {
        Transaction transaction = null;
        if (!ManagedTask.USE_TRANSACTION_OF_EXECUTION_THREAD.equals(transactionExecutionProperty)) {
            try {
                transaction = transactionManager.suspend();
            } catch (Throwable e) {
                EeLogger.ROOT_LOGGER.debug("failed to suspend transaction",e);
            }
        }
        return new TransactionHandleImpl(transaction);
    }

    @Override
    public void afterProxyMethod(TransactionHandle transactionHandle, String transactionExecutionProperty) {
        final Transaction transaction = ((TransactionHandleImpl) transactionHandle).getTransaction();
        if (transaction != null) {
            try {
                transactionManager.resume(transaction);
            } catch (Throwable e) {
                EeLogger.ROOT_LOGGER.debug("failed to resume transaction",e);
            }
        }
    }

    private static class TransactionHandleImpl implements TransactionHandle {

        private final Transaction transaction;

        private TransactionHandleImpl(Transaction transaction) {
            this.transaction = transaction;
        }

        public Transaction getTransaction() {
            return transaction;
        }
    }
}
