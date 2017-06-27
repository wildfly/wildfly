/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2016, Red Hat, Inc., and individual contributors
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
 * 2110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.jboss.as.txn.service;

import com.arjuna.ats.jta.transaction.Transaction;
import io.undertow.server.handlers.PathHandler;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;
import org.wildfly.httpclient.transaction.HttpRemoteTransactionService;
import org.wildfly.transaction.client.LocalTransactionContext;

/**
 * @author Stuart Douglas
 */
public class TransactionRemoteHTTPService implements Service<TransactionRemoteHTTPService> {

    private final InjectedValue<PathHandler> pathHandlerInjectedValue = new InjectedValue<>();
    private final InjectedValue<LocalTransactionContext> localTransactionContextInjectedValue = new InjectedValue<>();

    @Override
    public void start(StartContext context) throws StartException {
        HttpRemoteTransactionService transactionService = new HttpRemoteTransactionService(localTransactionContextInjectedValue.getValue(), (transaction) -> transaction.getProviderInterface(Transaction.class).getTxId());
        pathHandlerInjectedValue.getValue().addPrefixPath("/txn", transactionService.createHandler());
    }

    @Override
    public void stop(StopContext context) {
        pathHandlerInjectedValue.getValue().removePrefixPath("/txn");
    }

    @Override
    public TransactionRemoteHTTPService getValue() throws IllegalStateException, IllegalArgumentException {
        return this;
    }

    public InjectedValue<PathHandler> getPathHandlerInjectedValue() {
        return pathHandlerInjectedValue;
    }

    public InjectedValue<LocalTransactionContext> getLocalTransactionContextInjectedValue() {
        return localTransactionContextInjectedValue;
    }
}
