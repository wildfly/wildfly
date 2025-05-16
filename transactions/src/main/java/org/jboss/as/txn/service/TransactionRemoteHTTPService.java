/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.txn.service;

import java.util.function.Consumer;
import java.util.function.Supplier;

import com.arjuna.ats.jta.transaction.Transaction;
import io.undertow.server.handlers.PathHandler;
import org.jboss.msc.Service;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.wildfly.httpclient.transaction.HttpRemoteTransactionService;
import org.wildfly.transaction.client.LocalTransactionContext;

/**
 * @author Stuart Douglas
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
public class TransactionRemoteHTTPService implements Service {
    private final Consumer<TransactionRemoteHTTPService> httpServiceConsumer;
    private final Supplier<LocalTransactionContext> localTransactionContextSupplier;
    private final Supplier<PathHandler> pathHandlerSupplier;

    public TransactionRemoteHTTPService(final Consumer<TransactionRemoteHTTPService> httpServiceConsumer,
                                        final Supplier<LocalTransactionContext> localTransactionContextSupplier,
                                        final Supplier<PathHandler> pathHandlerSupplier) {
        this.httpServiceConsumer = httpServiceConsumer;
        this.localTransactionContextSupplier = localTransactionContextSupplier;
        this.pathHandlerSupplier = pathHandlerSupplier;
    }

    @Override
    public void start(final StartContext context) throws StartException {
        HttpRemoteTransactionService transactionService = new HttpRemoteTransactionService(localTransactionContextSupplier.get(), (transaction) -> transaction.getProviderInterface(Transaction.class).getTxId());
        pathHandlerSupplier.get().addPrefixPath("/txn", transactionService.createHandler());
        httpServiceConsumer.accept(this);
    }

    @Override
    public void stop(final StopContext context) {
        httpServiceConsumer.accept(null);
        pathHandlerSupplier.get().removePrefixPath("/txn");
    }
}
