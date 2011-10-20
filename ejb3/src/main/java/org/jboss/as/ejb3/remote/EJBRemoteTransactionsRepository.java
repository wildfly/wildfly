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

package org.jboss.as.ejb3.remote;

import org.jboss.ejb.client.TransactionID;
import org.jboss.msc.inject.Injector;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;

import javax.transaction.Transaction;
import javax.transaction.TransactionManager;
import javax.transaction.UserTransaction;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * User: jpai
 */
public class EJBRemoteTransactionsRepository implements Service<EJBRemoteTransactionsRepository> {

    public static final ServiceName SERVICE_NAME = ServiceName.JBOSS.append("ejb").append("remote-transactions-repository");

    private final InjectedValue<TransactionManager> transactionManagerInjectedValue = new InjectedValue<TransactionManager>();

    private final InjectedValue<UserTransaction> userTransactionInjectedValue = new InjectedValue<UserTransaction>();

    private final Map<TransactionID, Transaction> transactions = Collections.synchronizedMap(new HashMap<TransactionID, Transaction>());

    @Override
    public void start(StartContext context) throws StartException {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void stop(StopContext context) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public EJBRemoteTransactionsRepository getValue() throws IllegalStateException, IllegalArgumentException {
        return this;
    }

    public TransactionManager getTransactionManager() {
        return this.transactionManagerInjectedValue.getValue();
    }

    public Transaction getTransaction(final TransactionID transactionID) {
        return this.transactions.get(transactionID);
    }

    public Transaction removeTransaction(final TransactionID transactionID) {
        return this.transactions.remove(transactionID);
    }

    public void addTransaction(final TransactionID transactionID, final Transaction transaction) {
        this.transactions.put(transactionID, transaction);
    }

    public UserTransaction getUserTransaction() {
        return this.userTransactionInjectedValue.getValue();
    }

    public Injector<TransactionManager> getTransactionManagerInjector() {
        return this.transactionManagerInjectedValue;
    }

    public Injector<UserTransaction> getUserTransactionInjector() {
        return this.userTransactionInjectedValue;
    }

}
