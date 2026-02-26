/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.ejb3.remote.http;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import io.undertow.server.handlers.PathHandler;
import org.jboss.as.ejb3.remote.AssociationService;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.wildfly.httpclient.ejb.HttpRemoteEjbService;
import org.wildfly.transaction.client.LocalTransactionContext;

/**
 * Provides an HTTP-based connection service for EJB/HTTP remote clients.
 *
 * @author Stuart Douglas
 * @author <a href="mailto:rachmato@redhat.com">Richard Achmatowicz</a>
 */
public class EJBRemoteHTTPService implements Service<EJBRemoteHTTPService> {

    private final Consumer<EJBRemoteHTTPService> serviceConsumer;
    private final Supplier<PathHandler> pathHandlerSupplier;
    private final Supplier<AssociationService> associationServiceSupplier;
    private final Supplier<LocalTransactionContext> localTransactionContextSupplier;
    private final Function<String, Boolean> classResolverFilter;

    public EJBRemoteHTTPService(final Consumer<EJBRemoteHTTPService> serviceConsumer, final Supplier<PathHandler> pathHandlerSupplier,
                                final Supplier<AssociationService> associationServiceSupplier, final Supplier<LocalTransactionContext> localTransactionContextSupplier, final Function<String, Boolean> classResolverFilter) {
        this.serviceConsumer = serviceConsumer;
        this.pathHandlerSupplier = pathHandlerSupplier;
        this.associationServiceSupplier = associationServiceSupplier;
        this.localTransactionContextSupplier = localTransactionContextSupplier;
        this.classResolverFilter = classResolverFilter;
    }

    @Override
    public void start(StartContext context) throws StartException {
        HttpRemoteEjbService service = new HttpRemoteEjbService(associationServiceSupplier.get().getAssociation(),
                null, localTransactionContextSupplier.get(), classResolverFilter);
        pathHandlerSupplier.get().addPrefixPath("/ejb", service.createHttpHandler());
    }

    @Override
    public void stop(StopContext context) {
        pathHandlerSupplier.get().removePrefixPath("/ejb");
    }

    @Override
    public EJBRemoteHTTPService getValue() throws IllegalStateException, IllegalArgumentException {
        return this;
    }
}
