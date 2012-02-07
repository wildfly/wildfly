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

import static org.jboss.as.jpa.JpaLogger.JPA_LOGGER;
import static org.jboss.as.jpa.JpaMessages.MESSAGES;

import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.transaction.Synchronization;
import javax.transaction.SystemException;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;
import javax.transaction.TransactionSynchronizationRegistry;

import org.jboss.as.jpa.container.EntityManagerUtil;
import org.jboss.as.jpa.container.ExtendedEntityManager;
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
     * Register the specified entity manager (persistence context) with the current transaction.
     * Precondition:  Only call while a transaction is active in the current thread.
     *
     * @param scopedPuName            is the fully (application deployment) scoped name of persistence unit
     * @param xpc                     is the entity manager (a org.jboss.as.jpa class) to register
     * @param underlyingEntityManager is the underlying entity manager obtained from the persistence provider
     */
    public static void registerExtendedUnderlyingWithTransaction(String scopedPuName, EntityManager xpc, EntityManager underlyingEntityManager) {
        // xpc invoked this method, we cannot call xpc because it will recurse back to here, join with underloying em instead
        registerSynchronization(xpc, scopedPuName, false);
        underlyingEntityManager.joinTransaction();
        putEntityManagerInTransactionRegistry(scopedPuName, xpc);
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

    /**
     * Get current PC or create a Transactional entity manager.
     * Only call while a transaction is active in the current thread.
     *
     * @param emf
     * @param scopedPuName
     * @param properties
     * @return
     */
    public static EntityManager getOrCreateTransactionScopedEntityManager(EntityManagerFactory emf, String scopedPuName, Map properties) {
        EntityManager entityManager = getEntityManagerInTransactionRegistry(scopedPuName);
        if (entityManager == null) {
            entityManager = EntityManagerUtil.createEntityManager(emf, properties);
            if (JPA_LOGGER.isDebugEnabled())
                JPA_LOGGER.debugf("%s: created entity manager session %s", getEntityManagerDetails(entityManager),
                    getTransaction().toString());
            boolean autoCloseEntityManager = true;
            registerSynchronization(entityManager, scopedPuName, autoCloseEntityManager);
            putEntityManagerInTransactionRegistry(scopedPuName, entityManager);
        } else {
            if (JPA_LOGGER.isDebugEnabled()) {
                JPA_LOGGER.debugf("%s: reuse entity manager session already in tx %s", getEntityManagerDetails(entityManager),
                    getTransaction().toString());
            }
        }
        return entityManager;
    }

    private static void registerSynchronization(EntityManager entityManager, String puScopedName, boolean closeEMAtTxEnd) {
        getTransactionSynchronizationRegistry().registerInterposedSynchronization(new SessionSynchronization(entityManager, closeEMAtTxEnd, puScopedName));
    }

    private static Transaction getTransaction() {
        try {
            return transactionManager.getTransaction();
        } catch (SystemException e) {
            throw MESSAGES.errorGettingTransaction(e);
        }
    }

    public static TransactionSynchronizationRegistry getTransactionSynchronizationRegistry() {
        return transactionSynchronizationRegistry;
    }


    private static String currentThread() {
        return Thread.currentThread().getName();
    }

    private static String getEntityManagerDetails(EntityManager manager) {
        String result = currentThread() + ":";  // show the thread for correlation with other modules
        if (manager instanceof ExtendedEntityManager) {
            result += manager.toString();
        }
        else {
            result += "[transaction scoped EntityManager]";
        }
        return result;
    }

    private static EntityManager getEntityManagerInTransactionRegistry(String scopedPuName) {
        return (EntityManager) getTransactionSynchronizationRegistry().getResource(scopedPuName);
    }

    /**
     * Save the specified EntityManager in the local threads active transaction.  The TransactionSynchronizationRegistry
     * will clear the reference to the EntityManager when the transaction completes.
     *
     * @param scopedPuName
     * @param entityManager
     */
    private static void putEntityManagerInTransactionRegistry(String scopedPuName, EntityManager entityManager) {
        getTransactionSynchronizationRegistry().putResource(scopedPuName, entityManager);
    }


    private static class SessionSynchronization implements Synchronization {
        private EntityManager manager;
        private boolean closeAtTxCompletion;
        private String scopedPuName;

        public SessionSynchronization(EntityManager session, boolean close, String scopedPuName) {
            this.manager = session;
            closeAtTxCompletion = close;
            this.scopedPuName = scopedPuName;
        }

        public void beforeCompletion() {
        }

        public void afterCompletion(int status) {
            if (closeAtTxCompletion) {
                if (JPA_LOGGER.isDebugEnabled())
                    JPA_LOGGER.debugf("%s: closing entity managersession", getEntityManagerDetails(manager));
                manager.close();
            }
            // The TX reference to the entity manager, should be cleared by the TM

        }
    }


}
