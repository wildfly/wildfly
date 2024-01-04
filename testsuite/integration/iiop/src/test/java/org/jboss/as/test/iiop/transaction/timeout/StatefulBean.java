/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.iiop.transaction.timeout;

import java.rmi.RemoteException;

import jakarta.annotation.Resource;
import jakarta.ejb.CreateException;
import jakarta.ejb.EJBException;
import jakarta.ejb.Remote;
import jakarta.ejb.RemoteHome;
import jakarta.ejb.SessionSynchronization;
import jakarta.ejb.Stateful;
import jakarta.inject.Inject;
import jakarta.transaction.SystemException;
import jakarta.transaction.Transaction;
import jakarta.transaction.TransactionManager;
import org.jboss.as.test.integration.transactions.TransactionCheckerSingleton;
import org.jboss.as.test.integration.transactions.TxTestUtil;
import org.jboss.ejb3.annotation.TransactionTimeout;
import org.jboss.logging.Logger;

@Stateful
@Remote(TestBeanRemote.class)
@RemoteHome(TestBeanHome.class)
public class StatefulBean implements SessionSynchronization {
    private static final Logger log = Logger.getLogger(StatefulBean.class);

    @Resource(name = "java:jboss/TransactionManager")
    private TransactionManager tm;

    @Inject
    private TransactionCheckerSingleton checker;

    public void afterBegin() throws EJBException, RemoteException {
        log.trace("afterBegin called");
        checker.setSynchronizedBegin();
    }

    public void beforeCompletion() throws EJBException, RemoteException {
        log.trace("beforeCompletion called");
        checker.setSynchronizedBefore();
    }

    public void afterCompletion(boolean committed) throws EJBException, RemoteException {
        log.tracef("afterCompletion: transaction was%s committed", committed ? "" : " not");
        checker.setSynchronizedAfter(committed);
    }

    public void ejbCreate() throws RemoteException, CreateException {
        log.debugf("Creating method for home interface '%s' was invoked", this.getClass());
    }

    public void testTransaction() throws RemoteException, SystemException {
        log.trace("Method stateful #testTransaction called");
        Transaction txn;
        txn = tm.getTransaction();

        TxTestUtil.enlistTestXAResource(txn, checker);
        TxTestUtil.enlistTestXAResource(txn, checker);
    }

    @TransactionTimeout(value = 1)
    public void testTimeout() throws SystemException, RemoteException {
        log.trace("Method stateful #testTimeout called");
        Transaction txn = tm.getTransaction();

        TxTestUtil.enlistTestXAResource(txn, checker);
        TxTestUtil.enlistTestXAResource(txn, checker);

        try {
            TxTestUtil.waitForTimeout(tm);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            throw new RemoteException("Interupted during waiting for transaction timeout", ie);
        }
    }

    public void touch() {
        log.trace("Stateful bean instance has been touched");
    }
}
