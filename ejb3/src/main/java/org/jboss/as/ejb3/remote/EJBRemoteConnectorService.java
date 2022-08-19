/*
 * JBoss, Home of Professional Open Source.
 * Copyright (c) 2011, Red Hat, Inc., and individual contributors
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
package org.jboss.as.ejb3.remote;

import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import org.jboss.ejb.protocol.remote.RemoteEJBService;
import org.jboss.msc.Service;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.remoting3.Endpoint;
import org.jboss.remoting3.OpenListener;
import org.jboss.remoting3.Registration;
import org.jboss.remoting3.ServiceRegistrationException;
import org.wildfly.transaction.client.provider.remoting.RemotingTransactionService;
import org.xnio.OptionMap;

/**
 * @author <a href="mailto:cdewolf@redhat.com">Carlo de Wolf</a>
 */
public class EJBRemoteConnectorService implements Service {

    // TODO: Should this be exposed via the management APIs?
    private static final String EJB_CHANNEL_NAME = "jboss.ejb";

    public static final ServiceName SERVICE_NAME = ServiceName.JBOSS.append("ejb3", "connector");

    private final Consumer<EJBRemoteConnectorService> serviceConsumer;
    private final Supplier<Endpoint> endpointSupplier;
    private final Supplier<ExecutorService> executorServiceSupplier;
    private final Supplier<AssociationService> associationServiceSupplier;
    private final Supplier<RemotingTransactionService> remotingTransactionServiceSupplier;
    private volatile Registration registration;
    private final OptionMap channelCreationOptions;
    private final Function<String, Boolean> classResolverFilter;

    public EJBRemoteConnectorService(
            final Consumer<EJBRemoteConnectorService> serviceConsumer, final Supplier<Endpoint> endpointSupplier, final Supplier<ExecutorService> executorServiceSupplier,
            final Supplier<AssociationService> associationServiceSupplier, final Supplier<RemotingTransactionService> remotingTransactionServiceSupplier,
            final OptionMap channelCreationOptions, final Function<String, Boolean> classResolverFilter) {
        this.serviceConsumer = serviceConsumer;
        this.endpointSupplier = endpointSupplier;
        this.executorServiceSupplier = executorServiceSupplier;
        this.associationServiceSupplier = associationServiceSupplier;
        this.remotingTransactionServiceSupplier = remotingTransactionServiceSupplier;
        this.channelCreationOptions = channelCreationOptions;
        this.classResolverFilter = classResolverFilter;
    }

    @Override
    public void start(StartContext context) throws StartException {
        final AssociationService associationService = associationServiceSupplier.get();
        final Endpoint endpoint = endpointSupplier.get();
        Executor executor = executorServiceSupplier != null ? executorServiceSupplier.get() : null;
        if (executor != null) {
            associationService.setExecutor(executor);
        }
        RemoteEJBService remoteEJBService = RemoteEJBService.create(
            associationService.getAssociation(),
            remotingTransactionServiceSupplier.get(),
            classResolverFilter
        );
        remoteEJBService.serverUp();

        // Register an EJB channel open listener
        OpenListener channelOpenListener = remoteEJBService.getOpenListener();
        try {
            registration = endpoint.registerService(EJB_CHANNEL_NAME, channelOpenListener, this.channelCreationOptions);
        } catch (ServiceRegistrationException e) {
            throw new StartException(e);
        }
        serviceConsumer.accept(this);
    }

    @Override
    public void stop(StopContext context) {
        serviceConsumer.accept(null);
        final AssociationService associationService = associationServiceSupplier.get();
        associationService.sendTopologyUpdateIfLastNodeToLeave();
        associationService.setExecutor(null);
        registration.close();
    }

}
