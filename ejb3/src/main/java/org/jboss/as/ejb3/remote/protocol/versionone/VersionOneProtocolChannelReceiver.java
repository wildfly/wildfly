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


import org.jboss.as.ejb3.deployment.DeploymentModuleIdentifier;
import org.jboss.as.ejb3.deployment.DeploymentRepository;
import org.jboss.as.ejb3.deployment.DeploymentRepositoryListener;
import org.jboss.as.ejb3.deployment.ModuleDeployment;
import org.jboss.as.ejb3.remote.EJBRemoteTransactionsRepository;
import org.jboss.logging.Logger;
import org.jboss.remoting3.Channel;
import org.jboss.remoting3.CloseHandler;
import org.jboss.remoting3.MessageInputStream;
import org.xnio.IoUtils;

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ExecutorService;

/**
 * User: jpai
 */
public class VersionOneProtocolChannelReceiver implements Channel.Receiver, DeploymentRepositoryListener {

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

    private final String marshallingStrategy;

    private final ExecutorService executorService;

    public VersionOneProtocolChannelReceiver(final Channel channel, final DeploymentRepository deploymentRepository,
                                             final EJBRemoteTransactionsRepository transactionsRepository, final String marshallingStrategy, final ExecutorService executorService) {
        this.marshallingStrategy = marshallingStrategy;
        this.channel = channel;
        this.executorService = executorService;
        this.deploymentRepository = deploymentRepository;
        this.transactionsRepository = transactionsRepository;
    }

    public void startReceiving() {
        this.channel.addCloseHandler(new ChannelCloseHandler());

        this.channel.receiveMessage(this);
        // listen to module availability/unavailability events
        this.deploymentRepository.addListener(this);
    }

    @Override
    public void handleError(Channel channel, IOException error) {
        try {
            channel.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            this.deploymentRepository.removeListener(this);
        }
        throw new RuntimeException("NYI: .handleError");
    }

    @Override
    public void handleEnd(Channel channel) {
        try {
            channel.close();
        } catch (IOException e) {
            // ignore
        } finally {
            this.deploymentRepository.removeListener(this);
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
                    messageHandler = new MethodInvocationMessageHandler(this.deploymentRepository, this.marshallingStrategy, this.executorService);
                    break;
                case HEADER_SESSION_OPEN_REQUEST:
                    messageHandler = new SessionOpenRequestHandler(this.deploymentRepository, this.marshallingStrategy, this.executorService);
                    break;
                case HEADER_TX_COMMIT_REQUEST:
                    messageHandler = new TransactionRequestHandler(this.transactionsRepository, this.executorService, TransactionRequestHandler.TransactionRequestType.COMMIT, this.marshallingStrategy);
                    break;
                case HEADER_TX_ROLLBACK_REQUEST:
                    messageHandler = new TransactionRequestHandler(this.transactionsRepository, this.executorService, TransactionRequestHandler.TransactionRequestType.ROLLBACK, this.marshallingStrategy);
                    break;
                case HEADER_TX_FORGET_REQUEST:
                    messageHandler = new TransactionRequestHandler(this.transactionsRepository, this.executorService, TransactionRequestHandler.TransactionRequestType.FORGET, this.marshallingStrategy);
                    break;
                case HEADER_TX_PREPARE_REQUEST:
                    messageHandler = new TransactionRequestHandler(this.transactionsRepository, this.executorService, TransactionRequestHandler.TransactionRequestType.PREPARE, this.marshallingStrategy);
                    break;
                case HEADER_TX_BEFORE_COMPLETION_REQUEST:
                    messageHandler = new TransactionRequestHandler(this.transactionsRepository, this.executorService, TransactionRequestHandler.TransactionRequestType.BEFORE_COMPLETION, this.marshallingStrategy);
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
                logger.debug("Sending initial module availabilty message, containing " + availableModules.size() + " module(s) to channel " + this.channel);
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
            // TODO: Change this to WARN once https://issues.jboss.org/browse/REM3-123 is fixed
            logger.debug("Could not send module un-availability notification of module " + deploymentModuleIdentifier + " to channel " + this.channel, e);
        }
    }

    private void sendModuleAvailability(DeploymentModuleIdentifier[] availableModules) throws IOException {
        final DataOutputStream outputStream = new DataOutputStream(this.channel.writeMessage());
        final ModuleAvailabilityWriter moduleAvailabilityWriter = new ModuleAvailabilityWriter(this.marshallingStrategy);
        try {
            moduleAvailabilityWriter.writeModuleAvailability(outputStream, availableModules);
        } finally {
            outputStream.close();
        }
    }

    private void sendModuleUnAvailability(DeploymentModuleIdentifier[] availableModules) throws IOException {
        final DataOutputStream outputStream = new DataOutputStream(this.channel.writeMessage());
        final ModuleAvailabilityWriter moduleAvailabilityWriter = new ModuleAvailabilityWriter(this.marshallingStrategy);
        try {
            moduleAvailabilityWriter.writeModuleUnAvailability(outputStream, availableModules);
        } finally {
            outputStream.close();
        }
    }

    private class ChannelCloseHandler implements CloseHandler<Channel> {

        @Override
        public void handleClose(Channel closedChannel, IOException exception) {
            logger.debug("Channel " + closedChannel + " closed. removing deployment listener " + this);
            VersionOneProtocolChannelReceiver.this.deploymentRepository.removeListener(VersionOneProtocolChannelReceiver.this);
        }

    }
}
