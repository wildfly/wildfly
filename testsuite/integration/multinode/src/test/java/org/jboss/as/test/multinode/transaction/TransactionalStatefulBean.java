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

import javax.annotation.Resource;
import javax.ejb.EJBException;
import javax.ejb.Remote;
import javax.ejb.SessionContext;
import javax.ejb.SessionSynchronization;
import javax.ejb.Stateful;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.transaction.TransactionSynchronizationRegistry;
import java.rmi.RemoteException;

/**
 * @author Stuart Douglas
 * @author Ivo Studensky
 */
@Remote(TransactionalStatefulRemote.class)
@Stateful
public class TransactionalStatefulBean implements SessionSynchronization, TransactionalStatefulRemote {

    private Boolean commitSucceeded;
    private boolean beforeCompletion = false;
    private Object transactionKey = null;
    private boolean rollbackOnlyBeforeCompletion = false;

    @Resource
    private SessionContext sessionContext;

    @Resource
    private TransactionSynchronizationRegistry transactionSynchronizationRegistry;


    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public int transactionStatus() {
        return transactionSynchronizationRegistry.getTransactionStatus();
    }

    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public void resetStatus() {
        commitSucceeded = null;
        beforeCompletion = false;
        transactionKey = null;
    }

    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public void setRollbackOnlyBeforeCompletion(boolean rollbackOnlyBeforeCompletion) throws RemoteException {
        this.rollbackOnlyBeforeCompletion = rollbackOnlyBeforeCompletion;
    }

    @TransactionAttribute(TransactionAttributeType.MANDATORY)
    public void sameTransaction(boolean first) throws RemoteException {
        if (first) {
            transactionKey = transactionSynchronizationRegistry.getTransactionKey();
        } else {
            if (!transactionKey.equals(transactionSynchronizationRegistry.getTransactionKey())) {
                throw new RemoteException("Transaction on second call was not the same as on first call");
            }
        }
    }

    @TransactionAttribute(TransactionAttributeType.MANDATORY)
    public void rollbackOnly() throws RemoteException {
        this.sessionContext.setRollbackOnly();
    }

    public void ejbCreate() {

    }

    public void afterBegin() throws EJBException, RemoteException {

    }

    public void beforeCompletion() throws EJBException, RemoteException {
        beforeCompletion = true;

        if (rollbackOnlyBeforeCompletion) {
            this.sessionContext.setRollbackOnly();
        }
    }

    public void afterCompletion(final boolean committed) throws EJBException, RemoteException {
        commitSucceeded = committed;
    }

    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public Boolean getCommitSucceeded() {
        return commitSucceeded;
    }

    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public boolean isBeforeCompletion() {
        return beforeCompletion;
    }
}
