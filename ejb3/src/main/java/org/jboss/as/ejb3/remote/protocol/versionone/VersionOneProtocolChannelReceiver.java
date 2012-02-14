/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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

package org.jboss.as.ejb3.remote.protocol.versionone;

import org.jboss.as.clustering.registry.Registry;
import org.jboss.as.clustering.registry.RegistryCollector;
import org.jboss.as.ejb3.deployment.DeploymentModuleIdentifier;
import org.jboss.as.ejb3.deployment.DeploymentRepository;
import org.jboss.as.ejb3.deployment.DeploymentRepositoryListener;
import org.jboss.as.ejb3.deployment.ModuleDeployment;
import org.jboss.as.ejb3.remote.EJBRemoteTransactionsRepository;
import org.jboss.as.network.ClientMapping;
import org.jboss.logging.Logger;
import org.jboss.marshalling.MarshallerFactory;
import org.jboss.remoting3.Channel;
import org.jboss.remoting3.CloseHandler;
import org.jboss.remoting3.MessageInputStream;
import org.xnio.IoUtils;

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;

/**
 * @author Jaikiran Pai
 */
public class VersionOneProtocolChannelReceiver implements Channel.Receiver, DeploymentRepositoryListener,
        RegistryCollector.Listener<String, List<ClientMapping>> {

    /**
     * Logger
     */
    private static final Logger logger = Logger.getLogger(VersionOneProtocolChannelReceiver.class);

    private static final byte HEADER_SESSION_OPEN_REQUEST = 0x01;
    private static final byte HEADER_INVOCATION_REQUEST = 0x03;
    private static final byte HEADER_TX_COMMIT_REQUEST = 0x0F;
    private static final byte HEADER_TX_ROLLBACK_REQUEST = 0x10;
    private static final byte HEADER_TX_PREPARE_REQUEST = 0x11;
    private static final byte HEADER_TX_FORGET_REQUEST = 0x12;
    private static final byte HEADER_TX_BEFORE_COMPLETION_REQUEST = 0x13;

    private final Channel channel;
    private final DeploymentRepository deploymentRepository;
    private final EJBRemoteTransactionsRepository transactionsRepository;
    private final MarshallerFactory marshallerFactory;
    private final ExecutorService executorService;
    private final RegistryCollector<String, List<ClientMapping>> clientMappingRegistryCollector;

    public VersionOneProtocolChannelReceiver(final Channel channel, final DeploymentRepository deploymentRepository,
                                             final EJBRemoteTransactionsRepository transactionsRepository, final RegistryCollector<String, List<ClientMapping>> clientMappingRegistryCollector,
                                             final MarshallerFactory marshallerFactory, final ExecutorService executorService) {
        this.marshallerFactory = marshallerFactory;
        this.channel = channel;
        this.executorService = executorService;
        this.deploymentRepository = deploymentRepository;
        this.transactionsRepository = transactionsRepository;
        this.clientMappingRegistryCollector = clientMappingRegistryCollector;
    }

    public void startReceiving() {
        this.channel.addCloseHandler(new ChannelCloseHandler());

        this.channel.receiveMessage(this);
        // listen to module availability/unavailability events
        this.deploymentRepository.addListener(this);
        // listen to new clusters (a.k.a groups) being started/stopped
        this.clientMappingRegistryCollector.addListener(this);
        // Send the cluster topology for existing clusters in the registry
        // and for each of these clusters added ourselves as a listener for cluster
        // topology changes (members added/removed events in the cluster)
        final Collection<Registry<String, List<ClientMapping>>> clusters = this.clientMappingRegistryCollector.getRegistries();
        try {
            this.sendNewClusterFormedMessage(clusters);
        } catch (IOException ioe) {
            // just log and don't throw an error
            logger.warn("Could not send cluster formation message to the client on channel " + channel, ioe);
        }
        for (final Registry<String, List<ClientMapping>> cluster : clusters) {
            // add the listener
            cluster.addListener(new ClusterTopologyUpdateListener(cluster.getName(), this));
        }
    }

    @Override
    public void handleError(Channel channel, IOException error) {
        try {
            channel.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            this.deploymentRepository.removeListener(this);
            this.clientMappingRegistryCollector.removeListener(this);
        }
    }

    @Override
    public void handleEnd(Channel channel) {
        try {
            channel.close();
        } catch (IOException e) {
            // ignore
        } finally {
            this.deploymentRepository.removeListener(this);
            this.clientMappingRegistryCollector.removeListener(this);
        }
    }

    @Override
    public void handleMessage(Channel channel, MessageInputStream messageInputStream) {
        try {
            // read the first byte to see what type of a message it is
            final int header = messageInputStream.read();
            if (logger.isTraceEnabled()) {
                logger.trace("Got message with header 0x" + Integer.toHexString(header) + " on channel " + channel);
            }
            MessageHandler messageHandler = null;
            switch (header) {
                case HEADER_INVOCATION_REQUEST:
                    messageHandler = new MethodInvocationMessageHandler(this.deploymentRepository, this.marshallerFactory, this.executorService);
                    break;
                case HEADER_SESSION_OPEN_REQUEST:
                    messageHandler = new SessionOpenRequestHandler(this.deploymentRepository, this.marshallerFactory, this.executorService);
                    break;
                case HEADER_TX_COMMIT_REQUEST:
                    messageHandler = new TransactionRequestHandler(this.transactionsRepository, this.marshallerFactory, this.executorService, TransactionRequestHandler.TransactionRequestType.COMMIT);
                    break;
                case HEADER_TX_ROLLBACK_REQUEST:
                    messageHandler = new TransactionRequestHandler(this.transactionsRepository, this.marshallerFactory, this.executorService, TransactionRequestHandler.TransactionRequestType.ROLLBACK);
                    break;
                case HEADER_TX_FORGET_REQUEST:
                    messageHandler = new TransactionRequestHandler(this.transactionsRepository, this.marshallerFactory, this.executorService, TransactionRequestHandler.TransactionRequestType.FORGET);
                    break;
                case HEADER_TX_PREPARE_REQUEST:
                    messageHandler = new TransactionRequestHandler(this.transactionsRepository, this.marshallerFactory, this.executorService, TransactionRequestHandler.TransactionRequestType.PREPARE);
                    break;
                case HEADER_TX_BEFORE_COMPLETION_REQUEST:
                    messageHandler = new TransactionRequestHandler(this.transactionsRepository, this.marshallerFactory, this.executorService, TransactionRequestHandler.TransactionRequestType.BEFORE_COMPLETION);
                    break;
                default:
                    logger.warn("Received unsupported message header 0x" + Integer.toHexString(header) + " on channel " + channel);
                    return;
            }
            // process the message
            messageHandler.processMessage(channel, messageInputStream);
            // enroll for next message (whenever it's available)
            channel.receiveMessage(this);

        } catch (IOException e) {
            // log it
            logger.errorf(e, "Exception on channel %s from message %s", channel, messageInputStream);
            // no more messages can be sent or received on this channel
            IoUtils.safeClose(channel);
        } finally {
            IoUtils.safeClose(messageInputStream);
        }
    }

    @Override
    public void listenerAdded(DeploymentRepository repository) {
        // get the initial available modules and send a message to the client
        final Map<DeploymentModuleIdentifier, ModuleDeployment> availableModules = this.deploymentRepository.getModules();
        if (availableModules != null && !availableModules.isEmpty()) {
            try {
                logger.debug("Sending initial module availability message, containing " + availableModules.size() + " module(s) to channel " + this.channel);
                this.sendModuleAvailability(availableModules.keySet().toArray(new DeploymentModuleIdentifier[availableModules.size()]));
            } catch (IOException e) {
                logger.warn("Could not send initial module availability report to channel " + this.channel, e);
            }
        }
    }

    @Override
    public void deploymentAvailable(DeploymentModuleIdentifier deploymentModuleIdentifier, ModuleDeployment moduleDeployment) {
        try {
            this.sendModuleAvailability(new DeploymentModuleIdentifier[]{deploymentModuleIdentifier});
        } catch (IOException e) {
            logger.warn("Could not send module availability notification of module " + deploymentModuleIdentifier + " to channel " + this.channel, e);
        }
    }

    @Override
    public void deploymentRemoved(DeploymentModuleIdentifier deploymentModuleIdentifier) {
        try {
            this.sendModuleUnAvailability(new DeploymentModuleIdentifier[]{deploymentModuleIdentifier});
        } catch (IOException e) {
            logger.warn("Could not send module un-availability notification of module " + deploymentModuleIdentifier + " to channel " + this.channel, e);
        }
    }

    private void sendModuleAvailability(DeploymentModuleIdentifier[] availableModules) throws IOException {
        final DataOutputStream outputStream = new DataOutputStream(this.channel.writeMessage());
        final ModuleAvailabilityWriter moduleAvailabilityWriter = new ModuleAvailabilityWriter();
        try {
            moduleAvailabilityWriter.writeModuleAvailability(outputStream, availableModules);
        } finally {
            outputStream.close();
        }
    }

    private void sendModuleUnAvailability(DeploymentModuleIdentifier[] availableModules) throws IOException {
        final DataOutputStream outputStream = new DataOutputStream(this.channel.writeMessage());
        final ModuleAvailabilityWriter moduleAvailabilityWriter = new ModuleAvailabilityWriter();
        try {
            moduleAvailabilityWriter.writeModuleUnAvailability(outputStream, availableModules);
        } finally {
            outputStream.close();
        }
    }

    @Override
    public void registryAdded(Registry<String, List<ClientMapping>> registry) {
        try {
            logger.debug("Received new cluster formation notification for cluster " + registry.getName());
            this.sendNewClusterFormedMessage(Collections.singleton(registry));
        } catch (IOException ioe) {
            logger.warn("Could not send a cluster formation message for cluster: " + registry.getName()
                    + " to the client on channel " + channel, ioe);
        }
    }

    @Override
    public void registryRemoved(Registry<String, List<ClientMapping>> registry) {
        // Removal of the registry (service) on one node of a cluster doesn't mean the entire
        // cluster has been removed.
        // TODO: We need a different/better hook for entire cluster removal event
        // Maybe if the cluster node count reaches 0 then send a cluster removal message?
//        try {
//            logger.debug("Received cluster removal notification for cluster " + registry.getName());
//            this.sendClusterRemovedMessage(registry);
//        } catch (IOException ioe) {
//            logger.warn("Could not send a cluster removal message for cluster: " + registry.getName()
//                    + " to the client on channel " + channel, ioe);
//        }
    }

    /**
     * Sends a cluster formation message for the passed clusters, over the remoting channel
     *
     * @param clientMappingRegistries The new clusters
     * @throws IOException If any exception occurs while sending the message over the channel
     */
    private void sendNewClusterFormedMessage(final Collection<Registry<String, List<ClientMapping>>> clientMappingRegistries) throws IOException {
        final DataOutputStream outputStream = new DataOutputStream(this.channel.writeMessage());
        final ClusterTopologyWriter clusterTopologyWriter = new ClusterTopologyWriter();
        try {
            logger.debug("Writing out cluster formation message for " + clientMappingRegistries.size() + " clusters, to channel " + this.channel);
            clusterTopologyWriter.writeCompleteClusterTopology(outputStream, clientMappingRegistries);
        } finally {
            outputStream.close();
        }
    }

    /**
     * Sends out a cluster removal message for the passed cluster, over the remoting channel
     *
     * @param registry The cluster which was removed
     * @throws IOException If any exception occurs while sending the message over the channel
     */
    private void sendClusterRemovedMessage(final Registry<String, List<ClientMapping>> registry) throws IOException {
        final DataOutputStream outputStream = new DataOutputStream(this.channel.writeMessage());
        final ClusterTopologyWriter clusterTopologyWriter = new ClusterTopologyWriter();
        try {
            logger.debug("Cluster " + registry.getName() + " removed, writing cluster removal message to channel " + this.channel);
            clusterTopologyWriter.writeClusterRemoved(outputStream, Collections.singleton(registry));
        } finally {
            outputStream.close();
        }
    }

    private class ChannelCloseHandler implements CloseHandler<Channel> {

        @Override
        public void handleClose(Channel closedChannel, IOException exception) {
            logger.debug("Channel " + closedChannel + " closed");
            VersionOneProtocolChannelReceiver.this.deploymentRepository.removeListener(VersionOneProtocolChannelReceiver.this);
            VersionOneProtocolChannelReceiver.this.clientMappingRegistryCollector.removeListener(VersionOneProtocolChannelReceiver.this);
        }
    }

    /**
     * A {@link org.jboss.as.clustering.GroupMembershipListener} which writes out messages to the client, over a {@link Channel remoting channel}
     * upon cluster topology updates
     */
    private class ClusterTopologyUpdateListener implements Registry.Listener<String, List<ClientMapping>> {
        private final String clusterName;
        private final VersionOneProtocolChannelReceiver channelReceiver;

        ClusterTopologyUpdateListener(final String clusterName, final VersionOneProtocolChannelReceiver channelReceiver) {
            this.channelReceiver = channelReceiver;
            this.clusterName = clusterName;
        }

        @Override
        public void addedEntries(Map<String, List<ClientMapping>> added) {
            try {
                this.sendClusterNodesAdded(added);
            } catch (IOException ioe) {
                logger.warn("Could not write a new cluster node addition message to channel " + this.channelReceiver.channel, ioe);
            }
        }

        @Override
        public void updatedEntries(Map<String, List<ClientMapping>> updated) {
            // We don't support client mapping updates just yet
        }

        @Override
        public void removedEntries(Set<String> removed) {
            try {
                this.sendClusterNodesRemoved(removed);
            } catch (IOException ioe) {
                logger.warn("Could not write a cluster node removal message to channel " + this.channelReceiver.channel, ioe);
            }
        }

        private void sendClusterNodesRemoved(final Set<String> removedNodes) throws IOException {
            final DataOutputStream outputStream = new DataOutputStream(this.channelReceiver.channel.writeMessage());
            final ClusterTopologyWriter clusterTopologyWriter = new ClusterTopologyWriter();
            try {
                logger.debug(removedNodes.size() + " nodes removed from cluster " + clusterName + ", writing a protocol message to channel " + this.channelReceiver.channel);
                clusterTopologyWriter.writeNodesRemoved(outputStream, clusterName, removedNodes);
            } finally {
                outputStream.close();
            }
        }

        private void sendClusterNodesAdded(final Map<String, List<ClientMapping>> addedNodes) throws IOException {
            final DataOutputStream outputStream = new DataOutputStream(this.channelReceiver.channel.writeMessage());
            final ClusterTopologyWriter clusterTopologyWriter = new ClusterTopologyWriter();
            try {
                logger.debug(addedNodes.size() + " nodes added to cluster " + clusterName + ", writing a protocol message to channel " + this.channelReceiver.channel);
                clusterTopologyWriter.writeNewNodesAdded(outputStream, clusterName, addedNodes);
            } finally {
                outputStream.close();
            }
        }
    }
}
