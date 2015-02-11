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

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;

import javax.transaction.TransactionManager;
import javax.transaction.TransactionSynchronizationRegistry;

import org.jboss.as.ejb3.deployment.DeploymentRepository;
import org.jboss.as.ejb3.logging.EjbLogger;
import org.jboss.as.ejb3.remote.protocol.versionone.ChannelAssociation;
import org.jboss.as.ejb3.remote.protocol.versionone.VersionOneProtocolChannelReceiver;
import org.jboss.as.ejb3.remote.protocol.versiontwo.VersionTwoProtocolChannelReceiver;
import org.jboss.as.network.ClientMapping;
import org.jboss.as.remoting.RemotingConnectorBindingInfoService;
import org.jboss.ejb.client.ConstantContextSelector;
import org.jboss.ejb.client.EJBClientTransactionContext;
import org.jboss.ejb.client.remoting.PackedInteger;
import org.jboss.marshalling.MarshallerFactory;
import org.jboss.marshalling.Marshalling;
import org.jboss.msc.inject.Injector;
import org.jboss.msc.service.Service;
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

/**
 * @author <a href="mailto:cdewolf@redhat.com">Carlo de Wolf</a>
 */
public class EJBRemoteConnectorService implements Service<EJBRemoteConnectorService> {

    // TODO: Should this be exposed via the management APIs?
    private static final String EJB_CHANNEL_NAME = "jboss.ejb";

    public static final ServiceName SERVICE_NAME = ServiceName.JBOSS.append("ejb3", "connector");

    private final InjectedValue<Endpoint> endpointValue = new InjectedValue<Endpoint>();
    private final InjectedValue<ExecutorService> executorService = new InjectedValue<ExecutorService>();
    private final InjectedValue<DeploymentRepository> deploymentRepositoryInjectedValue = new InjectedValue<DeploymentRepository>();
    private final InjectedValue<EJBRemoteTransactionsRepository> ejbRemoteTransactionsRepositoryInjectedValue = new InjectedValue<EJBRemoteTransactionsRepository>();
    private final InjectedValue<RegistryCollector> clusterRegistryCollector = new InjectedValue<RegistryCollector>();
    private final InjectedValue<RemoteAsyncInvocationCancelStatusService> remoteAsyncInvocationCancelStatus = new InjectedValue<RemoteAsyncInvocationCancelStatusService>();
    private final InjectedValue<TransactionManager> txManager = new InjectedValue<TransactionManager>();
    private final InjectedValue<TransactionSynchronizationRegistry> txSyncRegistry = new InjectedValue<TransactionSynchronizationRegistry>();
    private final InjectedValue<RemotingConnectorBindingInfoService.RemotingConnectorInfo> remotingConnectorInfoInjectedValue = new InjectedValue<>();
    private volatile Registration registration;
    private final byte serverProtocolVersion;
    private final String[] supportedMarshallingStrategies;
    private final OptionMap channelCreationOptions;

    public EJBRemoteConnectorService(final byte serverProtocolVersion, final String[] supportedMarshallingStrategies) {
        this(serverProtocolVersion, supportedMarshallingStrategies, OptionMap.EMPTY);
    }

    public EJBRemoteConnectorService(final byte serverProtocolVersion, final String[] supportedMarshallingStrategies,
                                     final OptionMap channelCreationOptions) {
        this.serverProtocolVersion = serverProtocolVersion;
        this.supportedMarshallingStrategies = supportedMarshallingStrategies;
        this.channelCreationOptions = channelCreationOptions;
    }

    @Override
    public void start(StartContext context) throws StartException {

        // Register an EJB channel open listener
        final OpenListener channelOpenListener = new ChannelOpenListener();
        try {
            registration = endpointValue.getValue().registerService(EJB_CHANNEL_NAME, channelOpenListener, this.channelCreationOptions);
        } catch (ServiceRegistrationException e) {
            throw new StartException(e);
        }

        // setup an EJBClientTransactionContext backed the transaction manager on this server.
        // This will be used to propagate the transactions from this server to remote servers during EJB invocations
        final EJBClientTransactionContext ejbClientTransactionContext = EJBClientTransactionContext.create(this.txManager.getValue(), this.txSyncRegistry.getValue());
        EJBClientTransactionContext.setSelector(new ConstantContextSelector<EJBClientTransactionContext>(ejbClientTransactionContext));
    }

    @Override
    public void stop(StopContext context) {
        registration.close();
        // reset the EJBClientTransactionContext on this server
        EJBClientTransactionContext.setSelector(new ConstantContextSelector<EJBClientTransactionContext>(null));
    }

    public String getProtocol() {
        return remotingConnectorInfoInjectedValue.getValue().getProtocol();
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

    public List<EjbListenerAddress> getListeningAddresses() {
        final RemotingConnectorBindingInfoService.RemotingConnectorInfo info = remotingConnectorInfoInjectedValue.getValue();
        return Collections.singletonList(new EjbListenerAddress(info.getSocketBinding().getSocketAddress(), info.getProtocol()));
    }

    private void sendVersionMessage(final ChannelAssociation channelAssociation) throws IOException {
        final DataOutputStream outputStream;
        final MessageOutputStream messageOutputStream;
        try {
            messageOutputStream = channelAssociation.acquireChannelMessageOutputStream();
        } catch (Exception e) {
            throw EjbLogger.ROOT_LOGGER.failedToOpenMessageOutputStream(e);
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

        @Override
        public void channelOpened(Channel channel) {
            final ChannelAssociation channelAssociation = new ChannelAssociation(channel);

            EjbLogger.ROOT_LOGGER.tracef("Welcome %s to the " + EJB_CHANNEL_NAME + " channel", channel);
            channel.addCloseHandler(new CloseHandler<Channel>() {
                @Override
                public void handleClose(Channel closed, IOException exception) {
                    // do nothing
                    EjbLogger.ROOT_LOGGER.tracef("channel %s closed", closed);
                }
            });
            // send the server version and supported marshalling types to the client
            try {
                EJBRemoteConnectorService.this.sendVersionMessage(channelAssociation);
            } catch (IOException e) {
                EjbLogger.ROOT_LOGGER.closingChannel(channel, e);
                IoUtils.safeClose(channel);
            }

            // receive messages from the client
            channel.receiveMessage(new ClientVersionMessageReceiver(channelAssociation));
        }

        @Override
        public void registrationTerminated() {
        }
    }

    private class ClientVersionMessageReceiver implements Channel.Receiver {

        private final ChannelAssociation channelAssociation;

        ClientVersionMessageReceiver(final ChannelAssociation channelAssociation) {
            this.channelAssociation = channelAssociation;
        }

        @Override
        public void handleError(Channel channel, IOException error) {
            EjbLogger.ROOT_LOGGER.closingChannel(channel, error);
            try {
                channel.close();
            } catch (IOException ioe) {
                // ignore
            }
        }

        @Override
        public void handleEnd(Channel channel) {
            EjbLogger.ROOT_LOGGER.closingChannelOnChannelEnd(channel);
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
                if (EjbLogger.ROOT_LOGGER.isDebugEnabled()) {
                    EjbLogger.ROOT_LOGGER.debug("Client with protocol version " + version + " and marshalling strategy "
                            + clientMarshallingStrategy + " trying to communicate on " + channel);
                }
                if (!EJBRemoteConnectorService.this.isSupportedMarshallingStrategy(clientMarshallingStrategy)) {
                    EjbLogger.ROOT_LOGGER.unsupportedClientMarshallingStrategy(clientMarshallingStrategy, channel);
                    channel.close();
                    return;
                }
                final MarshallerFactory marshallerFactory = EJBRemoteConnectorService.this.getMarshallerFactory(clientMarshallingStrategy);
                // enroll VersionOneProtocolChannelReceiver for handling subsequent messages on this channel
                final DeploymentRepository deploymentRepository = EJBRemoteConnectorService.this.deploymentRepositoryInjectedValue.getValue();
                final RegistryCollector<String, List<ClientMapping>> clientMappingRegistryCollector = EJBRemoteConnectorService.this.clusterRegistryCollector.getValue();
                final RemoteAsyncInvocationCancelStatusService asyncInvocationCancelStatus = EJBRemoteConnectorService.this.remoteAsyncInvocationCancelStatus.getValue();

                switch (version) {
                    case 0x01:
                        final VersionOneProtocolChannelReceiver versionOneProtocolHandler = new VersionOneProtocolChannelReceiver(this.channelAssociation, deploymentRepository,
                                EJBRemoteConnectorService.this.ejbRemoteTransactionsRepositoryInjectedValue.getValue(), clientMappingRegistryCollector,
                                marshallerFactory, executorService.getValue(), asyncInvocationCancelStatus);
                        // trigger the receiving
                        versionOneProtocolHandler.startReceiving();
                        break;
                    case 0x02:
                        final VersionTwoProtocolChannelReceiver versionTwoProtocolHandler = new VersionTwoProtocolChannelReceiver(this.channelAssociation, deploymentRepository,
                                EJBRemoteConnectorService.this.ejbRemoteTransactionsRepositoryInjectedValue.getValue(), clientMappingRegistryCollector,
                                marshallerFactory, executorService.getValue(), asyncInvocationCancelStatus);
                        // trigger the receiving
                        versionTwoProtocolHandler.startReceiving();
                        break;

                    default:
                        throw EjbLogger.ROOT_LOGGER.ejbRemoteServiceCannotHandleClientVersion(version);
                }

            } catch (IOException e) {
                // log it
                EjbLogger.ROOT_LOGGER.exceptionOnChannel(e, channel, messageInputStream);
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

    public Injector<RemoteAsyncInvocationCancelStatusService> getAsyncInvocationCancelStatusInjector() {
        return this.remoteAsyncInvocationCancelStatus;
    }

    public InjectedValue<RemotingConnectorBindingInfoService.RemotingConnectorInfo> getRemotingConnectorInfoInjectedValue() {
        return remotingConnectorInfoInjectedValue;
    }

    private boolean isSupportedMarshallingStrategy(final String strategy) {
        return Arrays.asList(this.supportedMarshallingStrategies).contains(strategy);
    }

    private MarshallerFactory getMarshallerFactory(final String marshallerStrategy) {
        final MarshallerFactory marshallerFactory = Marshalling.getProvidedMarshallerFactory(marshallerStrategy);
        if (marshallerFactory == null) {
            throw EjbLogger.ROOT_LOGGER.failedToFindMarshallerFactoryForStrategy(marshallerStrategy);
        }
        return marshallerFactory;
    }

    public static class EjbListenerAddress {
        private final InetSocketAddress address;
        private final String protocol;

        public EjbListenerAddress(final InetSocketAddress address, final String protocol) {
            this.address = address;
            this.protocol = protocol;
        }

        public InetSocketAddress getAddress() {
            return address;
        }

        public String getProtocol() {
            return protocol;
        }

        @Override
        public String toString() {
            return "EjbListenerAddress{" +
                    "address=" + address +
                    ", protocol='" + protocol + '\'' +
                    '}';
        }
    }
}
