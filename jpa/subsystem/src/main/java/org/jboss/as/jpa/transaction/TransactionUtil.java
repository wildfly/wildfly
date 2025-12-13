/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.jpa.transaction;

import static java.security.AccessController.doPrivileged;
import static org.jboss.as.jpa.messages.JpaLogger.ROOT_LOGGER;

import java.security.PrivilegedAction;

import jakarta.persistence.EntityManager;
import jakarta.transaction.Status;
import jakarta.transaction.Synchronization;
import jakarta.transaction.SystemException;
import jakarta.transaction.Transaction;
import jakarta.transaction.TransactionManager;
import jakarta.transaction.TransactionSynchronizationRegistry;

import org.jboss.as.jpa.container.ExtendedEntityManager;
import org.jboss.as.jpa.container.ScopedObjects;
import org.wildfly.transaction.client.AbstractTransaction;
import org.wildfly.transaction.client.AssociationListener;
import org.wildfly.transaction.client.ContextTransactionManager;

/**
 * Transaction utilities for Jakarta Persistence
 *
 * @author Scott Marlow (forked from code by Gavin King)
 */
public class TransactionUtil {
    public static boolean isInTx(TransactionManager transactionManager) {
        Transaction tx = getTransaction(transactionManager);
        if ( tx == null) {
            return false;
        }
        try {
            return tx.getStatus() == Status.STATUS_ACTIVE || tx.getStatus() == Status.STATUS_MARKED_ROLLBACK;
        } catch (SystemException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Get current persistence context.  Only call while a transaction is active in the current thread.
     *
     * @param puScopedName
     * @return
     */
    public static EntityManager getTransactionScopedEntityManager(String puScopedName, TransactionSynchronizationRegistry tsr) {
        return getScopedObjectInTransactionRegistry(EntityManager.class, puScopedName, tsr);
    }

    public static <T extends AutoCloseable> T getScopedObjectInTransactionRegistry(Class<T> type, String scopedPuName, TransactionSynchronizationRegistry tsr) {
        @SuppressWarnings("unchecked")
        ScopedObjects objs = (ScopedObjects) tsr.getResource(scopedPuName);
        return objs == null ? null : objs.get(type);
    }

    public static void registerSynchronization(AutoCloseable transactionScoped, String puScopedName, TransactionSynchronizationRegistry tsr, TransactionManager transactionManager) {
        SessionSynchronization sessionSynchronization = new SessionSynchronization(transactionScoped, puScopedName);
        tsr.registerInterposedSynchronization(sessionSynchronization);
        final AbstractTransaction transaction = ((ContextTransactionManager) transactionManager).getTransaction();
        doPrivileged((PrivilegedAction<Void>) () -> {
            transaction.registerAssociationListener(sessionSynchronization);
            return null;
        });
    }

    public static Transaction getTransaction(TransactionManager transactionManager) {
        try {
            return transactionManager.getTransaction();
        } catch (SystemException e) {
            throw ROOT_LOGGER.errorGettingTransaction(e);
        }
    }

    private static String currentThread() {
        return Thread.currentThread().getName();
    }

    public static String getTransactionScopedObjectDetails(AutoCloseable closeable, String scopedPuName) {
        String result = currentThread() + ":";  // show the thread for correlation with other modules
        if (closeable instanceof ExtendedEntityManager) { // hack to avoid dep on ExtendedEntityManager
            result += closeable.toString();
        } else if (closeable instanceof EntityManager) {
            result += "transaction scoped EntityManager [" + scopedPuName + "]";
        } else {
            result += "transaction scoped StatelessSession [" + scopedPuName + "]";
        }
        return result;
    }

    /**
     * Save the specified EntityManager in the local threads active transaction.  The TransactionSynchronizationRegistry
     * will clear the reference to the EntityManager when the transaction completes.
     *
     * @param scopedPuName
     * @param entityManager
     */
    public static void putEntityManagerInTransactionRegistry(String scopedPuName, EntityManager entityManager, TransactionSynchronizationRegistry tsr) {
        putScopedObjectInTransactionRegistry(scopedPuName, entityManager, tsr);
    }

    /**
     * Save the specified EntityManager or StatelessSession in the local thread's active transaction.
     * The TransactionSynchronizationRegistry will clear the reference to the object when the transaction completes.
     *
     * @param scopedPuName
     * @param scopedObject
     */
    public static <T extends AutoCloseable> void putScopedObjectInTransactionRegistry(String scopedPuName, T scopedObject, TransactionSynchronizationRegistry tsr) {
        @SuppressWarnings("unchecked")
        ScopedObjects objects = (ScopedObjects) tsr.getResource(scopedPuName);
        if (objects == null) {
            objects = new ScopedObjects();
            tsr.putResource(scopedPuName, objects);
        }
        objects.set(scopedObject);
    }

    /**
     * The AssociationListener helps protect against a non-application thread closing the entity manager at the same
     * time that the application thread may be using the entity manager.  We only close the entity manager after the
     * Synchronization.afterCompletion has been triggered and zero threads are associated with the transaction.
     *
     * We know when the application thread is associated with the transaction and can defer closing the EntityManager
     * until both conditions are met:
     *
     *   1. application thread is disassociated from transaction
     *   2. Synchronization.afterCompletion has been called
     *
     *   Note that entity managers do not get propagated on remote Jakarta Enterprise Beans invocations.
     *
     * See discussions for more details about how we arrived at using the AssociationListener (TransactionListener):
     *     https://developer.jboss.org/message/919807
     *     https://developer.jboss.org/thread/252572
     */
    private static class SessionSynchronization implements Synchronization, AssociationListener {
        private AutoCloseable scopedObject;  // the underlying entity manager or stateless session
        private String scopedPuName;
        private boolean afterCompletionCalled = false;
        private int associationCounter = 1; // set to one since transaction is associated with current thread already.
                                            // incremented when a thread is associated with transaction,
                                            // decremented when a thread is disassociated from transaction.
                                            // synchronization on this object protects associationCounter.

        public SessionSynchronization(AutoCloseable session, String scopedPuName) {
            this.scopedObject = session;
            this.scopedPuName = scopedPuName;
        }

        public void beforeCompletion() {
            afterCompletionCalled = false;
        }

        public void afterCompletion(int status) {
            /**
             * Note: synchronization is to protect against two concurrent threads from closing the EntityManager (manager)
             * at the same time.
             */
            synchronized (this) {
                afterCompletionCalled = true;
                safeCloseScopedObject();
            }
        }

        /**
         * After the Jakarta Transactions transaction is ended (Synchronization.afterCompletion has been called) and
         * the Jakarta Transactions transaction is no longer associated with application thread (application thread called
         * transaction.rollback/commit/suspend), the entity manager can safely be closed.
         *
         * NOTE: caller must call with synchronized(this), where this == instance of SessionSynchronization associated with
         * the Jakarta Transactions transaction.
         */
        private void safeCloseScopedObject() {
            if (afterCompletionCalled
                    && associationCounter == 0
                    && scopedObject != null) {
                try {
                    if (ROOT_LOGGER.isDebugEnabled())
                        ROOT_LOGGER.debugf("%s: closing entity manager/session", getTransactionScopedObjectDetails(scopedObject, scopedPuName));
                    scopedObject.close();
                } catch (Exception ignored) {
                    if (ROOT_LOGGER.isDebugEnabled())
                        ROOT_LOGGER.debugf(ignored, "ignoring error that occurred while closing EntityManager for %s (",
                                scopedPuName);
                }
                scopedObject = null;
            }
        }

        public void associationChanged(final AbstractTransaction transaction, final boolean associated) {
            synchronized (this) {
                // associationCounter is set to zero when application thread is no longer associated with Jakarta Transactions transaction.
                // We are tracking when the application thread
                // is no longer associated with the transaction, as that indicates that it is safe to
                // close the entity manager (since the application is no longer using the entity manager).
                //
                // Expected values for associationCounter:
                // 1 - application thread is associated with transaction
                // 0 - application thread is not associated with transaction (e.g. tm.suspend called)
                //
                // Expected values for TM Reaper thread timing out transaction
                // 1 - application thread is associated with transaction
                // 2 - TM reaper thread is associated with transaction (tx timeout handling)
                // 1 - either TM reaper or application thread disassociated from transaction
                // 0 - both TM reaper and application thread are disassociated from transaction
                //
                // the safeCloseEntityManager() may close the entity manager in the (background) reaper thread or
                // application thread (whichever thread reaches associationCounter == 0).
                associationCounter += associated ? 1 : -1;
                if (ROOT_LOGGER.isTraceEnabled()) {
                    ROOT_LOGGER.tracef("transaction association counter = %d for %s: ", associationCounter, getTransactionScopedObjectDetails(scopedObject, scopedPuName));
                }
                safeCloseScopedObject();
            }
        }
    }

}
