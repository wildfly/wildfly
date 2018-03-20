/*
 * JBoss, Home of Professional Open Source.
 * Copyright (c) 2016, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.jboss.as.test.integration.transactions;

import javax.transaction.RollbackException;
import javax.transaction.Status;
import javax.transaction.SystemException;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;
import javax.transaction.TransactionSynchronizationRegistry;
import javax.transaction.xa.XAResource;

import org.jboss.as.test.shared.TimeoutUtil;
import org.junit.Assert;

/**
 * Transaction util class which works with transaction like
 * getting state, enlisting xa resource, adding synchronization...
 *
 * @author Ondra Chaloupka <ochaloup@redhat.com>
 */
public final class TxTestUtil {
    private static final int timeoutWaitTime_ms = 2500;

    private TxTestUtil() {
        // no instance here
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
