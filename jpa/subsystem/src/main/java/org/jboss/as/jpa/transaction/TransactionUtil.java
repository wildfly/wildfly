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

import static org.jboss.as.jpa.messages.JpaLogger.JPA_LOGGER;

import javax.persistence.EntityManager;
import javax.transaction.Status;
import javax.transaction.Synchronization;
import javax.transaction.SystemException;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;
import javax.transaction.TransactionSynchronizationRegistry;

import org.jboss.as.jpa.container.ExtendedEntityManager;
import org.jboss.as.jpa.messages.JpaLogger;
import org.jboss.tm.TxUtils;

/**
 * Transaction utilities for JPA
 *
 * @author Scott Marlow (forked from code by Gavin King)
 */
public class TransactionUtil {

    private static volatile TransactionSynchronizationRegistry transactionSynchronizationRegistry;
    private static volatile TransactionManager transactionManager;

    public static void setTransactionManager(TransactionManager tm) {
        if (transactionManager == null) {
            transactionManager = tm;
        }
    }

    public static TransactionManager getTransactionManager() {
        return transactionManager;
    }

    public static void setTransactionSynchronizationRegistry(TransactionSynchronizationRegistry transactionSynchronizationRegistry) {
        if (TransactionUtil.transactionSynchronizationRegistry == null) {
            TransactionUtil.transactionSynchronizationRegistry = transactionSynchronizationRegistry;
        }
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
        getTransactionSynchronizationRegistry().registerInterposedSynchronization(new SessionSynchronization(entityManager, puScopedName));
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

    private static class SessionSynchronization implements Synchronization {
        private EntityManager manager;  // the underlying entity manager
        private String scopedPuName;

        public SessionSynchronization(EntityManager session, String scopedPuName) {
            this.manager = session;
            this.scopedPuName = scopedPuName;
        }

        public void beforeCompletion() {
        }

        public void afterCompletion(int status) {
            /**
             * If its not safe (safeToClose returns false) to close the EntityManager now,
             * any connections joined to the JTA transaction
             * will be released by the JCA connection pool manager.  When the JTA Transaction is no longer
             * referencing the EntityManager, it will be eligible for garbage collection.
             * See AS7-6586 for more details.
             */
            if (safeToClose(status)) {
                try {
                    if (JPA_LOGGER.isDebugEnabled())
                        JPA_LOGGER.debugf("%s: closing entity managersession", getEntityManagerDetails(manager, scopedPuName));
                    manager.close();
                } catch (Exception ignored) {
                    if (JPA_LOGGER.isDebugEnabled())
                        JPA_LOGGER.debugf(ignored, "ignoring error that occurred while closing EntityManager for %s (", scopedPuName);
                }
            }
            // The TX reference to the entity manager, should be cleared by the TM

        }

        /**
         * AS7-6586 requires that the container avoid closing the EntityManager while the application
         * may be using the EntityManager in a different thread.  If the transaction has been rolled
         * back, will check if the current thread is the Arjuna transaction manager Reaper thread.  It is not
         * safe to call EntityManager.close from the Reaper thread, so false is returned.
         *
         * TODO: switch to depend on JBTM-1556 instead of checking the current thread name.
         *
         * @param status of transaction.
         * @return
         */
        private boolean safeToClose(int status) {
            if (Status.STATUS_COMMITTED != status) {
                return !TxUtils.isTransactionManagerTimeoutThread();
            }

            return true;
        }
    }


}
