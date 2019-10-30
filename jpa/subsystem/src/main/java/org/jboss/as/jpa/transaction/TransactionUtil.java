/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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

package org.jboss.as.jpa.transaction;

import static java.security.AccessController.doPrivileged;
import static org.jboss.as.jpa.messages.JpaLogger.ROOT_LOGGER;

import java.security.PrivilegedAction;
import java.util.EnumSet;

import javax.persistence.EntityManager;
import javax.transaction.Synchronization;
import javax.transaction.SystemException;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;
import javax.transaction.TransactionSynchronizationRegistry;

import org.jboss.as.jpa.container.ExtendedEntityManager;
import org.jboss.as.jpa.messages.JpaLogger;
import org.jboss.tm.TxUtils;
import org.jboss.tm.listener.EventType;
import org.wildfly.transaction.client.AbstractTransaction;
import org.wildfly.transaction.client.AssociationListener;
import org.wildfly.transaction.client.ContextTransactionManager;

/**
 * Transaction utilities for JPA
 *
 * @author Scott Marlow (forked from code by Gavin King)
 */
public class TransactionUtil {

    private static final EnumSet<EventType> eventTypes = EnumSet.of(EventType.ASSOCIATED, EventType.DISASSOCIATING);

    public static boolean isInTx(TransactionManager transactionManager) {
        Transaction tx = getTransaction(transactionManager);
        if (tx == null || !TxUtils.isActive(tx))
            return false;
        return true;
    }

    /**
     * Get current persistence context.  Only call while a transaction is active in the current thread.
     *
     * @param puScopedName
     * @return
     */
    public static EntityManager getTransactionScopedEntityManager(String puScopedName, TransactionSynchronizationRegistry tsr) {
        return getEntityManagerInTransactionRegistry(puScopedName, tsr);
    }

    public static void registerSynchronization(EntityManager entityManager, String puScopedName, TransactionSynchronizationRegistry tsr, TransactionManager transactionManager) {
        SessionSynchronization sessionSynchronization = new SessionSynchronization(entityManager, puScopedName);
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
            throw JpaLogger.ROOT_LOGGER.errorGettingTransaction(e);
        }
    }

    private static String currentThread() {
        return Thread.currentThread().getName();
    }

    public static String getEntityManagerDetails(EntityManager manager, String scopedPuName) {
        String result = currentThread() + ":";  // show the thread for correlation with other modules
        if (manager instanceof ExtendedEntityManager) {
            result += manager.toString();
        }
        else {
            result += "transaction scoped EntityManager [" + scopedPuName + "]";
        }
        return result;
    }


    private static EntityManager getEntityManagerInTransactionRegistry(String scopedPuName, TransactionSynchronizationRegistry tsr) {
        return (EntityManager)tsr.getResource(scopedPuName);
    }

    /**
     * Save the specified EntityManager in the local threads active transaction.  The TransactionSynchronizationRegistry
     * will clear the reference to the EntityManager when the transaction completes.
     *
     * @param scopedPuName
     * @param entityManager
     */
    public static void putEntityManagerInTransactionRegistry(String scopedPuName, EntityManager entityManager, TransactionSynchronizationRegistry tsr) {
        tsr.putResource(scopedPuName, entityManager);
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
     *   Note that entity managers do not get propagated on remote EJB invocations.
     *
     * See discussions for more details about how we arrived at using the AssociationListener (TransactionListener):
     *     https://developer.jboss.org/message/919807
     *     https://developer.jboss.org/thread/252572
     */
    private static class SessionSynchronization implements Synchronization, AssociationListener {
        private EntityManager manager;  // the underlying entity manager
        private String scopedPuName;
        private boolean afterCompletionCalled = false;
        private int associationCounter = 1; // set to one since transaction is associated with current thread already.
                                            // incremented when a thread is associated with transaction,
                                            // decremented when a thread is disassociated from transaction.
                                            // synchronization on this object protects associationCounter.

        public SessionSynchronization(EntityManager session, String scopedPuName) {
            this.manager = session;
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
                safeCloseEntityManager();
            }
        }

        /**
         * After the JTA transaction is ended (Synchronization.afterCompletion has been called) and
         * the JTA transaction is no longer associated with application thread (application thread called
         * transaction.rollback/commit/suspend), the entity manager can safely be closed.
         *
         * NOTE: caller must call with synchronized(this), where this == instance of SessionSynchronization associated with
         * the JTA transaction.
         */
        private void safeCloseEntityManager() {
            if ( afterCompletionCalled == true && associationCounter == 0) {
                if (manager != null) {
                    try {
                        if (ROOT_LOGGER.isDebugEnabled())
                            ROOT_LOGGER.debugf("%s: closing entity managersession", getEntityManagerDetails(manager, scopedPuName));
                        manager.close();
                    } catch (Exception ignored) {
                        if (ROOT_LOGGER.isDebugEnabled())
                            ROOT_LOGGER.debugf(ignored, "ignoring error that occurred while closing EntityManager for %s (", scopedPuName);
                    }
                    manager = null;
                }
            }
        }

        public void associationChanged(final AbstractTransaction transaction, final boolean associated) {
            synchronized (this) {
                // associationCounter is set to zero when application thread is no longer associated with JTA transaction.
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
                    ROOT_LOGGER.tracef("transaction association counter = %d for %s: ", associationCounter, getEntityManagerDetails(manager, scopedPuName));
                }
                safeCloseEntityManager();
            }
        }
    }

}
