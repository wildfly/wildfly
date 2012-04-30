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
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ProxyOperationAddressTranslator;
import org.jboss.as.controller.client.OperationAttachments;
import org.jboss.as.controller.client.OperationMessageHandler;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.EXTENSION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MANAGEMENT_MAJOR_VERSION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MANAGEMENT_MINOR_VERSION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PRODUCT_NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PRODUCT_VERSION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RELEASE_CODENAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RELEASE_VERSION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.controller.remote.RemoteProxyController;
import org.jboss.as.controller.transform.TransformationTarget;
import org.jboss.as.controller.transform.TransformationTargetImpl;
import org.jboss.as.controller.transform.Transformers;
import org.jboss.as.domain.controller.DomainController;
import org.jboss.as.domain.controller.DomainControllerMessages;
import org.jboss.as.domain.controller.SlaveRegistrationException;
import org.jboss.as.domain.controller.operations.ReadMasterDomainModelHandler;
import static org.jboss.as.host.controller.HostControllerLogger.DOMAIN_LOGGER;
import static org.jboss.as.process.protocol.ProtocolUtils.expectHeader;

import org.jboss.as.host.controller.HostControllerMessages;
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
import java.util.Collection;
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
    private final OperationExecutor operationExecutor;
    private final DomainController domainController;

    public HostControllerRegistrationHandler(ManagementChannelHandler handler, DomainController domainController, OperationExecutor operationExecutor) {
        this.handler = handler;
        this.operationExecutor = operationExecutor;
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
                return new InitiateRegistrationHandler();
            case DomainControllerProtocol.REQUEST_SUBSYSTEM_VERSIONS:
                // register the subsystem versions
                return new RegisterSubsystemVersionsHandler();
            case DomainControllerProtocol.COMPLETE_HOST_CONTROLLER_REGISTRATION:
                // Complete the registration process
                return new CompleteRegistrationHandler();
        }
        return handlers.resolveNext();
    }

    /**
     * wrapper to the DomainController and the underlying {@code ModelController} to execute
     * a {@code OperationStepHandler} implementation directly, bypassing normal domain coordination layer.
     */
    public interface OperationExecutor {

        /**
         * Execute the operation.
         *
         * @param operation operation
         * @param handler the message handler
         * @param control the transaction control
         * @param attachments the operation attachments
         * @param step the step to be executed
         * @return the result
         */
        ModelNode execute(ModelNode operation, OperationMessageHandler handler, ModelController.OperationTransactionControl control, OperationAttachments attachments, OperationStepHandler step);

    }

    class InitiateRegistrationHandler implements ManagementRequestHandler<Void, RegistrationContext> {

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
                    try {
                        // The domain model is going to be sent as part of the prepared notification
                        final OperationStepHandler handler = new HostRegistrationStepHandler(registration);
                        operationExecutor.execute(READ_DOMAIN_MODEL, OperationMessageHandler.logging, registration, OperationAttachments.EMPTY, handler);
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

    class RegisterSubsystemVersionsHandler implements ManagementRequestHandler<Void, RegistrationContext> {

        @Override
        public void handleRequest(DataInput input, ActiveOperation.ResultHandler<Void> resultHandler, ManagementRequestContext<RegistrationContext> context) throws IOException {
            final byte status = input.readByte();
            final ModelNode subsystems = new ModelNode();
            subsystems.readExternal(input);

            final RegistrationContext registration = context.getAttachment();
            if(status == DomainControllerProtocol.PARAM_OK) {
                registration.setSubsystems(subsystems, context);
            } else {
                registration.setSubsystems(null, context);
            }
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

    class HostRegistrationStepHandler implements OperationStepHandler {

        private RegistrationContext registrationContext;
        protected HostRegistrationStepHandler(final RegistrationContext registrationContext) {
            this.registrationContext = registrationContext;
        }

        @Override
        public void execute(final OperationContext context, final ModelNode operation) throws OperationFailedException {
            // First lock the domain controller
            context.acquireControllerLock();
            // Read the domain root model
            final Resource root = context.readResource(PathAddress.EMPTY_ADDRESS);
            // Check the mgmt version
            final ModelNode hostInfo = registrationContext.hostInfo;
            boolean as711 = hostInfo.get(MANAGEMENT_MAJOR_VERSION).asInt() == 1 && hostInfo.get(MANAGEMENT_MINOR_VERSION).asInt() == 1;
            final ModelNode subsystems;
            if(as711) {
                throw HostControllerMessages.MESSAGES.unsupportedManagementVersionForHost(hostInfo.get(MANAGEMENT_MAJOR_VERSION).asInt(),
                        hostInfo.get(MANAGEMENT_MINOR_VERSION).asInt(), 1, 2);
            } else {
                // Build the extensions list
                final ModelNode extensions = new ModelNode();
                final Collection<Resource.ResourceEntry> resources = root.getChildren(EXTENSION);
                for(final Resource.ResourceEntry entry : resources) {
                    extensions.add(entry.getName());
                }
                // Remotely resolve the subsystem versions
                subsystems = registrationContext.resolveSubsystemVersions(extensions);
            }
            // Now run the read-domain model operation
            final ReadMasterDomainModelHandler handler = new ReadMasterDomainModelHandler(registrationContext.transformers);
            final ModelNode op = READ_DOMAIN_MODEL.clone();
            op.get(SUBSYSTEM).set(subsystems);
            context.addStep(op, handler, OperationContext.Stage.MODEL);
            // Complete
            context.completeStep();
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

        private final CountDownLatch subsystemsLatch = new CountDownLatch(1);
        private ModelNode subsystems;
        private Transformers transformers;

        protected synchronized void initialize(final String hostName, final ModelNode hostInfo, final ManagementRequestContext<RegistrationContext> responseChannel) {
            this.hostName = hostName;
            this.hostInfo = hostInfo;
            this.responseChannel = responseChannel;
        }

        protected ModelNode resolveSubsystemVersions(final ModelNode extensions) {
            synchronized (this) {
                // Check again with the controller lock held
                if(domainController.isHostRegistered(hostName)) {
                    failed(SlaveRegistrationException.ErrorCode.HOST_ALREADY_EXISTS, DomainControllerMessages.MESSAGES.slaveAlreadyRegistered(hostName));
                    throw new IllegalStateException();
                }
                try {
                    // Send the versions
                    sendResponse(responseChannel, DomainControllerProtocol.PARAM_OK, extensions);
                } catch (IOException e) {
                    ProtocolLogger.ROOT_LOGGER.debugf(e, "failed to process message");
                    failed(SlaveRegistrationException.ErrorCode.UNKNOWN, e.getClass().getName() + ":" + e.getMessage());
                    throw new IllegalStateException(e);
                }
            }

            try {
                subsystemsLatch.await();
            } catch (InterruptedException e) {
                failed(SlaveRegistrationException.ErrorCode.UNKNOWN, e.getClass().getName() + ":" + e.getMessage());
                Thread.currentThread().interrupt();
                throw new IllegalStateException(e);
            }
            if(failed) {
                throw new IllegalStateException();
            }
            final ModelNode subsystems;
            synchronized (this) {
                subsystems = this.subsystems;
            }
            if(subsystems == null) {
                throw new IllegalStateException();
            }
            return subsystems;
        }

        protected void setSubsystems(final ModelNode resolved, final ManagementRequestContext<RegistrationContext> responseChannel) {
            synchronized (this) {
                if(failed) {
                    throw new IllegalStateException();
                }
                this.responseChannel = responseChannel;
                setSubsystemVersions(resolved);
                subsystemsLatch.countDown();
            }
        }

        protected void setSubsystemVersions(final ModelNode subsystems) {
            this.subsystems = subsystems;
            int major = hostInfo.get(MANAGEMENT_MAJOR_VERSION).asInt();
            int minor = hostInfo.get(MANAGEMENT_MINOR_VERSION).asInt();
            TransformationTarget target = TransformationTargetImpl.create(major, minor, subsystems);
            transformers = Transformers.Factory.create(target);
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
                Thread.currentThread().interrupt();
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
//                final TransformingProxyController transforming = TransformingProxyController.Factory.create(proxy, transformers);
                try {
                    // Register proxy controller
//                    domainController.registerRemoteHost(transforming);
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
