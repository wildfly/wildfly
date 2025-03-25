/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.ejb3.remote.http;

import java.util.function.Function;

import io.undertow.server.handlers.PathHandler;
import org.jboss.as.ejb3.remote.AssociationService;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;
import org.wildfly.httpclient.ejb.HttpRemoteEjbService;
import org.wildfly.transaction.client.LocalTransactionContext;

/**
 * @author Stuart Douglas
 */
public class EJB3RemoteHTTPService implements Service<EJB3RemoteHTTPService> {

    public static final ServiceName SERVICE_NAME = ServiceName.JBOSS.append("ejb", "remote", "http-invoker");
    private final InjectedValue<PathHandler> pathHandlerInjectedValue = new InjectedValue<>();
    private final InjectedValue<AssociationService> associationServiceInjectedValue = new InjectedValue<>();
    private final InjectedValue<LocalTransactionContext> localTransactionContextInjectedValue = new InjectedValue<>();
    private final Function<String, Boolean> classResolverFilter;

    public EJB3RemoteHTTPService(final Function<String, Boolean> classResolverFilter) {
        this.classResolverFilter = classResolverFilter;
    }

    @Override
    public void start(StartContext context) throws StartException {
        HttpRemoteEjbService service = new HttpRemoteEjbService(associationServiceInjectedValue.getValue().getAssociation(),
                null, localTransactionContextInjectedValue.getValue(), classResolverFilter);
        pathHandlerInjectedValue.getValue().addPrefixPath("/ejb", service.createHttpHandler());
    }

    @Override
    public void stop(StopContext context) {
        pathHandlerInjectedValue.getValue().removePrefixPath("/ejb");
    }

    @Override
    public EJB3RemoteHTTPService getValue() throws IllegalStateException, IllegalArgumentException {
        return this;
    }

    public InjectedValue<PathHandler> getPathHandlerInjectedValue() {
        return pathHandlerInjectedValue;
    }

    public InjectedValue<AssociationService> getAssociationServiceInjectedValue() {
        return associationServiceInjectedValue;
    }

    public InjectedValue<LocalTransactionContext> getLocalTransactionContextInjectedValue() {
        return localTransactionContextInjectedValue;
    }
}
