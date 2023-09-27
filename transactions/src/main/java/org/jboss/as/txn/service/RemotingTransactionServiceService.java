/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.txn.service;

import org.jboss.msc.service.Service;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;
import org.jboss.remoting3.Endpoint;
import org.jboss.remoting3.Registration;
import org.jboss.remoting3.ServiceRegistrationException;
import org.wildfly.transaction.client.LocalTransactionContext;
import org.wildfly.transaction.client.provider.remoting.RemotingTransactionService;

/**
 * The service providing the Remoting transaction service.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public final class RemotingTransactionServiceService implements Service<RemotingTransactionService> {
    private final InjectedValue<LocalTransactionContext> localTransactionContextInjector = new InjectedValue<>();
    private final InjectedValue<Endpoint> endpointInjector = new InjectedValue<>();

    private volatile RemotingTransactionService value;
    private volatile Registration registration;

    public RemotingTransactionServiceService() {
    }

    public void start(final StartContext context) throws StartException {
        final RemotingTransactionService remotingTransactionService = RemotingTransactionService.builder().setEndpoint(endpointInjector.getValue()).setTransactionContext(localTransactionContextInjector.getValue()).build();
        try {
            registration = remotingTransactionService.register();
        } catch (ServiceRegistrationException e) {
            throw new StartException(e);
        }
        value = remotingTransactionService;
    }

    public void stop(final StopContext context) {
        value = null;
        registration.close();
    }

    public InjectedValue<LocalTransactionContext> getLocalTransactionContextInjector() {
        return localTransactionContextInjector;
    }

    public InjectedValue<Endpoint> getEndpointInjector() {
        return endpointInjector;
    }

    public RemotingTransactionService getValue() throws IllegalStateException, IllegalArgumentException {
        return value;
    }
}
