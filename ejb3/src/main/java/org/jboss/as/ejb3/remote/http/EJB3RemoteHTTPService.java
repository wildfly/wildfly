/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.ejb3.remote.http;

import java.util.function.Function;

import io.undertow.server.handlers.PathHandler;
import org.jboss.as.ejb3.remote.AssociationService;
import org.jboss.logging.Logger;
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

    protected static final Logger log = Logger.getLogger(EJB3RemoteHTTPService.class.getSimpleName());

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
        log.info("Starting");
        HttpRemoteEjbService service = new HttpRemoteEjbService(associationServiceInjectedValue.getValue().getAssociation(),
                null, localTransactionContextInjectedValue.getValue(), classResolverFilter);
        pathHandlerInjectedValue.getValue().addPrefixPath("/ejb", service.createHttpHandler());
        log.info("Started");
    }

    @Override
    public void stop(StopContext context) {
        log.info("Stopping");
        pathHandlerInjectedValue.getValue().removePrefixPath("/ejb");
        log.info("Stopped");
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
