/*
 * JBoss, Home of Professional Open Source
 * Copyright 2016, Red Hat Inc., and individual contributors as indicated
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
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

package org.jboss.as.test.iiop.transaction.timeout;

import java.rmi.RemoteException;

import javax.annotation.Resource;
import javax.ejb.EJBException;
import javax.ejb.Remote;
import javax.ejb.RemoteHome;
import javax.ejb.SessionSynchronization;
import javax.ejb.Stateful;
import javax.inject.Inject;
import javax.transaction.SystemException;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;
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

    public void ejbCreate() throws java.rmi.RemoteException, javax.ejb.CreateException {
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
        Transaction txn;
        txn = tm.getTransaction();

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
