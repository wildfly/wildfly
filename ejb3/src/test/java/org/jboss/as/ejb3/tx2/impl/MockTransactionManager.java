/*
 * JBoss, Home of Professional Open Source.
 * Copyright (c) 2011, Red Hat, Inc., and individual contributors
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
package org.jboss.as.ejb3.tx2.impl;

import javax.transaction.HeuristicMixedException;
import javax.transaction.HeuristicRollbackException;
import javax.transaction.InvalidTransactionException;
import javax.transaction.NotSupportedException;
import javax.transaction.RollbackException;
import javax.transaction.Status;
import javax.transaction.SystemException;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;

import static org.mockito.Mockito.mock;

/**
 * @author <a href="mailto:cdewolf@redhat.com">Carlo de Wolf</a>
 */
public class MockTransactionManager implements TransactionManager {
    private ThreadLocal<Transaction> currentTx = new ThreadLocal<Transaction>();

    @Override
    public void begin() throws NotSupportedException, SystemException {
        Transaction tx = currentTx.get();
        if (tx != null)
            throw new NotSupportedException("Nested tx are not supported");
        tx = mock(Transaction.class);
        currentTx.set(tx);
    }

    @Override
    public void commit() throws RollbackException, HeuristicMixedException, HeuristicRollbackException, SecurityException, IllegalStateException, SystemException {
        throw new RuntimeException("NYI: org.jboss.ejb3.tx2.impl.MockTransactionManager.commit");
    }

    @Override
    public void rollback() throws IllegalStateException, SecurityException, SystemException {
        throw new RuntimeException("NYI: org.jboss.ejb3.tx2.impl.MockTransactionManager.rollback");
    }

    @Override
    public void setRollbackOnly() throws IllegalStateException, SystemException {
        throw new RuntimeException("NYI: org.jboss.ejb3.tx2.impl.MockTransactionManager.setRollbackOnly");
    }

    @Override
    public int getStatus() throws SystemException {
        Transaction tx = currentTx.get();
        if (tx == null)
            return Status.STATUS_NO_TRANSACTION;
        return tx.getStatus();
    }

    @Override
    public Transaction getTransaction() throws SystemException {
        return currentTx.get();
    }

    @Override
    public void setTransactionTimeout(int seconds) throws SystemException {
        throw new RuntimeException("NYI: org.jboss.ejb3.tx2.impl.MockTransactionManager.setTransactionTimeout");
    }

    @Override
    public Transaction suspend() throws SystemException {
        Transaction tx = currentTx.get();
        currentTx.set(null);
        return tx;
    }

    @Override
    public void resume(Transaction tobj) throws InvalidTransactionException, IllegalStateException, SystemException {
        throw new RuntimeException("NYI: org.jboss.ejb3.tx2.impl.MockTransactionManager.resume");
    }
}
