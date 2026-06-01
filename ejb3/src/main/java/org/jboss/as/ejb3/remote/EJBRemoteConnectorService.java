/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.ejb3.remote;

import java.util.concurrent.Executor;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import org.jboss.ejb.protocol.remote.RemoteEJBService;
import org.jboss.logging.Logger;
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
 * Service that allows remote EJB clients to connect using the Remoting protocol.
 *
 * @author <a href="mailto:cdewolf@redhat.com">Carlo de Wolf</a>
 */
public class EJBRemoteConnectorService implements Service {

    private static Logger logger = Logger.getLogger("org.jboss.as.ejb3.remote.EJBRemoteConenctorSefrvice");

    // TODO: Should this be exposed via the management APIs?
    private static final String EJB_CHANNEL_NAME = "jboss.ejb";

    public static final ServiceName SERVICE_NAME = ServiceName.JBOSS.append("ejb3", "connector");

    private final Consumer<EJBRemoteConnectorService> connectorServiceConsumer;
    private final Supplier<Endpoint> endpointSupplier;
    private final Supplier<Executor> executorSupplier;
    private final Supplier<AssociationService> associationServiceSupplier;
    private final Supplier<RemotingTransactionService> remotingTransactionServiceSupplier;
    private volatile Registration registration;
    private final OptionMap channelCreationOptions;
    private final Function<String, Boolean> classResolverFilter;

    public EJBRemoteConnectorService(
            final Consumer<EJBRemoteConnectorService> connectorServiceConsumer,
            final Supplier<Endpoint> endpointSupplier,
            final Supplier<Executor> executorSupplier,
            final Supplier<AssociationService> associationServiceSupplier,
            final Supplier<RemotingTransactionService> remotingTransactionServiceSupplier,
            final OptionMap channelCreationOptions, final Function<String, Boolean> classResolverFilter) {
        this.connectorServiceConsumer = connectorServiceConsumer;
        this.endpointSupplier = endpointSupplier;
        this.executorSupplier = executorSupplier;
        this.associationServiceSupplier = associationServiceSupplier;
        this.remotingTransactionServiceSupplier = remotingTransactionServiceSupplier;
        this.channelCreationOptions = channelCreationOptions;
        this.classResolverFilter = classResolverFilter;
    }

    @Override
    public void start(StartContext context) throws StartException {
        logger.trace("Starting EJB remote connector");

        final AssociationService associationService = associationServiceSupplier.get();
        final Endpoint endpoint = endpointSupplier.get();
        Executor executor = executorSupplier.get();
        if (executor != null) {
            associationService.setExecutor(executor);
        }
        RemoteEJBService remoteEJBService = RemoteEJBService.create(
            associationService.getDelegator(),
            remotingTransactionServiceSupplier.get(),
            classResolverFilter
        );

        logger.trace("Calling serverUp");
        remoteEJBService.serverUp();

        // Register an EJB channel open listener
        OpenListener channelOpenListener = remoteEJBService.getOpenListener();
        try {
            registration = endpoint.registerService(EJB_CHANNEL_NAME, channelOpenListener, this.channelCreationOptions);
        } catch (ServiceRegistrationException e) {
            throw new StartException(e);
        }
        connectorServiceConsumer.accept(this);
        logger.trace("Started EJB remote connector");
    }

    @Override
    public void stop(StopContext context) {
        logger.trace("Stopping EJB remote connector");
        connectorServiceConsumer.accept(null);
        final AssociationService associationService = associationServiceSupplier.get();
        associationService.sendTopologyUpdateIfLastNodeToLeave();
        associationService.setExecutor(null);
        registration.close();
        logger.trace("Stopped EJB remote connector");
    }

}
