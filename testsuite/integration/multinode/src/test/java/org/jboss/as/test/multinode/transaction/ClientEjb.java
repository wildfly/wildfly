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
package org.jboss.as.test.multinode.transaction;

import org.junit.Assert;

import javax.annotation.Resource;
import javax.ejb.Stateless;
import javax.ejb.TransactionManagement;
import javax.ejb.TransactionManagementType;
import javax.naming.Context;
import javax.naming.NamingException;
import javax.transaction.HeuristicMixedException;
import javax.transaction.HeuristicRollbackException;
import javax.transaction.NotSupportedException;
import javax.transaction.RollbackException;
import javax.transaction.Status;
import javax.transaction.SystemException;
import javax.transaction.UserTransaction;
import java.rmi.RemoteException;
import java.util.Hashtable;

/**
 * @author Stuart Douglas
 * @author Ivo Studensky
 */
@Stateless
@TransactionManagement(TransactionManagementType.BEAN)
public class ClientEjb {

    @Resource
    private UserTransaction userTransaction;


    private TransactionalRemote getRemote() throws NamingException {
        final Hashtable props = new Hashtable();
        props.put(Context.URL_PKG_PREFIXES, "org.jboss.ejb.client.naming");
        final Context context = new javax.naming.InitialContext(props);
        final TransactionalRemote remote = (TransactionalRemote) context.lookup("ejb:/" + TransactionInvocationTestCase.SERVER_DEPLOYMENT + "/" + "" + "/" + "TransactionalStatelessBean" + "!" + TransactionalRemote.class.getName());
        return remote;
    }

    private TransactionalStatefulRemote getStatefulRemote() throws NamingException {
        final Hashtable props = new Hashtable();
        props.put(Context.URL_PKG_PREFIXES, "org.jboss.ejb.client.naming");
        final Context context = new javax.naming.InitialContext(props);
        final TransactionalStatefulRemote statefulRemote = (TransactionalStatefulRemote) context.lookup("ejb:/" + TransactionInvocationTestCase.SERVER_DEPLOYMENT + "/" + "" + "/" + "TransactionalStatefulBean" + "!" + TransactionalStatefulRemote.class.getName() + "?stateful");
        return statefulRemote;
    }

    public void basicTransactionPropagationTest() throws RemoteException, SystemException, NotSupportedException, NamingException {
        final TransactionalRemote remote = getRemote();
        Assert.assertEquals("No transaction expected!", Status.STATUS_NO_TRANSACTION, remote.transactionStatus());
        userTransaction.begin();
        try {
            Assert.assertEquals("Active transaction expected!", Status.STATUS_ACTIVE, remote.transactionStatus());
        } finally {
            userTransaction.rollback();
        }

    }

    public void testSameTransactionEachCall() throws RemoteException, SystemException, NotSupportedException, NamingException {
        final TransactionalStatefulRemote statefulRemote = getStatefulRemote();
        userTransaction.begin();
        try {
            statefulRemote.sameTransaction(true);
            statefulRemote.sameTransaction(false);
        } finally {
            userTransaction.rollback();
        }
    }

    public void testSynchronization(final boolean succeeded) throws RemoteException, SystemException, NotSupportedException, RollbackException, HeuristicRollbackException, HeuristicMixedException, NamingException {
        final TransactionalStatefulRemote statefulRemote = getStatefulRemote();
        userTransaction.begin();
        try {
            statefulRemote.sameTransaction(true);
            statefulRemote.sameTransaction(false);
        } finally {
            if (succeeded) {
                userTransaction.commit();
            } else {
                userTransaction.rollback();
            }
        }
        Assert.assertEquals("The beforeCompletion method invalid invocation!", succeeded, statefulRemote.isBeforeCompletion());
        Assert.assertEquals("The result of the transaction does not match the input of afterCompletion!", (Boolean) succeeded, statefulRemote.getCommitSucceeded());
    }

    public void testRollbackOnly() throws RemoteException, SystemException, NotSupportedException, NamingException {
        final TransactionalStatefulRemote statefulRemote = getStatefulRemote();
        userTransaction.begin();
        try {
            Assert.assertEquals("Active transaction expected!", Status.STATUS_ACTIVE, statefulRemote.transactionStatus());
            statefulRemote.rollbackOnly();
            Assert.assertEquals("Rollback-only marked transaction expected!", Status.STATUS_MARKED_ROLLBACK, statefulRemote.transactionStatus());
        } finally {
            userTransaction.rollback();
        }
        Assert.assertFalse("The beforeCompletion method invoked even in case of rollback-only transaction!", statefulRemote.isBeforeCompletion());
        Assert.assertFalse("The result of the transaction does not match the input of afterCompletion!", statefulRemote.getCommitSucceeded());
    }

    public void testRollbackOnlyBeforeCompletion() throws RemoteException, SystemException, NotSupportedException, HeuristicRollbackException, HeuristicMixedException, NamingException {
        final TransactionalStatefulRemote statefulRemote = getStatefulRemote();
        userTransaction.begin();
        try {
            Assert.assertEquals("Active transaction expected!", Status.STATUS_ACTIVE, statefulRemote.transactionStatus());
            statefulRemote.setRollbackOnlyBeforeCompletion(true);
            userTransaction.commit();
        } catch (RollbackException expected) {
        } finally {
            if (userTransaction.getStatus() == Status.STATUS_ACTIVE) {
                userTransaction.rollback();
            }
        }
        Assert.assertFalse("The result of the transaction does not match the input of afterCompletion!", statefulRemote.getCommitSucceeded());
        Assert.assertEquals("No transaction expected!", Status.STATUS_NO_TRANSACTION, statefulRemote.transactionStatus());
    }

}
