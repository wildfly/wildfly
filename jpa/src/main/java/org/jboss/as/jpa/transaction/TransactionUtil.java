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

import org.jboss.as.jpa.container.EntityManagerMetadata;
import org.jboss.as.jpa.container.EntityManagerUtil;
import org.jboss.logging.Logger;
import org.jboss.tm.TxUtils;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceException;
import javax.persistence.TransactionRequiredException;
import javax.transaction.RollbackException;
import javax.transaction.Status;
import javax.transaction.Synchronization;
import javax.transaction.SystemException;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;
import javax.transaction.TransactionSynchronizationRegistry;
import javax.transaction.UserTransaction;
import javax.transaction.xa.XAException;
import javax.transaction.xa.XAResource;
import java.util.Map;

/**
 * Transaction utilities for JPA
 *
 * @author Scott Marlow (forked from code by Gavin King)
 */
public class TransactionUtil {

    private static final TransactionUtil INSTANCE = new TransactionUtil();
    private static final Logger log = Logger.getLogger("org.jboss.jpa");

    private static volatile TransactionSynchronizationRegistry transactionSynchronizationRegistry;
    private static volatile TransactionManager transactionManager;

    public static TransactionUtil getInstance() {
        return INSTANCE;
    }

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

    public Transaction getTransaction() {
        try {
            return transactionManager.getTransaction();
        } catch (SystemException e) {
            throw new IllegalStateException("An error occured while getting the " +
                "transaction associated with the current thread: " + e);
        }
    }

    public static TransactionSynchronizationRegistry getTransactionSynchronizationRegistry() {
        return transactionSynchronizationRegistry;
    }

    public boolean isInTx() {
        Transaction tx = getTransaction();
        if (tx == null || !TxUtils.isActive(tx))
            return false;
        return true;
    }

    private static String currentThread() {
        return Thread.currentThread().getName();
    }

    private static String getEntityManagerDetails(EntityManager manager) {
        String result = currentThread() + ":";  // show the thread for correlation with other modules
        EntityManagerMetadata metadata;
        try {
            if ((metadata = manager.unwrap(EntityManagerMetadata.class)) != null) {
                result += metadata.getPuName() +
                    ((metadata.isTransactionScopedEntityManager()) ? " [XPC]" : " [transactional]"
                    );
            }
        } catch (PersistenceException ignoreUnhandled) {  // TODO:  switch to a different way to lookup EntityManagerMetadata
                                                          // so that we don't get this error on TransactionalEntityManager
                                                          // (because the EntityManager is the underlying provider that
                                                          // doesn't have the metadata.)
        }


        return result;
    }

    public void registerExtendedUnderlyingWithTransaction(String scopedPuName, EntityManager xpc, EntityManager underlyingEntityManager) {
        // xpc invoked this method, we cannot call xpc because it will recurse back to here, join with underloying em instead
        underlyingEntityManager.joinTransaction();
        setPC(scopedPuName, xpc);
    }

    public void registerExtendedWithTransaction(String scopedPuName, EntityManager pc) {
        pc.joinTransaction();
        setPC(scopedPuName, pc);
    }

    private EntityManager getPC(String scopedPuName) {
        return (EntityManager) getTransactionSynchronizationRegistry().getResource(scopedPuName);
    }

    /**
     * Save the specified EntityManager in the local threads active transaction.  The TransactionSynchronizationRegistry
     * will clear the reference to the EntityManager when the transaction completes.
     *
     * @param scopedPuName
     * @param entityManager
     */
    private void setPC(String scopedPuName, EntityManager entityManager) {
        getTransactionSynchronizationRegistry().putResource(scopedPuName, entityManager);
    }

    /**
     * Get current PC.  Only call while a transaction is active in the current thread.
     *
     * @param puScopedName
     * @return
     */
    public EntityManager getTransactionScopedEntityManager(String puScopedName) {
        return getPC(puScopedName);
    }

    /**
     * Get current PC or create a Transactional entity manager.
     * Only call while a transaction is active in the current thread.
     *
     * @param emf
     * @param puScopedName
     * @param properties
     * @return
     */
    public EntityManager getOrCreateTransactionScopedEntityManager(EntityManagerFactory emf, String puScopedName, Map properties) {

        EntityManager rtnSession = getPC(puScopedName);
        if (rtnSession == null) {
            rtnSession = EntityManagerUtil.createEntityManager(emf, properties);
            Transaction tx = getTransaction();
            if (log.isDebugEnabled())
                log.debug(getEntityManagerDetails(rtnSession) + ": created entity managersession " +
                    tx.toString());
            try {
                tx.registerSynchronization(new SessionSynchronization(rtnSession, tx, true, puScopedName));
            } catch (RollbackException e) {
                throw new RuntimeException(e);  //To change body of catch statement use Options | File Templates.
            } catch (SystemException e) {
                throw new RuntimeException(e);  //To change body of catch statement use Options | File Templates.
            }
            setPC(puScopedName, rtnSession);
            rtnSession.joinTransaction(); // force registration with TX
        } else {
            if (log.isDebugEnabled()) {
                Transaction tx = getTransaction();
                log.debug(getEntityManagerDetails(rtnSession) + ": reuse entity managersession already in tx" +
                    tx.toString());
            }
        }
        return rtnSession;
    }

    private static class SessionSynchronization implements Synchronization {
        private EntityManager manager;
        private boolean closeAtTxCompletion;
        private String scopedPuName;

        public SessionSynchronization(EntityManager session, Transaction tx, boolean close, String scopedPuName) {
            this.manager = session;
            closeAtTxCompletion = close;
            this.scopedPuName = scopedPuName;
        }

        public void beforeCompletion() {
        }

        public void afterCompletion(int status) {
            if (closeAtTxCompletion) {
                if (log.isDebugEnabled())
                    log.debug(getEntityManagerDetails(manager) + ": closing entity managersession ");
                manager.close();
                getInstance().setPC(scopedPuName, null);
            }
        }
    }


}
