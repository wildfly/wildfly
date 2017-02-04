/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2017, Red Hat, Inc., and individual contributors
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
