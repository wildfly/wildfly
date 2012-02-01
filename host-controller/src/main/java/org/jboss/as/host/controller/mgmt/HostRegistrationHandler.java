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
import org.jboss.as.controller.ProxyController;
import org.jboss.as.controller.ProxyOperationAddressTranslator;
import org.jboss.as.controller.client.OperationAttachments;
import org.jboss.as.controller.client.OperationMessageHandler;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.FAILED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.FAILURE_DESCRIPTION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OUTCOME;
import org.jboss.as.controller.remote.RemoteProxyController;
import org.jboss.as.domain.controller.DomainController;
import org.jboss.as.domain.controller.SlaveRegistrationException;
import org.jboss.as.domain.controller.operations.ReadMasterDomainModelHandler;
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
import org.jboss.dmr.ModelNode;

import java.io.DataInput;
import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Handler responsible for the host-controller registration process.
 *
 * @author Emanuel Muckenhuber
 */
public class HostRegistrationHandler implements ManagementRequestHandlerFactory {

    private static final ModelNode READ_DOMAIN_MODEL = new ModelNode();
    static {
        READ_DOMAIN_MODEL.get(ModelDescriptionConstants.OP).set(ReadMasterDomainModelHandler.OPERATION_NAME);
        READ_DOMAIN_MODEL.get(ModelDescriptionConstants.OP_ADDR).setEmptyList();
        READ_DOMAIN_MODEL.protect();
    }

    private final ManagementChannelHandler handler;
    private final ModelController controller;
    private final DomainController domainController;

    public HostRegistrationHandler(ManagementChannelHandler handler, ModelController controller, DomainController domainController) {
        this.handler = handler;
        this.controller = controller;
        this.domainController = domainController;
    }

    @Override
    public ManagementRequestHandler<?, ?> resolveHandler(final RequestHandlerChain handlers, final ManagementRequestHeader header) {
        final byte operationId = header.getOperationId();
        switch (operationId) {
            case DomainControllerProtocol.REGISTER_HOST_CONTROLLER_REQUEST:
                final RegistrationContext context = new RegistrationContext();
                context.activeOperation = handlers.registerActiveOperation(header.getBatchId(), context);
                return new RegistrationRequestHandler();
            case DomainControllerProtocol.COMPLETE_HOST_CONTROLLER_REGISTRATION:

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
            final ModelNode registrationParams = new ModelNode();
            registrationParams.readExternal(input);

            final RegistrationContext registration = context.getAttachment();
            registration.initialize(hostName, context);
            final ProxyController exiting = null; // domainController.getHostControllerProxy(hostName);
            if(exiting != null) {
                registration.failed(createError(SlaveRegistrationException.ErrorCode.HOST_ALREADY_EXISTS, hostName));
            }
            // Read the domain model async, since this will block until the registration process is complete
            context.executeAsync(new ManagementRequestContext.AsyncTask<RegistrationContext>() {
                @Override
                public void execute(ManagementRequestContext<RegistrationContext> context) throws Exception {
                    final ModelNode result;
                    try {
                        // The domain model is going to be sent as part of the prepared notification
                        result = controller.execute(READ_DOMAIN_MODEL, OperationMessageHandler.logging, registration, OperationAttachments.EMPTY);
                    } catch (Exception e) {
                        final ModelNode failure = new ModelNode();
                        failure.get(OUTCOME).set(FAILED);
                        failure.get(FAILURE_DESCRIPTION).set(e.getClass().getName() + ":" + e.getMessage());
                        registration.failed(failure);
                        return;
                    }
                    // Send a registered notification back
                    registration.sendCompletedMessage(result);
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
            final String message = input.readUTF();
            final RegistrationContext registration = context.getAttachment();
            // Complete the registration
            registration.completeRegistration(context, status == DomainControllerProtocol.PARAM_OK);
        }

    }

    class RegistrationContext implements ModelController.OperationTransactionControl {

        private ManagementRequestContext<RegistrationContext> responseChannel;
        private String hostName;

        private volatile boolean failed;
        private ActiveOperation<Void, RegistrationContext> activeOperation;
        private final AtomicBoolean completed = new AtomicBoolean();
        private final CountDownLatch completedLatch = new CountDownLatch(1);

        protected synchronized void initialize(final String hostName, final ManagementRequestContext<RegistrationContext> responseChannel) {
            this.hostName = hostName;
            this.responseChannel = responseChannel;
        }

        @Override
        public void operationPrepared(final ModelController.OperationTransaction transaction, final ModelNode result) {
            try {
                if(failed) {
                    transaction.rollback();
                } else {
                    registerHost(transaction, result);
                }
            } finally {
                activeOperation.getResultHandler().done(null);
            }
        }

        void registerHost(final ModelController.OperationTransaction transaction, final ModelNode result) {
            synchronized (this) {
                // Check again with the controller lock held
                final ProxyController exiting = null; // domainController.getHostControllerProxy(hostName);
                if(exiting != null) {
                    failed(createError(SlaveRegistrationException.ErrorCode.HOST_ALREADY_EXISTS, hostName));
                    return;
                }
                // Send model back to HC
                try {
                    sendResponse(responseChannel, DomainControllerProtocol.PARAM_OK, result);
                } catch (IOException e) {
                    ProtocolLogger.ROOT_LOGGER.debugf(e, "failed to process message");
                    failed(null);
                    return;
                }
            }
            // wait until either we get a go or no-go
            try {
                completedLatch.await();
            } catch (InterruptedException e) {
                failed(null);
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
                    failed(createError(e.getErrorCode(), hostName));
                    return;
                } catch (Exception e) {
                    failed(createError(SlaveRegistrationException.ErrorCode.NONE, e.getClass().getName() + ":" + e.getMessage()));
                    return;
                }
                // Complete registration
                if(! failed) {
                    transaction.commit();
                } else {
                    transaction.rollback();
                }
            }

        }

        void completeRegistration(final ManagementRequestContext<RegistrationContext> responseChannel, boolean commit) {
            if(completed.compareAndSet(false, true)) {
                failed |= ! commit;
                this.responseChannel = responseChannel;
                completedLatch.countDown();
            }
        }

        void failed(final ModelNode error) {
            if(completed.compareAndSet(false, true)) {
                failed = true;
                try {
                    sendResponse(responseChannel, DomainControllerProtocol.PARAM_ERROR, error);
                } catch (IOException e) {
                    ProtocolLogger.ROOT_LOGGER.debugf(e, "failed to process message");
                } finally {
                    completedLatch.countDown();
                }
            }
        }

        void sendCompletedMessage(final ModelNode result) {
            try {
                sendResponse(responseChannel, DomainControllerProtocol.PARAM_OK, result);
            } catch (IOException e) {
                ProtocolLogger.ROOT_LOGGER.debugf(e, "failed to process message");
            }
        }

    }

    /**
     * Send a operation response.
     *
     * @param context the request context
     * @param responseType the response type
     * @param response the operation response
     * @throws java.io.IOException for any error
     */
    static void sendResponse(final ManagementRequestContext<RegistrationContext> context, final byte responseType, final ModelNode response) throws IOException {
        final ManagementResponseHeader header = ManagementResponseHeader.create(context.getRequestHeader());
        final FlushableDataOutput output = context.writeMessage(header);
        try {
            // response type
            output.writeByte(responseType);
            // operation result
            response.writeExternal(output);
            // response end
            output.writeByte(ManagementProtocol.RESPONSE_END);
            output.close();
        } finally {
            StreamUtils.safeClose(output);
        }
    }

    static ModelNode createError(final SlaveRegistrationException.ErrorCode code, final String message) {
        final ModelNode failure = new ModelNode();
        failure.get(FAILURE_DESCRIPTION).set(message);
        failure.get("error-code").set(code.getCode());
        return failure;
    }

}