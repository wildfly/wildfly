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

import org.jboss.as.clustering.registry.RegistryCollector;
import org.jboss.as.ejb3.EjbLogger;
import org.jboss.as.ejb3.EjbMessages;
import org.jboss.as.ejb3.deployment.DeploymentRepository;
import org.jboss.as.ejb3.remote.protocol.versionone.ChannelAssociation;
import org.jboss.as.ejb3.remote.protocol.versionone.VersionOneProtocolChannelReceiver;
import org.jboss.as.network.ClientMapping;
import org.jboss.as.remoting.AbstractStreamServerService;
import org.jboss.as.remoting.InjectedSocketBindingStreamServerService;
import org.jboss.as.server.ServerEnvironment;
import org.jboss.ejb.client.ConstantContextSelector;
import org.jboss.ejb.client.EJBClientTransactionContext;
import org.jboss.ejb.client.remoting.PackedInteger;
import org.jboss.logging.Logger;
import org.jboss.marshalling.MarshallerFactory;
import org.jboss.marshalling.Marshalling;
import org.jboss.msc.inject.Injector;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceContainer;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;
import org.jboss.remoting3.Channel;
import org.jboss.remoting3.CloseHandler;
import org.jboss.remoting3.Endpoint;
import org.jboss.remoting3.MessageInputStream;
import org.jboss.remoting3.MessageOutputStream;
import org.jboss.remoting3.OpenListener;
import org.jboss.remoting3.Registration;
import org.jboss.remoting3.ServiceRegistrationException;
import org.xnio.IoUtils;
import org.xnio.OptionMap;

import javax.transaction.TransactionManager;
import javax.transaction.TransactionSynchronizationRegistry;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;

/**
 * @author <a href="mailto:cdewolf@redhat.com">Carlo de Wolf</a>
 */
public class EJBRemoteConnectorService implements Service<EJBRemoteConnectorService> {
    private static final Logger log = Logger.getLogger(EJBRemoteConnectorService.class);

    // TODO: Should this be exposed via the management APIs?
    private static final String EJB_CHANNEL_NAME = "jboss.ejb";

    public static final ServiceName SERVICE_NAME = ServiceName.JBOSS.append("ejb3", "connector");

    private final InjectedValue<Endpoint> endpointValue = new InjectedValue<Endpoint>();
    private final InjectedValue<ExecutorService> executorService = new InjectedValue<ExecutorService>();
    private final InjectedValue<DeploymentRepository> deploymentRepositoryInjectedValue = new InjectedValue<DeploymentRepository>();
    private final InjectedValue<EJBRemoteTransactionsRepository> ejbRemoteTransactionsRepositoryInjectedValue = new InjectedValue<EJBRemoteTransactionsRepository>();
    private final InjectedValue<RegistryCollector> clusterRegistryCollector = new InjectedValue<RegistryCollector>();
    private final InjectedValue<ServerEnvironment> serverEnvironment = new InjectedValue<ServerEnvironment>();
    private final InjectedValue<TransactionManager> txManager = new InjectedValue<TransactionManager>();
    private final InjectedValue<TransactionSynchronizationRegistry> txSyncRegistry = new InjectedValue<TransactionSynchronizationRegistry>();
    private final ServiceName remotingConnectorServiceName;
    private volatile Registration registration;
    private volatile InjectedSocketBindingStreamServerService remotingServer;
    private final byte serverProtocolVersion;
    private final String[] supportedMarshallingStrategies;
    private final OptionMap channelCreationOptions;

    public EJBRemoteConnectorService(final byte serverProtocolVersion, final String[] supportedMarshallingStrategies, final ServiceName remotingConnectorServiceName) {
        this(serverProtocolVersion, supportedMarshallingStrategies, remotingConnectorServiceName, OptionMap.EMPTY);
    }

    public EJBRemoteConnectorService(final byte serverProtocolVersion, final String[] supportedMarshallingStrategies, final ServiceName remotingConnectorServiceName,
                                     final OptionMap channelCreationOptions) {
        this.serverProtocolVersion = serverProtocolVersion;
        this.supportedMarshallingStrategies = supportedMarshallingStrategies;
        this.remotingConnectorServiceName = remotingConnectorServiceName;
        this.channelCreationOptions = channelCreationOptions;
    }

    @Override
    public void start(StartContext context) throws StartException {
        // get the remoting server (which allows remoting connector to connect to it) service
        final ServiceContainer serviceContainer = context.getController().getServiceContainer();
        final ServiceController streamServerServiceController = serviceContainer.getRequiredService(this.remotingConnectorServiceName);
        final AbstractStreamServerService streamServerService = (AbstractStreamServerService) streamServerServiceController.getService();
        // we can only work off a remoting connector which is backed by a socket binding
        if (streamServerService instanceof InjectedSocketBindingStreamServerService) {
            this.remotingServer = (InjectedSocketBindingStreamServerService) streamServerService;
        }

        // Register a EJB channel open listener
        final OpenListener channelOpenListener = new ChannelOpenListener(serviceContainer);
        try {
            registration = endpointValue.getValue().registerService(EJB_CHANNEL_NAME, channelOpenListener, this.channelCreationOptions);
        } catch (ServiceRegistrationException e) {
            throw new StartException(e);
        }

        // setup a EJBClientTransactionContext backed the transaction manager on this server.
        // This will be used to propagate the transactions from this server to remote servers during EJB invocations
        final EJBClientTransactionContext ejbClientTransactionContext = EJBClientTransactionContext.create(this.txManager.getValue(), this.txSyncRegistry.getValue());
        EJBClientTransactionContext.setSelector(new ConstantContextSelector<EJBClientTransactionContext>(ejbClientTransactionContext));
    }

    @Override
    public void stop(StopContext context) {
        this.remotingServer = null;
        registration.close();
        // reset the EJBClientTransactionContext on this server
        EJBClientTransactionContext.setSelector(new ConstantContextSelector<EJBClientTransactionContext>(null));
    }

    @Override
    public EJBRemoteConnectorService getValue() throws IllegalStateException, IllegalArgumentException {
        return this;
    }

    public InjectedValue<Endpoint> getEndpointInjector() {
        return endpointValue;
    }

    public Injector<TransactionManager> getTransactionManagerInjector() {
        return this.txManager;
    }

    public Injector<TransactionSynchronizationRegistry> getTxSyncRegistryInjector() {
        return this.txSyncRegistry;
    }

    private void sendVersionMessage(final ChannelAssociation channelAssociation) throws IOException {
        final DataOutputStream outputStream;
        final MessageOutputStream messageOutputStream;
        try {
            messageOutputStream = channelAssociation.acquireChannelMessageOutputStream();
        } catch (Exception e) {
            throw EjbMessages.MESSAGES.failedToOpenMessageOutputStream(e);
        }
        outputStream = new DataOutputStream(messageOutputStream);
        try {
            // write the version
            outputStream.write(this.serverProtocolVersion);
            // write the marshaller type count
            PackedInteger.writePackedInteger(outputStream, this.supportedMarshallingStrategies.length);
            // write the marshaller types
            for (int i = 0; i < this.supportedMarshallingStrategies.length; i++) {
                outputStream.writeUTF(this.supportedMarshallingStrategies[i]);
            }
        } finally {
            channelAssociation.releaseChannelMessageOutputStream(messageOutputStream);
            outputStream.close();
        }
    }

    private class ChannelOpenListener implements OpenListener {

        private final ServiceContainer serviceContainer;

        ChannelOpenListener(final ServiceContainer serviceContainer) {
            this.serviceContainer = serviceContainer;
        }

        @Override
        public void channelOpened(Channel channel) {
            final ChannelAssociation channelAssociation = new ChannelAssociation(channel);

            log.tracef("Welcome %s to the " + EJB_CHANNEL_NAME + " channel", channel);
            channel.addCloseHandler(new CloseHandler<Channel>() {
                @Override
                public void handleClose(Channel closed, IOException exception) {
                    // do nothing
                    log.tracef("channel %s closed", closed);
                }
            });
            // send the server version and supported marshalling types to the client
            try {
                EJBRemoteConnectorService.this.sendVersionMessage(channelAssociation);
            } catch (IOException e) {
                EjbLogger.EJB3_LOGGER.closingChannel(channel, e);
                IoUtils.safeClose(channel);
            }

            // receive messages from the client
            channel.receiveMessage(new ClientVersionMessageReceiver(this.serviceContainer, channelAssociation));
        }

        @Override
        public void registrationTerminated() {
        }
    }

    private class ClientVersionMessageReceiver implements Channel.Receiver {

        private final ServiceContainer serviceContainer;
        private final ChannelAssociation channelAssociation;

        ClientVersionMessageReceiver(final ServiceContainer serviceContainer, final ChannelAssociation channelAssociation) {
            this.serviceContainer = serviceContainer;
            this.channelAssociation = channelAssociation;
        }

        @Override
        public void handleError(Channel channel, IOException error) {
            EjbLogger.EJB3_LOGGER.closingChannel(channel, error);
            try {
                channel.close();
            } catch (IOException ioe) {
                // ignore
            }
        }

        @Override
        public void handleEnd(Channel channel) {
            EjbLogger.EJB3_LOGGER.closingChannelOnChannelEnd(channel);
            try {
                channel.close();
            } catch (IOException ioe) {
                // ignore
            }
        }

        @Override
        public void handleMessage(Channel channel, MessageInputStream messageInputStream) {
            final DataInputStream dataInputStream = new DataInputStream(messageInputStream);
            try {
                final byte version = dataInputStream.readByte();
                final String clientMarshallingStrategy = dataInputStream.readUTF();
                log.debug("Client with protocol version " + version + " and marshalling strategy " + clientMarshallingStrategy +
                        " trying to communicate on " + channel);
                if (!EJBRemoteConnectorService.this.isSupportedMarshallingStrategy(clientMarshallingStrategy)) {
                    EjbLogger.EJB3_LOGGER.unsupportedClientMarshallingStrategy(clientMarshallingStrategy, channel);
                    channel.close();
                    return;
                }
                switch (version) {
                    case 0x01:
                        final MarshallerFactory marshallerFactory = EJBRemoteConnectorService.this.getMarshallerFactory(clientMarshallingStrategy);
                        // enroll VersionOneProtocolChannelReceiver for handling subsequent messages on this channel
                        final DeploymentRepository deploymentRepository = EJBRemoteConnectorService.this.deploymentRepositoryInjectedValue.getValue();
                        final RegistryCollector<String, List<ClientMapping>> clientMappingRegistryCollector = EJBRemoteConnectorService.this.clusterRegistryCollector.getValue();
                        final VersionOneProtocolChannelReceiver receiver = new VersionOneProtocolChannelReceiver(this.channelAssociation, deploymentRepository,
                                EJBRemoteConnectorService.this.ejbRemoteTransactionsRepositoryInjectedValue.getValue(), clientMappingRegistryCollector,
                                marshallerFactory, executorService.getValue());
                        // trigger the receiving
                        receiver.startReceiving();
                        break;

                    default:
                        throw new RuntimeException("Cannot handle client version " + version);
                }

            } catch (IOException e) {
                // log it
                log.errorf(e, "Exception on channel %s from message %s", channel, messageInputStream);
                IoUtils.safeClose(channel);
            } finally {
                IoUtils.safeClose(messageInputStream);
            }


        }
    }

    public InjectedValue<ExecutorService> getExecutorService() {
        return executorService;
    }

    public Injector<DeploymentRepository> getDeploymentRepositoryInjector() {
        return this.deploymentRepositoryInjectedValue;
    }

    public Injector<EJBRemoteTransactionsRepository> getEJBRemoteTransactionsRepositoryInjector() {
        return this.ejbRemoteTransactionsRepositoryInjectedValue;
    }

    public Injector<RegistryCollector> getClusterRegistryCollectorInjector() {
        return this.clusterRegistryCollector;
    }

    public Injector<ServerEnvironment> getServerEnvironmentInjector() {
        return this.serverEnvironment;
    }

    private boolean isSupportedMarshallingStrategy(final String strategy) {
        return Arrays.asList(this.supportedMarshallingStrategies).contains(strategy);
    }

    private MarshallerFactory getMarshallerFactory(final String marshallerStrategy) {
        final MarshallerFactory marshallerFactory = Marshalling.getProvidedMarshallerFactory(marshallerStrategy);
        if (marshallerFactory == null) {
            throw new RuntimeException("Could not find a marshaller factory for " + marshallerStrategy + " marshalling strategy");
        }
        return marshallerFactory;
    }
}
