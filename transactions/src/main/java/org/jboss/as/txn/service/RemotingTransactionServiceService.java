/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.txn.service;

import java.util.function.Consumer;
import java.util.function.Supplier;

import org.jboss.logging.Logger;
import org.jboss.msc.Service;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.remoting3.Endpoint;
import org.jboss.remoting3.Registration;
import org.jboss.remoting3.ServiceRegistrationException;
import org.wildfly.transaction.client.LocalTransactionContext;
import org.wildfly.transaction.client.provider.remoting.RemotingTransactionService;

/**
 * The service providing the Remoting transaction service.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
public final class RemotingTransactionServiceService implements Service {

    protected static final Logger log = Logger.getLogger(RemotingTransactionServiceService.class.getSimpleName());

    private final Consumer<RemotingTransactionService> remotingTxnServiceConsumer;
    private final Supplier<LocalTransactionContext> localTransactionContextSupplier;
    private final Supplier<Endpoint> endpointSupplier;
    private volatile Registration registration;

    public RemotingTransactionServiceService(final Consumer<RemotingTransactionService> remotingTxnServiceConsumer,
                                             final Supplier<LocalTransactionContext> localTransactionContextSupplier,
                                             final Supplier<Endpoint> endpointSupplier) {
        this.remotingTxnServiceConsumer = remotingTxnServiceConsumer;
        this.localTransactionContextSupplier = localTransactionContextSupplier;
        this.endpointSupplier = endpointSupplier;
    }

    public void start(final StartContext context) throws StartException {
        log.info("Starting");
        final RemotingTransactionService remotingTransactionService = RemotingTransactionService.builder().setEndpoint(endpointSupplier.get()).setTransactionContext(localTransactionContextSupplier.get()).build();
        try {
            registration = remotingTransactionService.register();
        } catch (ServiceRegistrationException e) {
            throw new StartException(e);
        }
        remotingTxnServiceConsumer.accept(remotingTransactionService);
        log.info("Started");
    }

    public void stop(final StopContext context) {
        log.info("Stopping");
        remotingTxnServiceConsumer.accept(null);
        registration.close();
        log.info("Stopped");
    }
}
