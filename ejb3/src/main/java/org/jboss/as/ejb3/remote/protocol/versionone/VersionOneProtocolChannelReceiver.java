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

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;

import org.jboss.as.ejb3.logging.EjbLogger;
import org.jboss.as.ejb3.deployment.DeploymentModuleIdentifier;
import org.jboss.as.ejb3.deployment.DeploymentRepository;
import org.jboss.as.ejb3.deployment.DeploymentRepositoryListener;
import org.jboss.as.ejb3.deployment.ModuleDeployment;
import org.jboss.as.ejb3.remote.EJBRemoteTransactionsRepository;
import org.jboss.as.ejb3.remote.RegistryCollector;
import org.jboss.as.ejb3.remote.RemoteAsyncInvocationCancelStatusService;
import org.jboss.as.ejb3.remote.protocol.MessageHandler;
import org.jboss.as.network.ClientMapping;
import org.jboss.as.server.suspend.ServerActivity;
import org.jboss.as.server.suspend.ServerActivityCallback;
import org.jboss.as.server.suspend.SuspendController;
import org.jboss.marshalling.MarshallerFactory;
import org.jboss.remoting3.Channel;
import org.jboss.remoting3.CloseHandler;
import org.jboss.remoting3.MessageInputStream;
import org.jboss.remoting3.MessageOutputStream;
import org.wildfly.clustering.registry.Registry;
import org.xnio.IoUtils;

/**
 * @author Jaikiran Pai
 */
public class VersionOneProtocolChannelReceiver implements Channel.Receiver, DeploymentRepositoryListener,
        RegistryCollector.Listener<String, List<ClientMapping>>, ServerActivity {

    private static final byte HEADER_SESSION_OPEN_REQUEST = 0x01;
    private static final byte HEADER_INVOCATION_REQUEST = 0x03;
    private static final byte HEADER_INVOCATION_CANCELLATION_REQUEST = 0x04;
    private static final byte HEADER_TX_COMMIT_REQUEST = 0x0F;
    private static final byte HEADER_TX_ROLLBACK_REQUEST = 0x10;
    private static final byte HEADER_TX_PREPARE_REQUEST = 0x11;
    private static final byte HEADER_TX_FORGET_REQUEST = 0x12;
    private static final byte HEADER_TX_BEFORE_COMPLETION_REQUEST = 0x13;


    protected final ChannelAssociation channelAssociation;
    protected final DeploymentRepository deploymentRepository;
    protected final EJBRemoteTransactionsRepository transactionsRepository;
    protected final MarshallerFactory marshallerFactory;
    protected final ExecutorService executorService;
    protected final RegistryCollector<String, List<ClientMapping>> clientMappingRegistryCollector;
    protected final Set<ClusterTopologyUpdateListener> clusterTopologyUpdateListeners = Collections.synchronizedSet(new HashSet<ClusterTopologyUpdateListener>());
    protected final RemoteAsyncInvocationCancelStatusService remoteAsyncInvocationCancelStatus;
    protected final SuspendController suspendController;

    public VersionOneProtocolChannelReceiver(final ChannelAssociation channelAssociation, final DeploymentRepository deploymentRepository,
                                             final EJBRemoteTransactionsRepository transactionsRepository, final RegistryCollector<String, List<ClientMapping>> clientMappingRegistryCollector,
                                             final MarshallerFactory marshallerFactory, final ExecutorService executorService,
                                             final RemoteAsyncInvocationCancelStatusService asyncInvocationCancelStatusService, final SuspendController suspendController) {
        this.marshallerFactory = marshallerFactory;
        this.channelAssociation = channelAssociation;
        this.executorService = executorService;
        this.deploymentRepository = deploymentRepository;
        this.transactionsRepository = transactionsRepository;
        this.clientMappingRegistryCollector = clientMappingRegistryCollector;
        this.remoteAsyncInvocationCancelStatus = asyncInvocationCancelStatusService;
        this.suspendController = suspendController;
    }

    public void startReceiving() {
        final Channel channel = this.channelAssociation.getChannel();
        channel.addCloseHandler(new ChannelCloseHandler());

        channel.receiveMessage(this);
        // listen to module availability/unavailability events
        this.deploymentRepository.addListener(this);
        // listen to new clusters (a.k.a groups) being started/stopped
        this.clientMappingRegistryCollector.addListener(this);
        // register as a ServerActivity to be informed of suspend/resume
        this.suspendController.registerActivity(this);
        // Send the cluster topology for existing clusters in the registry
        // and for each of these clusters added ourselves as a listener for cluster
        // topology changes (members added/removed events in the cluster)
        final Collection<Registry<String, List<ClientMapping>>> clusters = this.clientMappingRegistryCollector.getRegistries();
        try {
            // WFLY-4273
            if (clusters != null && clusters.size() > 0) {
                this.sendNewClusterFormedMessage(clusters);
            }
        } catch (IOException ioe) {
            // just log and don't throw an error
            EjbLogger.REMOTE_LOGGER.failedToSendClusterFormationMessageToClient(ioe, channel);
        }
        for (final Registry<String, List<ClientMapping>> cluster : clusters) {
            // add the topology update listener
            final ClusterTopologyUpdateListener clusterTopologyUpdateListener = new ClusterTopologyUpdateListener(cluster, this);
            cluster.addListener(clusterTopologyUpdateListener);

            // keep track of this topology update listener so that we can unregister it when the channel is closed and
            // we no longer are interested in the topology updates
            this.clusterTopologyUpdateListeners.add(clusterTopologyUpdateListener);
        }
    }

    @Override
    public void handleError(Channel channel, IOException error) {
        try {
            channel.close();
        } catch (IOException e) {
            throw EjbLogger.ROOT_LOGGER.couldNotCloseChannel(e);
        } finally {
            this.cleanupOnChannelDown();
        }
    }

    @Override
    public void handleEnd(Channel channel) {
        try {
            channel.close();
        } catch (IOException e) {
            // ignore
        } finally {
            this.cleanupOnChannelDown();
        }
    }

    @Override
    public void handleMessage(Channel channel, MessageInputStream messageInputStream) {
        try {
            // enroll for next message (whenever it's available)
            channel.receiveMessage(this);
            this.processMessage(channel, messageInputStream);

        } catch (Throwable e) {
            // log it
            EjbLogger.REMOTE_LOGGER.exceptionOnChannel(e, channel, messageInputStream);
            // no more messages can be sent or received on this channel
            IoUtils.safeClose(channel);
        } finally {
            IoUtils.safeClose(messageInputStream);
        }
    }

    /**
     * Returns a {@link MessageHandler} which can handle a message represented by the passed <code>header</code>. Returns null if there's no such {@link MessageHandler}
     *
     * @param header The message header
     * @return
     */
    protected MessageHandler getMessageHandler(final byte header) {
        switch (header) {
            case HEADER_INVOCATION_REQUEST:
                return new MethodInvocationMessageHandler(this.deploymentRepository, this.marshallerFactory, this.executorService, this.remoteAsyncInvocationCancelStatus);
            case HEADER_INVOCATION_CANCELLATION_REQUEST:
                return new InvocationCancellationMessageHandler(this.remoteAsyncInvocationCancelStatus);
            case HEADER_SESSION_OPEN_REQUEST:
                return new SessionOpenRequestHandler(this.deploymentRepository, this.marshallerFactory, this.executorService);
            case HEADER_TX_COMMIT_REQUEST:
                return new TransactionRequestHandler(this.transactionsRepository, this.marshallerFactory, this.executorService, TransactionRequestHandler.TransactionRequestType.COMMIT);
            case HEADER_TX_ROLLBACK_REQUEST:
                return new TransactionRequestHandler(this.transactionsRepository, this.marshallerFactory, this.executorService, TransactionRequestHandler.TransactionRequestType.ROLLBACK);
            case HEADER_TX_FORGET_REQUEST:
                return new TransactionRequestHandler(this.transactionsRepository, this.marshallerFactory, this.executorService, TransactionRequestHandler.TransactionRequestType.FORGET);
            case HEADER_TX_PREPARE_REQUEST:
                return new TransactionRequestHandler(this.transactionsRepository, this.marshallerFactory, this.executorService, TransactionRequestHandler.TransactionRequestType.PREPARE);
            case HEADER_TX_BEFORE_COMPLETION_REQUEST:
                return new TransactionRequestHandler(this.transactionsRepository, this.marshallerFactory, this.executorService, TransactionRequestHandler.TransactionRequestType.BEFORE_COMPLETION);
            default:
                return null;
        }
    }

    protected void processMessage(final Channel channel, final InputStream inputStream) throws IOException {
        // read the first byte to see what type of a message it is
        final int header = inputStream.read();
        EjbLogger.REMOTE_LOGGER.tracef("Got message with header 0x%x on channel %s", header, channel);
        final MessageHandler messageHandler = getMessageHandler((byte) header);
        if (messageHandler == null) {
            // enroll for next message (whenever it's available)
            channel.receiveMessage(this);
            // log a message that the message wasn't identified
            EjbLogger.REMOTE_LOGGER.unsupportedMessageHeader(header, channel);
            return;
        }
        // let the message handler process the message
        messageHandler.processMessage(channelAssociation, inputStream);
    }

    @Override
    public void listenerAdded(DeploymentRepository repository) {
        // get the initial available modules and send a message to the client
        final Map<DeploymentModuleIdentifier, ModuleDeployment> availableModules = this.deploymentRepository.getStartedModules();
        if (availableModules != null && !availableModules.isEmpty()) {
            try {
                EjbLogger.REMOTE_LOGGER.debugf("Sending initial module availability message, containing %s module(s) to channel %s", availableModules.size(), this.channelAssociation.getChannel());
                this.sendModuleAvailability(availableModules.keySet().toArray(new DeploymentModuleIdentifier[availableModules.size()]));
            } catch (IOException e) {
                EjbLogger.REMOTE_LOGGER.failedToSendModuleAvailabilityMessageToClient(e, this.channelAssociation.getChannel());
            }
        }
    }

    @Override
    public void deploymentAvailable(DeploymentModuleIdentifier deploymentModuleIdentifier, ModuleDeployment moduleDeployment) {
    }

    @Override
    public void deploymentStarted(DeploymentModuleIdentifier deploymentModuleIdentifier, ModuleDeployment moduleDeployment) {
        try {
            this.sendModuleAvailability(new DeploymentModuleIdentifier[] {deploymentModuleIdentifier});
        } catch (IOException e) {
            EjbLogger.REMOTE_LOGGER.failedToSendModuleAvailabilityMessageToClient(e, deploymentModuleIdentifier, channelAssociation.getChannel());
        }
    }

    @Override
    public void deploymentRemoved(DeploymentModuleIdentifier deploymentModuleIdentifier) {
        try {
            this.sendModuleUnAvailability(new DeploymentModuleIdentifier[] {deploymentModuleIdentifier});
        } catch (IOException e) {
            EjbLogger.REMOTE_LOGGER.failedToSendModuleUnavailabilityMessageToClient(e, deploymentModuleIdentifier, channelAssociation.getChannel());
        }
    }

    private void sendModuleAvailability(DeploymentModuleIdentifier[] availableModules) throws IOException {
        final DataOutputStream outputStream;
        final MessageOutputStream messageOutputStream;
        try {
            messageOutputStream = channelAssociation.acquireChannelMessageOutputStream();
        } catch (Exception e) {
            throw EjbLogger.ROOT_LOGGER.failedToOpenMessageOutputStream(e);
        }
        outputStream = new DataOutputStream(messageOutputStream);
        final ModuleAvailabilityWriter moduleAvailabilityWriter = new ModuleAvailabilityWriter();
        try {
            moduleAvailabilityWriter.writeModuleAvailability(outputStream, availableModules);
        } finally {
            channelAssociation.releaseChannelMessageOutputStream(messageOutputStream);
            outputStream.close();
        }
    }

    private void sendModuleUnAvailability(DeploymentModuleIdentifier[] availableModules) throws IOException {
        final DataOutputStream outputStream;
        final MessageOutputStream messageOutputStream;
        try {
            messageOutputStream = channelAssociation.acquireChannelMessageOutputStream();
        } catch (Exception e) {
            throw EjbLogger.ROOT_LOGGER.failedToOpenMessageOutputStream(e);
        }
        outputStream = new DataOutputStream(messageOutputStream);
        final ModuleAvailabilityWriter moduleAvailabilityWriter = new ModuleAvailabilityWriter();
        try {
            moduleAvailabilityWriter.writeModuleUnAvailability(outputStream, availableModules);
        } finally {
            channelAssociation.releaseChannelMessageOutputStream(messageOutputStream);
            outputStream.close();
        }
    }

    @Override
    public void registryAdded(Registry<String, List<ClientMapping>> cluster) {
        try {
            if (EjbLogger.REMOTE_LOGGER.isDebugEnabled()) {
                EjbLogger.REMOTE_LOGGER.debugf("Received new cluster formation notification for cluster %s", cluster.getGroup().getName());
            }
            this.sendNewClusterFormedMessage(Collections.singleton(cluster));
        } catch (IOException ioe) {
            EjbLogger.REMOTE_LOGGER.failedToSendClusterFormationMessageToClient(ioe, cluster.getGroup().getName(), channelAssociation.getChannel());
        } finally {
            // add a listener for receiving node(s) addition/removal from the cluster
            final ClusterTopologyUpdateListener clusterTopologyUpdateListener = new ClusterTopologyUpdateListener(cluster, this);
            cluster.addListener(clusterTopologyUpdateListener);
            // keep track of this update listener so that we cleanup properly
            this.clusterTopologyUpdateListeners.add(clusterTopologyUpdateListener);
        }
    }

    @Override
    public void registryRemoved(Registry<String, List<ClientMapping>> registry) {
        // When the cluster node count reaches 0 then send a cluster removal message to clean up the ClusterContext on the client
        try {
            if (EjbLogger.REMOTE_LOGGER.isDebugEnabled()) {
                EjbLogger.REMOTE_LOGGER.debugf("Received cluster removal notification for cluster %s", registry.getGroup());
            }
            // when the membership of the cluster being left is 1, we are the last node
            if (registry.getGroup().getNodes().size() == 1) {
                this.sendClusterRemovedMessage(registry);
            }
        } catch (IOException ioe) {
            EjbLogger.REMOTE_LOGGER.couldNotSendClusterRemovalMessage(ioe, registry.getGroup(), channelAssociation.getChannel());
        }
    }

    /**
     * Sends a cluster formation message for the passed clusters, over the remoting channel
     *
     * @param clientMappingRegistries The new clusters
     * @throws IOException If any exception occurs while sending the message over the channel
     */
    private void sendNewClusterFormedMessage(final Collection<Registry<String, List<ClientMapping>>> clientMappingRegistries) throws IOException {
        final DataOutputStream outputStream;
        final MessageOutputStream messageOutputStream;
        try {
            messageOutputStream = channelAssociation.acquireChannelMessageOutputStream();
        } catch (Exception e) {
            throw EjbLogger.ROOT_LOGGER.failedToOpenMessageOutputStream(e);
        }
        outputStream = new DataOutputStream(messageOutputStream);
        final ClusterTopologyWriter clusterTopologyWriter = new ClusterTopologyWriter();
        try {
            EjbLogger.REMOTE_LOGGER.debugf("Writing out cluster formation message for %d clusters, to channel %s", clientMappingRegistries.size(), this.channelAssociation.getChannel());
            clusterTopologyWriter.writeCompleteClusterTopology(outputStream, clientMappingRegistries);
        } finally {
            channelAssociation.releaseChannelMessageOutputStream(messageOutputStream);
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
        final DataOutputStream outputStream;
        final MessageOutputStream messageOutputStream;
        try {
            messageOutputStream = channelAssociation.acquireChannelMessageOutputStream();
        } catch (Exception e) {
            throw EjbLogger.ROOT_LOGGER.failedToOpenMessageOutputStream(e);
        }
        outputStream = new DataOutputStream(messageOutputStream);
        final ClusterTopologyWriter clusterTopologyWriter = new ClusterTopologyWriter();
        try {
            if (EjbLogger.REMOTE_LOGGER.isDebugEnabled()) {
                EjbLogger.REMOTE_LOGGER.debugf("Cluster Ts removed, writing cluster removal message to channel %s", registry.getGroup().getName(), this.channelAssociation.getChannel());
            }
            clusterTopologyWriter.writeClusterRemoved(outputStream, Collections.singleton(registry));
        } finally {
            channelAssociation.releaseChannelMessageOutputStream(messageOutputStream);
            outputStream.close();
        }
    }

    /**
     * Does all the necessary cleanup when a channel is no longer usable
     */
    void cleanupOnChannelDown() {
        // we no longer are interested in cluster topology updates, so unregister the update listener
        synchronized (this.clusterTopologyUpdateListeners) {
            for (final ClusterTopologyUpdateListener clusterTopologyUpdateListener : this.clusterTopologyUpdateListeners) {
                clusterTopologyUpdateListener.unregisterListener();
            }
        }
        this.deploymentRepository.removeListener(this);
        this.clientMappingRegistryCollector.removeListener(this);
        this.suspendController.unRegisterActivity(this);
    }

    class ChannelCloseHandler implements CloseHandler<Channel> {

        @Override
        public void handleClose(Channel closedChannel, IOException exception) {
            EjbLogger.REMOTE_LOGGER.debugf("Channel %s closed", closedChannel);
            VersionOneProtocolChannelReceiver.this.cleanupOnChannelDown();
        }
    }

    @Override
    public void preSuspend(ServerActivityCallback listener) {
        // get the initial available modules and send a message to the client
        final Map<DeploymentModuleIdentifier, ModuleDeployment> availableModules = this.deploymentRepository.getStartedModules();
        if (availableModules != null && !availableModules.isEmpty()) {
            try {
                EjbLogger.ROOT_LOGGER.debugf("Sending module unavailability message on suspend of server, containing %s module(s) to channel %s", availableModules.size(), this.channelAssociation.getChannel());
                this.sendModuleUnAvailability(availableModules.keySet().toArray(new DeploymentModuleIdentifier[availableModules.size()]));
            } catch (IOException e) {
                EjbLogger.ROOT_LOGGER.failedToSendModuleAvailabilityMessageToClient(e, this.channelAssociation.getChannel());
            } finally {
                listener.done();
            }
        }
    }

    @Override
    public void suspended(ServerActivityCallback listener) {
        listener.done();
    }

    @Override
    public void resume() {
        // get the initial available modules and send a message to the client
        final Map<DeploymentModuleIdentifier, ModuleDeployment> availableModules = this.deploymentRepository.getStartedModules();
        if (availableModules != null && !availableModules.isEmpty()) {
            try {
                EjbLogger.ROOT_LOGGER.debugf("Sending module availability message on resume of server, containing %s module(s) to channel %s", availableModules.size(), this.channelAssociation.getChannel());
                this.sendModuleAvailability(availableModules.keySet().toArray(new DeploymentModuleIdentifier[availableModules.size()]));
            } catch (IOException e) {
                EjbLogger.ROOT_LOGGER.failedToSendModuleAvailabilityMessageToClient(e, this.channelAssociation.getChannel());
            }
        }
    }

    /**
     * A {@link org.wildfly.clustering.registry.Registry.Listener} which writes out messages to the client, over a {@link Channel remoting channel}
     * upon cluster topology updates
     */
    private class ClusterTopologyUpdateListener implements Registry.Listener<String, List<ClientMapping>> {
        private final String clusterName;
        private final VersionOneProtocolChannelReceiver channelReceiver;
        private final Registry<String, List<ClientMapping>> cluster;

        ClusterTopologyUpdateListener(Registry<String, List<ClientMapping>> cluster, final VersionOneProtocolChannelReceiver channelReceiver) {
            this.channelReceiver = channelReceiver;
            this.clusterName = cluster.getGroup().getName();
            this.cluster = cluster;
        }

        @Override
        public void addedEntries(Map<String, List<ClientMapping>> added) {
            try {
                this.sendClusterNodesAdded(added);
            } catch (IOException ioe) {
                EjbLogger.REMOTE_LOGGER.failedToSendClusterNodeAdditionMessageToClient(ioe, channelAssociation.getChannel());
            }
        }

        @Override
        public void updatedEntries(Map<String, List<ClientMapping>> updated) {
            // We don't support client mapping updates just yet
        }

        @Override
        public void removedEntries(Map<String, List<ClientMapping>> removed) {
            try {
                this.sendClusterNodesRemoved(removed.keySet());
            } catch (IOException ioe) {
                EjbLogger.REMOTE_LOGGER.failedToSendClusterNodeRemovalMessageToClient(ioe, channelAssociation.getChannel());
            }
        }

        void unregisterListener() {
            this.cluster.removeListener(this);
        }

        private void sendClusterNodesRemoved(final Set<String> removedNodes) throws IOException {
            final DataOutputStream outputStream;
            final MessageOutputStream messageOutputStream;
            try {
                messageOutputStream = channelAssociation.acquireChannelMessageOutputStream();
            } catch (Exception e) {
                throw EjbLogger.ROOT_LOGGER.failedToOpenMessageOutputStream(e);
            }
            outputStream = new DataOutputStream(messageOutputStream);
            final ClusterTopologyWriter clusterTopologyWriter = new ClusterTopologyWriter();
            try {
                if (EjbLogger.REMOTE_LOGGER.isDebugEnabled()) {
                    EjbLogger.REMOTE_LOGGER.debug("Following " + removedNodes.size() + " nodes removed from cluster " + clusterName + ", writing a protocol message to channel " + this.channelReceiver.channelAssociation.getChannel());
                    final StringBuilder sb = new StringBuilder();
                    for (final String nodeName : removedNodes) {
                        sb.append(nodeName);
                        sb.append(System.lineSeparator());
                    }
                    EjbLogger.REMOTE_LOGGER.debug(sb.toString());
                }
                clusterTopologyWriter.writeNodesRemoved(outputStream, clusterName, removedNodes);
            } finally {
                channelAssociation.releaseChannelMessageOutputStream(messageOutputStream);
                outputStream.close();
            }
        }

        private void sendClusterNodesAdded(final Map<String, List<ClientMapping>> addedNodes) throws IOException {
            final DataOutputStream outputStream;
            final MessageOutputStream messageOutputStream;
            try {
                messageOutputStream = channelAssociation.acquireChannelMessageOutputStream();
            } catch (Exception e) {
                throw EjbLogger.ROOT_LOGGER.failedToOpenMessageOutputStream(e);
            }
            outputStream = new DataOutputStream(messageOutputStream);
            final ClusterTopologyWriter clusterTopologyWriter = new ClusterTopologyWriter();
            try {
                if (EjbLogger.REMOTE_LOGGER.isDebugEnabled()) {
                    EjbLogger.REMOTE_LOGGER.debug("Following " + addedNodes.size() + " nodes added to cluster " + clusterName + ", writing a protocol message to channel " + this.channelReceiver.channelAssociation.getChannel());
                    final StringBuilder sb = new StringBuilder();
                    for (final String nodeName : addedNodes.keySet()) {
                        sb.append(nodeName);
                        sb.append(System.lineSeparator());
                    }
                    EjbLogger.REMOTE_LOGGER.debug(sb.toString());
                }

                clusterTopologyWriter.writeNewNodesAdded(outputStream, clusterName, addedNodes);
            } finally {
                channelAssociation.releaseChannelMessageOutputStream(messageOutputStream);
                outputStream.close();
            }
        }
    }
}
