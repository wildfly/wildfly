/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.transactions;

import javax.transaction.xa.XAResource;

import jakarta.transaction.RollbackException;
import jakarta.transaction.Status;
import jakarta.transaction.SystemException;
import jakarta.transaction.Transaction;
import jakarta.transaction.TransactionManager;
import jakarta.transaction.TransactionSynchronizationRegistry;
import org.jboss.as.test.shared.TimeoutUtil;
import org.junit.Assert;

/**
 * Transaction util class which works with transaction like
 * getting state, enlisting xa resource, adding synchronization...
 *
 * @author Ondra Chaloupka <ochaloup@redhat.com>
 */
public final class TxTestUtil {
    public static final int timeoutWaitTime_ms = 2500;

    private TxTestUtil() {
        // no instance here
    }

    public static TestXAResource enlistTestXAResource(TransactionManager tm, TransactionCheckerSingleton checker) {
        try {
            return enlistTestXAResource(tm.getTransaction(), checker);
        } catch (SystemException se) {
            throw new RuntimeException(String.format("Can't obtain transaction for transaction manager '%s' "
                    + "to enlist %s", tm, TestXAResource.class.getName()), se);
        }
    }

    public static TestXAResource enlistTestXAResource(Transaction txn, TransactionCheckerSingleton checker) {
        TestXAResource xaResource = new TestXAResource(checker);
        try {
            txn.enlistResource(xaResource);
        } catch (IllegalStateException | RollbackException | SystemException e) {
            throw new RuntimeException("Can't enlist test xa resource '" + xaResource + "'", e);
        }
        return xaResource;
    }

    public static void enlistTestXAResource(Transaction txn, XAResource xaResource) {
        try {
            txn.enlistResource(xaResource);
        } catch (IllegalStateException | RollbackException | SystemException e) {
            throw new RuntimeException("Can't enlist test xa resource '" + xaResource + "'", e);
        }
    }

    public static void addSynchronization(TransactionManager tm, TransactionCheckerSingletonRemote checker) {
        try {
            addSynchronization(tm.getTransaction(), checker);
        } catch (SystemException se) {
            throw new RuntimeException(String.format("Can't obtain transaction for transaction manager '%s' "
                    + "to enlist add test synchronization '%s'"), se);
        }
    }

    public static void addSynchronization(Transaction txn, TransactionCheckerSingletonRemote checker) {
        TestSynchronization synchro = new TestSynchronization(checker);
        try {
            txn.registerSynchronization(synchro);
        } catch (IllegalStateException | RollbackException | SystemException e) {
            throw new RuntimeException("Can't register synchronization '" + synchro + "' to txn '" + txn + "'", e);
        }
    }

    public static void addSynchronization(TransactionSynchronizationRegistry registry, TransactionCheckerSingletonRemote checker) {
        TestSynchronization synchro = new TestSynchronization(checker);
        try {
            registry.registerInterposedSynchronization(synchro);
        } catch (IllegalStateException e) {
            throw new RuntimeException("Can't register synchronization '" + synchro + "' to synchro registry '" + registry+ "'", e);
        }
    }

    public static void waitForTimeout(TransactionManager tm) throws SystemException, InterruptedException {
        // waiting for timeout
        long startTime = System.currentTimeMillis();
        while(System.currentTimeMillis() - startTime < TimeoutUtil.adjust(timeoutWaitTime_ms) && tm.getStatus() == Status.STATUS_ACTIVE) {
            Thread.sleep(200);
        }
    }

    public static void checkTransactionExists(TransactionManager tm, boolean isExpectTransaction) {
        try {
            Transaction tx = tm.getTransaction();

            if(!isExpectTransaction && tx != null && tx.getStatus() != Status.STATUS_NO_TRANSACTION) {
                Assert.fail("We do not expect transaction would be active - we haven't activated it in BMT bean");
            } else if (isExpectTransaction && (tx == null || tx.getStatus() != Status.STATUS_ACTIVE)) {
                Assert.fail("We do expect tranaction would be active - we have alredy activated it in BMT bean");
            }
        } catch (SystemException e) {
            throw new RuntimeException("Cannot get the current transaction from injected TransationManager!", e);
        }
    }

    public static String getStatusAsString(int statusCode) {
        switch(statusCode) {
            case Status.STATUS_ACTIVE: return "STATUS_ACTIVE";
            case Status.STATUS_MARKED_ROLLBACK: return "STATUS_MARKED_ROLLBACK";
            case Status.STATUS_PREPARED: return "STATUS_PREPARED";
            case Status.STATUS_COMMITTED: return "STATUS_COMMITTED";
            case Status.STATUS_ROLLEDBACK: return "STATUS_ROLLEDBACK";
            case Status.STATUS_UNKNOWN: return "STATUS_UNKNOWN";
            case Status.STATUS_NO_TRANSACTION: return "STATUS_NO_TRANSACTION";
            case Status.STATUS_PREPARING: return "STATUS_PREPARING";
            case Status.STATUS_COMMITTING: return "STATUS_COMMITTING";
            case Status.STATUS_ROLLING_BACK: return "STATUS_ROLLING_BACK";
            default:
                throw new IllegalStateException("Can't determine status code " + statusCode
                    + " as transaction status code defined under " + Status.class.getName());
        }
    }
}
