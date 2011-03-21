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

    /**
     * Transaction Status Strings
     */
    private static final String[] TxStatusStrings =
        {
            "STATUS_ACTIVE",
            "STATUS_MARKED_ROLLBACK",
            "STATUS_PREPARED",
            "STATUS_COMMITTED",
            "STATUS_ROLLEDBACK",
            "STATUS_UNKNOWN",
            "STATUS_NO_TRANSACTION",
            "STATUS_PREPARING",
            "STATUS_COMMITTING",
            "STATUS_ROLLING_BACK"
        };

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
            }
        }
    }

    private static String currentThread() {
        return Thread.currentThread().getName();
    }

    private static String getEntityManagerDetails(EntityManager manager) {
        String result = currentThread() + ":";  // show the thread for correlation with other modules
        EntityManagerMetadata metadata;
        if ((metadata = (EntityManagerMetadata) manager.unwrap(EntityManagerMetadata.class)) != null) {
            result += metadata.getPuName() +
                ((metadata.isTransactionScopedEntityManager()) ? " [XPC]" : " [transactional]"
                );
        }

        return result;
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

    public void verifyInTx() {
        Transaction tx = getTransaction();
        if (tx == null || !TxUtils.isActive(tx))
            throw new TransactionRequiredException("EntityManager must be access within a transaction");
        if (!TxUtils.isActive(tx))
            throw new TransactionRequiredException("Transaction must be active to access EntityManager");
    }

    /**
     * Get current PC.  Only call while a transaction is active in the current thread.
     * @param puScopedName
     * @return
     */
    public EntityManager getTransactionScopedEntityManager(String puScopedName) {
        return getPC(puScopedName);
    }

    /**
     * Get current PC or create a Transactional entity manager.
     * Only call while a transaction is active in the current thread.
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

    public static boolean isActive(Transaction tx) {
        if (tx == null)
            return false;

        try {
            int status = tx.getStatus();
            return isActive(status);
        } catch (SystemException error) {
            throw new RuntimeException(error);
        }
    }

    public static boolean isActive(TransactionManager tm) {
        try {
            return isActive(tm.getTransaction());
        } catch (SystemException error) {
            throw new RuntimeException(error);
        }
    }

    public static boolean isActive() {
        return isActive(getTransactionManager());
    }

    public static boolean isActive(UserTransaction ut) {
        try {
            int status = ut.getStatus();
            return isActive(status);
        } catch (SystemException error) {
            throw new RuntimeException(error);
        }
    }

    public static boolean isActive(int status) {
        return status == Status.STATUS_ACTIVE;
    }

    public static boolean isUncommitted(Transaction tx) {
        if (tx == null)
            return false;

        try {
            int status = tx.getStatus();
            return isUncommitted(status);
        } catch (SystemException error) {
            throw new RuntimeException(error);
        }
    }

    public static boolean isUncommitted(TransactionManager tm) {
        try {
            return isUncommitted(tm.getTransaction());
        } catch (SystemException error) {
            throw new RuntimeException(error);
        }
    }

    public static boolean isUncommitted() {
        return isUncommitted(getTransactionManager());
    }

    public static boolean isUncommitted(UserTransaction ut) {
        try {
            int status = ut.getStatus();
            return isUncommitted(status);

        } catch (SystemException error) {
            throw new RuntimeException(error);
        }
    }

    public static boolean isUncommitted(int status) {
        return status == Status.STATUS_ACTIVE
            || status == Status.STATUS_MARKED_ROLLBACK;
    }

    public static boolean isCompleted(Transaction tx) {
        if (tx == null)
            return true;

        try {
            int status = tx.getStatus();
            return isCompleted(status);
        } catch (SystemException error) {
            throw new RuntimeException(error);
        }
    }

    public static boolean isCompleted(TransactionManager tm) {
        try {
            return isCompleted(tm.getTransaction());
        } catch (SystemException error) {
            throw new RuntimeException(error);
        }
    }

    public static boolean isCompleted() {
        return isCompleted(getTransactionManager());
    }

    public static boolean isCompleted(UserTransaction ut) {
        try {
            int status = ut.getStatus();
            return isCompleted(status);

        } catch (SystemException error) {
            throw new RuntimeException(error);
        }
    }

    public static boolean isCompleted(int status) {
        return status == Status.STATUS_COMMITTED
            || status == Status.STATUS_ROLLEDBACK
            || status == Status.STATUS_NO_TRANSACTION;
    }

    public static boolean isRollback(Transaction tx) {
        if (tx == null)
            return false;

        try {
            int status = tx.getStatus();
            return isRollback(status);
        } catch (SystemException error) {
            throw new RuntimeException(error);
        }
    }

    public static boolean isRollback(TransactionManager tm) {
        try {
            return isRollback(tm.getTransaction());
        } catch (SystemException error) {
            throw new RuntimeException(error);
        }
    }

    public static boolean isRollback() {
        return isRollback();
    }

    public static boolean isRollback(UserTransaction ut) {
        try {
            int status = ut.getStatus();
            return isRollback(status);
        } catch (SystemException error) {
            throw new RuntimeException(error);
        }
    }

    public static boolean isRollback(int status) {
        return status == Status.STATUS_MARKED_ROLLBACK
            || status == Status.STATUS_ROLLING_BACK
            || status == Status.STATUS_ROLLEDBACK;
    }

    /**
     * Converts a tx Status index to a String
     *
     * @param status the Status index
     * @return status as String or "STATUS_INVALID(value)"
     * @see javax.transaction.Status
     */
    public static String getStatusAsString(int status) {
        if (status >= Status.STATUS_ACTIVE && status <= Status.STATUS_ROLLING_BACK) {
            return TxStatusStrings[status];
        } else {
            return "STATUS_INVALID(" + status + ")";
        }
    }

    /**
     * Converts a XAResource flag to a String
     *
     * @param flags the flags passed in to start(), end(), recover()
     * @return the flags in String form
     * @see javax.transaction.xa.XAResource
     */
    public static String getXAResourceFlagsAsString(int flags) {
        if (flags == XAResource.TMNOFLAGS) {
            return "|TMNOFLAGS";
        } else {
            StringBuffer sbuf = new StringBuffer(64);

            if ((flags & XAResource.TMONEPHASE) != 0) {
                sbuf.append("|TMONEPHASE");
            }
            if ((flags & XAResource.TMJOIN) != 0) {
                sbuf.append("|TMJOIN");
            }
            if ((flags & XAResource.TMRESUME) != 0) {
                sbuf.append("|TMRESUME");
            }
            if ((flags & XAResource.TMSUCCESS) != 0) {
                sbuf.append("|TMSUCCESS");
            }
            if ((flags & XAResource.TMFAIL) != 0) {
                sbuf.append("|TMFAIL");
            }
            if ((flags & XAResource.TMSUSPEND) != 0) {
                sbuf.append("|TMSUSPEND");
            }
            if ((flags & XAResource.TMSTARTRSCAN) != 0) {
                sbuf.append("|TMSTARTRSCAN");
            }
            if ((flags & XAResource.TMENDRSCAN) != 0) {
                sbuf.append("|TMENDRSCAN");
            }
            return sbuf.toString();
        }
    }

    /**
     * Converts a XAException error code to a string.
     *
     * @param errorCode an XAException error code
     * @return the error code in String form.
     * @see javax.transaction.xa.XAException
     */
    public static String getXAErrorCodeAsString(int errorCode) {
        switch (errorCode) {
            case XAException.XA_HEURCOM:
                return "XA_HEURCOM";
            case XAException.XA_HEURHAZ:
                return "XA_HEURHAZ";
            case XAException.XA_HEURMIX:
                return "XA_HEURMIX";
            case XAException.XA_HEURRB:
                return "XA_HEURRB";
            case XAException.XA_NOMIGRATE:
                return "XA_NOMIGRATE";
            case XAException.XA_RBCOMMFAIL:
                return "XA_RBCOMMFAIL";
            case XAException.XA_RBDEADLOCK:
                return "XA_RBDEADLOCK";
            case XAException.XA_RBINTEGRITY:
                return "XA_RBINTEGRITY";
            case XAException.XA_RBOTHER:
                return "XA_RBOTHER";
            case XAException.XA_RBPROTO:
                return "XA_RBPROTO";
            case XAException.XA_RBROLLBACK:
                return "XA_RBROLLBACK";
            case XAException.XA_RBTIMEOUT:
                return "XA_RBTIMEOUT";
            case XAException.XA_RBTRANSIENT:
                return "XA_RBTRANSIENT";
            case XAException.XA_RDONLY:
                return "XA_RDONLY";
            case XAException.XA_RETRY:
                return "XA_RETRY";
            case XAException.XAER_ASYNC:
                return "XAER_ASYNC";
            case XAException.XAER_DUPID:
                return "XAER_DUPID";
            case XAException.XAER_INVAL:
                return "XAER_INVAL";
            case XAException.XAER_NOTA:
                return "XAER_NOTA";
            case XAException.XAER_OUTSIDE:
                return "XAER_OUTSIDE";
            case XAException.XAER_PROTO:
                return "XAER_PROTO";
            case XAException.XAER_RMERR:
                return "XAER_RMERR";
            case XAException.XAER_RMFAIL:
                return "XAER_RMFAIL";
            default:
                return "XA_UNKNOWN(" + errorCode + ")";
        }
    }

}
