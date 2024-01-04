/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.iiop.transaction.timeout;

import java.rmi.RemoteException;

import jakarta.annotation.Resource;
import jakarta.ejb.EJBException;
import jakarta.ejb.Remote;
import jakarta.ejb.RemoteHome;
import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import jakarta.transaction.SystemException;
import jakarta.transaction.Transaction;
import jakarta.transaction.TransactionManager;
import org.jboss.as.test.integration.transactions.TransactionCheckerSingleton;
import org.jboss.as.test.integration.transactions.TxTestUtil;
import org.jboss.ejb3.annotation.TransactionTimeout;
import org.jboss.logging.Logger;

@Stateless
@Remote(TestBeanRemote.class)
@RemoteHome(TestBeanHome.class)
public class StatelessBean {
    private static final Logger log = Logger.getLogger(StatelessBean.class);

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

    public void testTransaction() throws RemoteException, SystemException {
        log.trace("Method stateless #testTransaction called");
        Transaction txn;
        txn = tm.getTransaction();

        TxTestUtil.addSynchronization(txn, checker);

        TxTestUtil.enlistTestXAResource(txn, checker);
        TxTestUtil.enlistTestXAResource(txn, checker);
    }

    @TransactionTimeout(value = 1)
    public void testTimeout() throws SystemException, RemoteException {
        log.trace("Method stateless #testTimeout called");
        Transaction txn;
        txn = tm.getTransaction();

        TxTestUtil.addSynchronization(txn, checker);

        TxTestUtil.enlistTestXAResource(txn, checker);

        try {
            TxTestUtil.waitForTimeout(tm);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            throw new RemoteException("Interupted during waiting for transaction timeout", ie);
        }

        TxTestUtil.enlistTestXAResource(txn, checker);
    }

    public void touch() {
        log.trace("Stateless bean instance has been touched");
    }
}
