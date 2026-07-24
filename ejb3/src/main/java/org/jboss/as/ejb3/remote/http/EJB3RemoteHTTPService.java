/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.ejb3.remote.http;

import java.util.function.Function;
import java.util.function.Supplier;
import io.undertow.server.handlers.PathHandler;
import org.jboss.as.ejb3.remote.AssociationService;
import org.jboss.msc.Service;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.wildfly.httpclient.ejb.HttpRemoteEjbService;
import org.wildfly.transaction.client.LocalTransactionContext;

/**
 * A connector to allow remote EJB clients to connect via EJB/HTTP.
 *
 * @author Stuart Douglas
 */
public class EJB3RemoteHTTPService implements Service {

    public static final ServiceName SERVICE_NAME = ServiceName.JBOSS.append("ejb", "remote", "http-invoker");

    private final Supplier<PathHandler> pathHandlerSupplier;
    private final Supplier<AssociationService> associationServiceSupplier;
    private final Supplier<LocalTransactionContext> transactionContextSupplier;
    private final Function<String, Boolean> classResolverFilter;

    public EJB3RemoteHTTPService(final Supplier<PathHandler> pathHandlerSupplier,
                                 final Supplier<AssociationService> associationServiceSupplier,
                                 final Supplier<LocalTransactionContext> transactionContextSupplier,
                                 final Function<String, Boolean> classResolverFilter) {
        this.pathHandlerSupplier = pathHandlerSupplier;
        this.associationServiceSupplier = associationServiceSupplier;
        this.transactionContextSupplier = transactionContextSupplier;
        this.classResolverFilter = classResolverFilter;
    }

    @Override
    public void start(StartContext context) throws StartException {
        HttpRemoteEjbService service = new HttpRemoteEjbService(associationServiceSupplier.get().getAssociation(),
                null, transactionContextSupplier.get(), classResolverFilter);
        pathHandlerSupplier.get().addPrefixPath("/ejb", service.createHttpHandler());
    }

    @Override
    public void stop(StopContext context) {
        pathHandlerSupplier.get().removePrefixPath("/ejb");
    }
}
