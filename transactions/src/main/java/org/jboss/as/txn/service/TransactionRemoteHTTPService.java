/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
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
