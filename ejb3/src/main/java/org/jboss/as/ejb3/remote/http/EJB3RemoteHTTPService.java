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
import org.jboss.ejb.server.Association;
import org.jboss.msc.Service;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.wildfly.httpclient.ejb.HttpRemoteEjbService;
import org.wildfly.transaction.client.LocalTransactionContext;

/**
 * Service that allows remote EJB clients to connect using the HTTP protocol.
 *
 * @author Stuart Douglas
 */
public class EJB3RemoteHTTPService implements Service {

    public static final ServiceName SERVICE_NAME = ServiceName.JBOSS.append("ejb", "remote", "http-invoker");

    private final Consumer<EJB3RemoteHTTPService> remoteHTTPServiceConsumer;
    private final Supplier<PathHandler> pathHandlerSupplier;
    private final Supplier<AssociationService> associationServiceSupplier;
    private final Supplier<LocalTransactionContext> transactionContextSupplier;
    private final Function<String, Boolean> classResolverFilter;

    public EJB3RemoteHTTPService(final Consumer<EJB3RemoteHTTPService> remoteHTTPServiceConsumer,
                                 final Supplier<PathHandler> pathHandlerSupplier,
                                 final Supplier<AssociationService> associationServiceSupplier,
                                 final Supplier<LocalTransactionContext> transactionContextSupplier,
                                 final Function<String, Boolean> classResolverFilter) {
        this.remoteHTTPServiceConsumer = remoteHTTPServiceConsumer;
        this.pathHandlerSupplier = pathHandlerSupplier;
        this.associationServiceSupplier = associationServiceSupplier;
        this.transactionContextSupplier = transactionContextSupplier;
        this.classResolverFilter = classResolverFilter;
    }

    @Override
    public void start(StartContext context) throws StartException {
        Association association = associationServiceSupplier.get().getDelegator();
        LocalTransactionContext localTransactionContext = transactionContextSupplier.get();
        HttpRemoteEjbService service = new HttpRemoteEjbService(association, null, localTransactionContext, classResolverFilter);
        pathHandlerSupplier.get().addPrefixPath("/ejb", service.createHttpHandler());
        remoteHTTPServiceConsumer.accept(this);
    }

    @Override
    public void stop(StopContext context) {
        remoteHTTPServiceConsumer.accept(null);
        pathHandlerSupplier.get().removePrefixPath("/ejb");
    }
}
