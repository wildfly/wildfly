/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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

package org.jboss.as.host.controller.mgmt;

import org.jboss.as.controller.ModelController;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ProxyOperationAddressTranslator;
import org.jboss.as.controller.client.OperationAttachments;
import org.jboss.as.controller.client.OperationMessageHandler;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PRODUCT_NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PRODUCT_VERSION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RELEASE_CODENAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RELEASE_VERSION;
import org.jboss.as.controller.remote.RemoteProxyController;
import org.jboss.as.domain.controller.DomainController;
import org.jboss.as.domain.controller.DomainControllerMessages;
import org.jboss.as.domain.controller.SlaveRegistrationException;
import org.jboss.as.domain.controller.operations.ReadMasterDomainModelHandler;
import static org.jboss.as.host.controller.HostControllerLogger.DOMAIN_LOGGER;
import static org.jboss.as.process.protocol.ProtocolUtils.expectHeader;
import org.jboss.as.protocol.ProtocolLogger;
import org.jboss.as.protocol.StreamUtils;
import org.jboss.as.protocol.mgmt.ActiveOperation;
import org.jboss.as.protocol.mgmt.FlushableDataOutput;
import org.jboss.as.protocol.mgmt.ManagementChannelHandler;
import org.jboss.as.protocol.mgmt.ManagementProtocol;
import org.jboss.as.protocol.mgmt.ManagementRequestContext;
import org.jboss.as.protocol.mgmt.ManagementRequestHandler;
import org.jboss.as.protocol.mgmt.ManagementRequestHandlerFactory;
import org.jboss.as.protocol.mgmt.ManagementRequestHeader;
import org.jboss.as.protocol.mgmt.ManagementResponseHeader;
import org.jboss.as.version.ProductConfig;
import org.jboss.dmr.ModelNode;
import org.jboss.remoting3.Channel;
import org.jboss.remoting3.CloseHandler;

import java.io.DataInput;
import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Handler responsible for the host-controller registration process. This may involve assembling the correct
 * {@code ManagementRequestHandlerFactory} based on the version of the host-controller registering.
 *
 * @author Emanuel Muckenhuber
 */
public class HostControllerRegistrationHandler implements ManagementRequestHandlerFactory {

    private static final ModelNode READ_DOMAIN_MODEL = new ModelNode();
    static {
        READ_DOMAIN_MODEL.get(ModelDescriptionConstants.OP).set(ReadMasterDomainModelHandler.OPERATION_NAME);
        READ_DOMAIN_MODEL.get(ModelDescriptionConstants.OP_ADDR).setEmptyList();
        READ_DOMAIN_MODEL.protect();
    }

    private final ManagementChannelHandler handler;
    private final ModelController controller;
    private final DomainController domainController;

    public HostControllerRegistrationHandler(ManagementChannelHandler handler, ModelController controller, DomainController domainController) {
        this.handler = handler;
        this.controller = controller;
        this.domainController = domainController;
    }

    @Override
    public ManagementRequestHandler<?, ?> resolveHandler(final RequestHandlerChain handlers, final ManagementRequestHeader header) {
        final byte operationId = header.getOperationId();
        switch (operationId) {
            case DomainControllerProtocol.REGISTER_HOST_CONTROLLER_REQUEST:
                // Start the registration process
                final RegistrationContext context = new RegistrationContext();
                context.activeOperation = handlers.registerActiveOperation(header.getBatchId(), context);
                return new RegistrationRequestHandler();
            case DomainControllerProtocol.COMPLETE_HOST_CONTROLLER_REGISTRATION:
                // Complete the registration process
                return new CompleteRegistrationHandler();
        }
        return handlers.resolveNext();
    }

    /**
     * The handler for the request request. This will read the domain model and send it back to the host-controller.
     */
    class RegistrationRequestHandler implements ManagementRequestHandler<Void, RegistrationContext> {

        @Override
        public void handleRequest(final DataInput input, final ActiveOperation.ResultHandler<Void> resultHandler, final ManagementRequestContext<RegistrationContext> context) throws IOException {
            expectHeader(input, DomainControllerProtocol.PARAM_HOST_ID);
            final String hostName = input.readUTF();
            final ModelNode hostInfo = new ModelNode();
            hostInfo.readExternal(input);

            final RegistrationContext registration = context.getAttachment();
            registration.initialize(hostName, hostInfo, context);
            if(domainController.isHostRegistered(hostName)) {
                registration.failed(SlaveRegistrationException.ErrorCode.HOST_ALREADY_EXISTS, DomainControllerMessages.MESSAGES.slaveAlreadyRegistered(hostName));
            }
            // Read the domain model async, this will block until the registration process is complete
            context.executeAsync(new ManagementRequestContext.AsyncTask<RegistrationContext>() {
                @Override
                public void execute(ManagementRequestContext<RegistrationContext> context) throws Exception {
                    final Channel channel = context.getChannel();
                    final ModelNode result;
                    try {
                        // The domain model is going to be sent as part of the prepared notification
                        result = controller.execute(READ_DOMAIN_MODEL, OperationMessageHandler.logging, registration, OperationAttachments.EMPTY);
                    } catch (Exception e) {
                        registration.failed(SlaveRegistrationException.ErrorCode.UNKNOWN, e.getClass().getName() + ":" + e.getMessage());
                        return;
                    }
                    // Send a registered notification back
                    registration.sendCompletedMessage();
                    // Make sure that the host controller gets unregistered when the channel is closed
                    channel.addCloseHandler(new CloseHandler<Channel>() {
                        @Override
                        public void handleClose(Channel closed, IOException exception) {
                            if(domainController.isHostRegistered(hostName)) {
                                DOMAIN_LOGGER.lostConnectionToRemoteHost(hostName);
                            }
                            domainController.unregisterRemoteHost(hostName);
                        }
                    });
                }
            });
        }
    }

    /**
     * Handler responsible for completing the registration request.
     */
    static class CompleteRegistrationHandler implements ManagementRequestHandler<Void, RegistrationContext> {

        @Override
        public void handleRequest(final DataInput input, final ActiveOperation.ResultHandler<Void> resultHandler, final ManagementRequestContext<RegistrationContext> context) throws IOException {
            final byte status = input.readByte();
            final String message = input.readUTF(); // Perhaps use message when the host failed
            final RegistrationContext registration = context.getAttachment();
            // Complete the registration
            registration.completeRegistration(context, status == DomainControllerProtocol.PARAM_OK);
        }

    }

    class RegistrationContext implements ModelController.OperationTransactionControl {

        private ManagementRequestContext<RegistrationContext> responseChannel;
        private ModelNode hostInfo;
        private String hostName;

        private volatile boolean failed;
        private ActiveOperation<Void, RegistrationContext> activeOperation;
        private final AtomicBoolean completed = new AtomicBoolean();
        private final CountDownLatch completedLatch = new CountDownLatch(1);

        protected synchronized void initialize(final String hostName, final ModelNode hostInfo, final ManagementRequestContext<RegistrationContext> responseChannel) {
            this.hostName = hostName;
            this.hostInfo = hostInfo;
            this.responseChannel = responseChannel;
        }

        @Override
        public void operationPrepared(final ModelController.OperationTransaction transaction, final ModelNode result) {
            try {
                if(failed) {
                    transaction.rollback();
                } else {
                    registerHost(transaction, result);
                    if(failed) {
                        transaction.rollback();
                    }
                }
            } finally {
                activeOperation.getResultHandler().done(null);
            }
        }

        /**
         * Once the "read-domain-mode" operation is in operationPrepared, send the model back to registering HC.
         * When the model was applied successfully on the client, we process registering the proxy in the domain,
         * otherwise we rollback.
         *
         * @param transaction the model controller tx
         * @param result the prepared result (domain model)
         */
        void registerHost(final ModelController.OperationTransaction transaction, final ModelNode result) {
            synchronized (this) {
                // Check again with the controller lock held
                if(domainController.isHostRegistered(hostName)) {
                    failed(SlaveRegistrationException.ErrorCode.HOST_ALREADY_EXISTS, DomainControllerMessages.MESSAGES.slaveAlreadyRegistered(hostName));
                    return;
                }
                // Send model back to HC
                try {
                    sendResponse(responseChannel, DomainControllerProtocol.PARAM_OK, result);
                } catch (IOException e) {
                    ProtocolLogger.ROOT_LOGGER.debugf(e, "failed to process message");
                    failed(SlaveRegistrationException.ErrorCode.UNKNOWN, e.getClass().getName() + ":" + e.getMessage());
                    return;
                }
            }
            // wait until either we get a go or no-go
            try {
                completedLatch.await();
            } catch (InterruptedException e) {
                failed(SlaveRegistrationException.ErrorCode.UNKNOWN, e.getClass().getName() + ":" + e.getMessage());
                return;
            }
            synchronized (this) {
                // Check if the host-controller boot worked
                if(failed) {
                    return;
                }
                // Create the proxy controller
                final PathAddress addr = PathAddress.pathAddress(PathElement.pathElement(ModelDescriptionConstants.HOST, hostName));
                final RemoteProxyController proxy = RemoteProxyController.create(handler, addr, ProxyOperationAddressTranslator.HOST);
                // Register proxy controller
                try {
                    domainController.registerRemoteHost(proxy);
                } catch (SlaveRegistrationException e) {
                    failed(e.getErrorCode(), e.getErrorMessage());
                    return;
                } catch (Exception e) {
                    failed(SlaveRegistrationException.ErrorCode.UNKNOWN, e.getClass().getName() + ":" + e.getMessage());
                    return;
                }
                // Complete registration
                if(! failed) {
                    transaction.commit();
                } else {
                    transaction.rollback();
                    return;
                }

                final String productName;
                if(hostInfo.hasDefined(PRODUCT_NAME)) {
                    final String name = hostInfo.get(PRODUCT_NAME).asString();
                    final String version1 = hostInfo.get(PRODUCT_VERSION).asString();
                    final String version2 = hostInfo.get(RELEASE_VERSION).asString();
                    productName = ProductConfig.getPrettyVersionString(name, version1, version2);
                } else {
                    String version1 = hostInfo.get(RELEASE_VERSION).asString();
                    String version2 = hostInfo.get(RELEASE_CODENAME).asString();
                    productName = ProductConfig.getPrettyVersionString(null, version1, version2);
                }
                DOMAIN_LOGGER.registeredRemoteSlaveHost(hostName, productName);
            }
        }

        void completeRegistration(final ManagementRequestContext<RegistrationContext> responseChannel, boolean commit) {
            failed |= ! commit;
            this.responseChannel = responseChannel;
            completedLatch.countDown();
        }

        void failed(SlaveRegistrationException.ErrorCode errorCode, String message) {
            failed(errorCode.getCode(), message);
        }

        void failed(byte errorCode, String message) {
            if(completed.compareAndSet(false, true)) {
                failed = true;
                try {
                    sendFailedResponse(responseChannel, errorCode, message);
                } catch (IOException e) {
                    ProtocolLogger.ROOT_LOGGER.debugf(e, "failed to process message");
                } finally {
                    completedLatch.countDown();
                }
            }
        }

        void sendCompletedMessage() {
            if(completed.compareAndSet(false, true)) {
                try {
                    sendResponse(responseChannel, DomainControllerProtocol.PARAM_OK, null);
                } catch (IOException e) {
                    ProtocolLogger.ROOT_LOGGER.debugf(e, "failed to process message");
                }
            }
        }

    }

    /**
     * Send a operation response.
     *
     * @param context the request context
     * @param responseType the response type
     * @param response the operation response
     * @throws IOException for any error
     */
    static void sendResponse(final ManagementRequestContext<RegistrationContext> context, final byte responseType, final ModelNode response) throws IOException {
        final ManagementResponseHeader header = ManagementResponseHeader.create(context.getRequestHeader());
        final FlushableDataOutput output = context.writeMessage(header);
        try {
            // response type
            output.writeByte(responseType);
            if(response != null) {
                // operation result
                response.writeExternal(output);
            }
            // response end
            output.writeByte(ManagementProtocol.RESPONSE_END);
            output.close();
        } finally {
            StreamUtils.safeClose(output);
        }
    }

    /**
     * Send a failed operation response.
     *
     * @param context the request context
     * @param errorCode the error code
     * @param message the operation message
     * @throws IOException for any error
     */
    static void sendFailedResponse(final ManagementRequestContext<RegistrationContext> context, final byte errorCode, final String message) throws IOException {
        final ManagementResponseHeader header = ManagementResponseHeader.create(context.getRequestHeader());
        final FlushableDataOutput output = context.writeMessage(header);
        try {
            // This is an error
            output.writeByte(DomainControllerProtocol.PARAM_ERROR);
            // send error code
            output.writeByte(errorCode);
            // error message
            output.writeUTF(message);
            // response end
            output.writeByte(ManagementProtocol.RESPONSE_END);
            output.close();
        } finally {
            StreamUtils.safeClose(output);
        }
    }

}
