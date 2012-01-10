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

import org.infinispan.Cache;
import org.jboss.as.clustering.GroupMembershipNotifierRegistry;
import org.jboss.as.clustering.registry.Registry;
import org.jboss.as.ejb3.EjbLogger;
import org.jboss.as.ejb3.deployment.DeploymentRepository;
import org.jboss.as.ejb3.remote.protocol.versionone.VersionOneProtocolChannelReceiver;
import org.jboss.as.network.ClientMapping;
import org.jboss.as.network.SocketBinding;
import org.jboss.as.remoting.AbstractStreamServerService;
import org.jboss.as.remoting.InjectedSocketBindingStreamServerService;
import org.jboss.as.server.ServerEnvironment;
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
import org.jboss.remoting3.OpenListener;
import org.jboss.remoting3.Registration;
import org.jboss.remoting3.ServiceRegistrationException;
import org.xnio.IoUtils;
import org.xnio.OptionMap;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Collections;
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
    public static final ServiceName EJB_REMOTE_CONNECTOR_CLIENT_MAPPINGS_REGISTRY_SERVICE = ServiceName.JBOSS.append("ejb").append("remoting").append("connector").append("client-mappings-registry-service");

    private final InjectedValue<Endpoint> endpointValue = new InjectedValue<Endpoint>();
    private final InjectedValue<ExecutorService> executorService = new InjectedValue<ExecutorService>();
    private final InjectedValue<DeploymentRepository> deploymentRepositoryInjectedValue = new InjectedValue<DeploymentRepository>();
    private final InjectedValue<EJBRemoteTransactionsRepository> ejbRemoteTransactionsRepositoryInjectedValue = new InjectedValue<EJBRemoteTransactionsRepository>();
    private final InjectedValue<GroupMembershipNotifierRegistry> clusterRegistry = new InjectedValue<GroupMembershipNotifierRegistry>();
    private final InjectedValue<Registry> clientMappingsRegistryService = new InjectedValue<Registry>();
    private final InjectedValue<ServerEnvironment> serverEnvironment = new InjectedValue<ServerEnvironment>();
    private final InjectedValue<Cache> clientMappingsBackingCache = new InjectedValue<Cache>();
    private final ServiceName remotingConnectorServiceName;
    private volatile Registration registration;
    private volatile InjectedSocketBindingStreamServerService remotingServer;
    private final byte serverProtocolVersion;
    private final String[] supportedMarshallingStrategies;

    public EJBRemoteConnectorService(final byte serverProtocolVersion, final String[] supportedMarshallingStrategies, final ServiceName remotingConnectorServiceName) {
        this.serverProtocolVersion = serverProtocolVersion;
        this.supportedMarshallingStrategies = supportedMarshallingStrategies;
        this.remotingConnectorServiceName = remotingConnectorServiceName;
    }

    @Override
    public void start(StartContext context) throws StartException {
        // get the remoting server (which allows remoting connector to connect to it) service
        final ServiceContainer serviceContainer = context.getController().getServiceContainer();
        final ServiceController streamServerServiceController = serviceContainer.getRequiredService(this.remotingConnectorServiceName);
        final AbstractStreamServerService streamServerService = (AbstractStreamServerService) streamServerServiceController.getService();
        // we can only work off a remoting connector which is backed by a socketbinding
        if (streamServerService instanceof InjectedSocketBindingStreamServerService) {
            this.remotingServer = (InjectedSocketBindingStreamServerService) streamServerService;
        }

        // populate the client-mapping cache which will be used for getting the client-mapping(s)
        // of each node's EJB remoting connector's socketbinding
        this.populateClientMappingsCache();

        // Register a EJB channel open listener
        final OpenListener channelOpenListener = new ChannelOpenListener();
        try {
            registration = endpointValue.getValue().registerService(EJB_CHANNEL_NAME, channelOpenListener, OptionMap.EMPTY);
        } catch (ServiceRegistrationException e) {
            throw new StartException(e);
        }
    }

    @Override
    public void stop(StopContext context) {
        this.remotingServer = null;
        registration.close();
    }

    @Override
    public EJBRemoteConnectorService getValue() throws IllegalStateException, IllegalArgumentException {
        return this;
    }

    public InjectedValue<Endpoint> getEndpointInjector() {
        return endpointValue;
    }

    private void sendVersionMessage(final Channel channel) throws IOException {
        final DataOutputStream outputStream = new DataOutputStream(channel.writeMessage());
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
            outputStream.close();
        }
    }

    private class ChannelOpenListener implements OpenListener {

        ChannelOpenListener() {
        }

        @Override
        public void channelOpened(Channel channel) {
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
                EJBRemoteConnectorService.this.sendVersionMessage(channel);
            } catch (IOException e) {
                EjbLogger.EJB3_LOGGER.closingChannel(channel, e);
                IoUtils.safeClose(channel);
            }

            // receive messages from the client
            channel.receiveMessage(new ClientVersionMessageReceiver());
        }

        @Override
        public void registrationTerminated() {
        }
    }

    private class ClientVersionMessageReceiver implements Channel.Receiver {

        ClientVersionMessageReceiver() {
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
                        final GroupMembershipNotifierRegistry groupMembershipNotifierRegistry = EJBRemoteConnectorService.this.clusterRegistry.getValue();
                        // the registry will be available when the clustering subsytem is present, so get the value optionally
                        final Registry<String, List<ClientMapping>> clientMappingRegistry = EJBRemoteConnectorService.this.clientMappingsRegistryService.getOptionalValue();
                        final VersionOneProtocolChannelReceiver receiver = new VersionOneProtocolChannelReceiver(channel, deploymentRepository,
                                EJBRemoteConnectorService.this.ejbRemoteTransactionsRepositoryInjectedValue.getValue(), groupMembershipNotifierRegistry,
                                clientMappingRegistry, marshallerFactory, executorService.getValue());
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

    public Injector<GroupMembershipNotifierRegistry> getClusterRegistryInjector() {
        return this.clusterRegistry;
    }

    public Injector<Registry> getClientMappingsRegistryServiceInjector() {
        return this.clientMappingsRegistryService;
    }

    public Injector<ServerEnvironment> getServerEnvironmentInjector() {
        return this.serverEnvironment;
    }

    public Injector<Cache> getClientMappingsBackingCacheInjector() {
        return this.clientMappingsBackingCache;
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

    private void populateClientMappingsCache() {
        final Cache<String, List<ClientMapping>> clientMappingsCache = this.clientMappingsBackingCache.getOptionalValue();
        if (clientMappingsCache == null) {
            // the cache into which we were planning to add the client mapping isn't available (valid case),
            // so just return
            return;
        }
        // without the remoting server for the connector, we can't get hold off the socket binding and ultimately
        // the client mappings. So we just return without populating the cache
        if (this.remotingServer == null) {
            return;
        }
        final SocketBinding socketBinding = this.remotingServer.getSocketBinding();
        List<ClientMapping> clientMappings = socketBinding.getClientMappings();
        final String nodeName = this.serverEnvironment.getValue().getNodeName();
        if (clientMappings == null || clientMappings.isEmpty()) {
            // TODO: We use the textual form of IP address as the destination address for now.
            // This needs to be configurable (i.e. send either host name or the IP address). But
            // since this is a corner case (i.e. absence of any client-mappings for a socket binding),
            // this should be OK for now
            final String destinationAddress = socketBinding.getAddress().getHostAddress();
            final InetAddress clientNetworkAddress;
            try {
                clientNetworkAddress = InetAddress.getByName("::");
            } catch (UnknownHostException e) {
                throw new RuntimeException(e);
            }
            final ClientMapping defaultClientMapping = new ClientMapping(clientNetworkAddress, 0, destinationAddress, socketBinding.getAbsolutePort());
            // add to cache
            clientMappingsCache.put(nodeName, Collections.singletonList(defaultClientMapping));
        } else {
            // add the client-mappings of the EJB remoting connector on this node, to the cache
            clientMappingsCache.put(nodeName, clientMappings);
        }
    }
}
