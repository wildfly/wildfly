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

import static org.jboss.as.jpa.messages.JpaLogger.ROOT_LOGGER;

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
import org.jboss.tm.listener.TransactionEvent;
import org.jboss.tm.listener.TransactionListener;
import org.jboss.tm.listener.TransactionListenerRegistry;
import org.jboss.tm.listener.TransactionListenerRegistryLocator;
import org.jboss.tm.listener.TransactionListenerRegistryUnavailableException;
import org.jboss.tm.listener.TransactionTypeNotSupported;

/**
 * Transaction utilities for JPA
 *
 * @author Scott Marlow (forked from code by Gavin King)
 */
public class TransactionUtil {

    private static volatile TransactionSynchronizationRegistry transactionSynchronizationRegistry;
    private static volatile TransactionManager transactionManager;
    private static volatile TransactionListenerRegistry transactionListenerRegistry;
    private static final EnumSet<EventType> eventTypes = EnumSet.of(EventType.ASSOCIATED, EventType.DISASSOCIATING);

    public static void setTransactionManager(TransactionManager tm) {
        if (transactionManager != tm) {
            transactionManager = tm;
        }
    }

    public static void clearTransactionManager() {
        transactionManager = null;
    }


    public static void setTransactionListenerRegistry() throws TransactionListenerRegistryUnavailableException {
        TransactionListenerRegistry tlr = TransactionListenerRegistryLocator.getTransactionListenerRegistry();
        if (transactionListenerRegistry != tlr) {
            transactionListenerRegistry = tlr;
        }

    }

    public static void clearTransactionListenerRegistry() {
        transactionListenerRegistry = null;
    }

    public static TransactionManager getTransactionManager() {
        return transactionManager;
    }

    public static void setTransactionSynchronizationRegistry(TransactionSynchronizationRegistry transactionSynchronizationRegistry) {
        if (TransactionUtil.transactionSynchronizationRegistry != transactionSynchronizationRegistry) {
            TransactionUtil.transactionSynchronizationRegistry = transactionSynchronizationRegistry;
        }
    }

    public static void clearTransactionSynchronizationRegistry() {
        transactionSynchronizationRegistry = null;
    }

    public static boolean isInTx() {
        Transaction tx = getTransaction();
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
    public static EntityManager getTransactionScopedEntityManager(String puScopedName) {
        return getEntityManagerInTransactionRegistry(puScopedName);
    }

    public static void registerSynchronization(EntityManager entityManager, String puScopedName) {
        SessionSynchronization sessionSynchronization = new SessionSynchronization(entityManager, puScopedName);
        getTransactionSynchronizationRegistry().registerInterposedSynchronization(sessionSynchronization);

        try {
            transactionListenerRegistry.addListener(getTransaction(), sessionSynchronization, eventTypes);
        } catch (TransactionTypeNotSupported transactionTypeNotSupported) {
            throw JpaLogger.ROOT_LOGGER.errorGettingTransaction(transactionTypeNotSupported);
        }
    }

    public static Transaction getTransaction() {
        try {
            return transactionManager.getTransaction();
        } catch (SystemException e) {
            throw JpaLogger.ROOT_LOGGER.errorGettingTransaction(e);
        }
    }

    public static TransactionSynchronizationRegistry getTransactionSynchronizationRegistry() {
        return transactionSynchronizationRegistry;
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


    private static EntityManager getEntityManagerInTransactionRegistry(String scopedPuName) {
        return (EntityManager)getTransactionSynchronizationRegistry().getResource(scopedPuName);
    }

    /**
     * Save the specified EntityManager in the local threads active transaction.  The TransactionSynchronizationRegistry
     * will clear the reference to the EntityManager when the transaction completes.
     *
     * @param scopedPuName
     * @param entityManager
     */
    public static void putEntityManagerInTransactionRegistry(String scopedPuName, EntityManager entityManager) {
        getTransactionSynchronizationRegistry().putResource(scopedPuName, entityManager);
    }

    private static class SessionSynchronization implements Synchronization, TransactionListener {
        private EntityManager manager;  // the underlying entity manager
        private String scopedPuName;
        private transient boolean transactionDisassociatedFromApplication = false;
        private transient boolean afterCompletionCalled = false;

        public SessionSynchronization(EntityManager session, String scopedPuName) {
            this.manager = session;
            this.scopedPuName = scopedPuName;
        }

        public void beforeCompletion() {
            afterCompletionCalled = false;
        }

        public synchronized void afterCompletion(int status) {
            /**
             * Note: synchronization is to protect against two concurrent threads from closing the EntityManager (manager)
             * at the same time.
             */
            afterCompletionCalled = true;
            safeCloseEntityManager();
        }

        /**
         * After the JTA transaction is ended (Synchronization.afterCompletion has been called) and
         * the JTA transaction is no longer associated with application thread (application thread called
         * transaction.rollback/commit/suspend), the entity manager can safely be closed.
         */
        private void safeCloseEntityManager() {
            if ( afterCompletionCalled == true && transactionDisassociatedFromApplication == true) {
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

        @Override
        public synchronized void onEvent(TransactionEvent transactionEvent) {
            if (transactionEvent.getTypes().contains(EventType.ASSOCIATED)) {
                // application thread is now associated with JTA transaction
                transactionDisassociatedFromApplication = false;
            }
            else if(transactionEvent.getTypes().contains(EventType.DISASSOCIATING)) {
                // application thread is no longer associated with JTA transaction
                transactionDisassociatedFromApplication = true;
            }
            safeCloseEntityManager();
        }
    }


}
