/*
 * JBoss, Home of Professional Open Source
 * Copyright 2011 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU Lesser General Public License, v. 2.1.
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License,
 * v.2.1 along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA  02110-1301, USA.
 */
package org.jboss.as.host.controller.mgmt;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.FAILURE_DESCRIPTION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.HOST;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.host.controller.HostControllerMessages.MESSAGES;
import static org.jboss.as.process.protocol.ProtocolUtils.expectHeader;

import java.io.DataInput;
import java.io.File;
import java.io.IOException;

import org.jboss.as.controller.HashUtil;
import org.jboss.as.controller.ModelController;
import org.jboss.as.controller.ModelController.OperationTransactionControl;
import org.jboss.as.controller.client.OperationMessageHandler;
import org.jboss.as.domain.controller.DomainController;
import org.jboss.as.domain.controller.SlaveRegistrationException;
import org.jboss.as.domain.controller.UnregisteredHostChannelRegistry;
import org.jboss.as.domain.controller.UnregisteredHostChannelRegistry.ProxyCreatedCallback;
import org.jboss.as.domain.controller.operations.ReadMasterDomainModelHandler;
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
import org.jboss.as.protocol.mgmt.RequestProcessingException;
import org.jboss.as.repository.HostFileRepository;
import org.jboss.as.repository.RemoteFileRequestAndHandler.RootFileReader;
import org.jboss.dmr.ModelNode;

/**
 * Handles for requests from slave DC to master DC on the 'domain' channel.
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 * @version $Revision: 1.1 $
 */
class MasterDomainControllerOperationHandlerImpl implements ManagementRequestHandlerFactory {

    private final ManagementChannelHandler handler;
    private final ModelController controller;
    private final DomainController domainController;
    private final UnregisteredHostChannelRegistry registry;

    // TODO
    private volatile ManagementRequestHandlerFactory proxyHandler;

    public MasterDomainControllerOperationHandlerImpl(final ManagementChannelHandler handler, final ModelController controller,
                                                      final UnregisteredHostChannelRegistry registry, final DomainController domainController) {
        this.domainController = domainController;
        this.controller = controller;
        this.registry = registry;
        this.handler = handler;
    }

    @Override
    public ManagementRequestHandler<?, ?> resolveHandler(RequestHandlerChain handlers, ManagementRequestHeader header) {
        final byte operationId = header.getOperationId();
        switch (operationId) {
            case DomainControllerProtocol.REGISTER_HOST_CONTROLLER_REQUEST: {
                handlers.registerActiveOperation(header.getBatchId(), null);
                return new RegisterOperation();
            } case DomainControllerProtocol.UNREGISTER_HOST_CONTROLLER_REQUEST: {
                handlers.registerActiveOperation(header.getBatchId(), null);
                return new UnregisterOperation();
            } case DomainControllerProtocol.GET_FILE_REQUEST: {
                handlers.registerActiveOperation(header.getBatchId(), null);
                return new GetFileOperation();
            }
        }
        return handlers.resolveNext();
    }

    private class RegisterOperation extends AbstractHostRequestHandler {
        String error;

        @Override
        void handleRequest(final String hostId, final DataInput input, final ManagementRequestContext<Void> context) throws IOException {
            context.executeAsync(new ManagementRequestContext.AsyncTask<Void>() {
                @Override
                public void execute(final ManagementRequestContext<Void> context) throws Exception {
                    try {

                        registry.registerChannel(hostId, handler, new ProxyCreatedCallback() {
                            @Override
                            public void proxyCreated(final ManagementRequestHandlerFactory handlerFactory) {
                                proxyHandler = handlerFactory;
                                handler.addHandlerFactory(handlerFactory);
                            }
                        });

                        final ModelNode op = new ModelNode();
                        op.get(OP).set(ReadMasterDomainModelHandler.OPERATION_NAME);
                        op.get(OP_ADDR).setEmptyList();
                        op.get(HOST).set(hostId);
                        final ModelNode result = MasterDomainControllerOperationHandlerImpl.this.controller.execute(op, OperationMessageHandler.logging, OperationTransactionControl.COMMIT, null);
                        if (result.hasDefined(FAILURE_DESCRIPTION)) {
                            error = SlaveRegistrationException.forUnknownError(result.get(FAILURE_DESCRIPTION).asString()).marshal();
                        }
                    } catch (SlaveRegistrationException e) {
                        error = e.marshal();
                    } catch (Exception e) {
                        error = SlaveRegistrationException.forUnknownError(e.getMessage()).marshal();
                    }
                    final FlushableDataOutput output = writeGenericResponseHeader(context);
                    try {
                        if (error != null) {
                            output.write(DomainControllerProtocol.PARAM_ERROR);
                            output.writeUTF(error);
                        } else {
                            output.write(DomainControllerProtocol.PARAM_OK);
                        }
                        output.close();
                    } finally {
                        StreamUtils.safeClose(output);
                    }
                }
            });
        }
    }

    private class UnregisterOperation extends AbstractHostRequestHandler {

        @Override
        void handleRequest(String hostId, DataInput input, ManagementRequestContext<Void> context) throws IOException {
            domainController.unregisterRemoteHost(hostId);
            handler.removeHandlerFactory(proxyHandler);
            proxyHandler = null;
            final FlushableDataOutput os = writeGenericResponseHeader(context);
            try {
                os.write(ManagementProtocol.RESPONSE_END);
                os.close();
            } finally {
                StreamUtils.safeClose(os);
            }
        }

    }

    private class GetFileOperation extends AbstractHostRequestHandler {

        @Override
        void handleRequest(String hostId, DataInput input, ManagementRequestContext<Void> context) throws IOException {
            final RootFileReader reader = new RootFileReader() {
                public File readRootFile(byte rootId, String filePath) throws RequestProcessingException {
                    final HostFileRepository localFileRepository = domainController.getLocalFileRepository();

                    switch (rootId) {
                        case DomainControllerProtocol.PARAM_ROOT_ID_FILE: {
                            return localFileRepository.getFile(filePath);
                        }
                        case DomainControllerProtocol.PARAM_ROOT_ID_CONFIGURATION: {
                            return localFileRepository.getConfigurationFile(filePath);
                        }
                        case DomainControllerProtocol.PARAM_ROOT_ID_DEPLOYMENT: {
                            byte[] hash = HashUtil.hexStringToByteArray(filePath);
                            return localFileRepository.getDeploymentRoot(hash);
                        }
                        default: {
                            throw MESSAGES.invalidRootId(rootId);
                        }
                    }
                }
            };
            DomainRemoteFileRequestAndHandler.INSTANCE.handleRequest(input, reader, context);
        }
    }

    abstract static class AbstractHostRequestHandler implements ManagementRequestHandler<ModelNode, Void> {

        abstract void handleRequest(final String hostId, DataInput input, ManagementRequestContext<Void> context) throws IOException;

        @Override
        public void handleRequest(DataInput input, ActiveOperation.ResultHandler<ModelNode> resultHandler, ManagementRequestContext<Void> context) throws IOException {
            expectHeader(input, DomainControllerProtocol.PARAM_HOST_ID);
            final String hostId = input.readUTF();
            handleRequest(hostId, input, context);
            resultHandler.done(null);
        }

        protected FlushableDataOutput writeGenericResponseHeader(final ManagementRequestContext<Void> context) throws IOException {
            final ManagementResponseHeader header = ManagementResponseHeader.create(context.getRequestHeader());
            return context.writeMessage(header);
        }

    }

}
