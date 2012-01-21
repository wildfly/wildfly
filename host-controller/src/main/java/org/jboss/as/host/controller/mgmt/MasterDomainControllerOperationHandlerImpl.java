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
import java.util.concurrent.ExecutorService;

import org.jboss.as.controller.HashUtil;
import org.jboss.as.controller.ModelController;
import org.jboss.as.controller.ModelController.OperationTransactionControl;
import org.jboss.as.controller.client.OperationMessageHandler;
import org.jboss.as.controller.remote.ModelControllerClientOperationHandler;
import org.jboss.as.domain.controller.DomainController;
import org.jboss.as.domain.controller.SlaveRegistrationException;
import org.jboss.as.domain.controller.UnregisteredHostChannelRegistry;
import org.jboss.as.domain.controller.UnregisteredHostChannelRegistry.ProxyCreatedCallback;
import org.jboss.as.domain.controller.operations.ReadMasterDomainModelHandler;
import org.jboss.as.protocol.StreamUtils;
import org.jboss.as.protocol.mgmt.ActiveOperation;
import org.jboss.as.protocol.mgmt.FlushableDataOutput;
import org.jboss.as.protocol.mgmt.ManagementChannelReceiver;
import org.jboss.as.protocol.mgmt.ManagementMessageHandler;
import org.jboss.as.protocol.mgmt.ManagementProtocol;
import org.jboss.as.protocol.mgmt.ManagementProtocolHeader;
import org.jboss.as.protocol.mgmt.ManagementRequestContext;
import org.jboss.as.protocol.mgmt.ManagementRequestHandler;
import org.jboss.as.protocol.mgmt.ManagementRequestHeader;
import org.jboss.as.protocol.mgmt.ManagementResponseHeader;
import org.jboss.as.protocol.mgmt.RequestProcessingException;
import org.jboss.as.repository.HostFileRepository;
import org.jboss.as.repository.RemoteFileRequestAndHandler.RootFileReader;
import org.jboss.dmr.ModelNode;
import org.jboss.remoting3.Channel;
import org.jboss.remoting3.CloseHandler;

/**
 * Handles for requests from slave DC to master DC on the 'domain' channel.
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 * @version $Revision: 1.1 $
 */
public class MasterDomainControllerOperationHandlerImpl extends ManagementChannelReceiver {

    private final LocalOperationHandler clientHandler;
    private final ModelController controller;
    private final DomainController domainController;
    private final UnregisteredHostChannelRegistry registry;

    private volatile ManagementMessageHandler proxyHandler;

    public MasterDomainControllerOperationHandlerImpl(final ExecutorService executorService, final ModelController controller,
                                                      final UnregisteredHostChannelRegistry registry, final DomainController domainController) {
        this.domainController = domainController;
        this.controller = controller;
        this.registry = registry;
        this.clientHandler = new LocalOperationHandler(controller, executorService);
    }

    @Override
    public void handleMessage(final Channel channel, final DataInput input, final ManagementProtocolHeader header) throws IOException {
        final byte type = header.getType();
        if(type == ManagementProtocol.TYPE_REQUEST) {
            final ManagementRequestHeader request = (ManagementRequestHeader) header;
            final byte id = request.getOperationId();

            ManagementRequestHandler<ModelNode, Void> handler = clientHandler.getRequestHandler(id);
            if (handler != null) {
                clientHandler.handleMessage(channel, input, header);
            }
            switch(id) {
                case DomainControllerProtocol.REGISTER_HOST_CONTROLLER_REQUEST:
                    handler = new RegisterOperation();
                    break;
                case DomainControllerProtocol.UNREGISTER_HOST_CONTROLLER_REQUEST:
                    handler = new UnregisterOperation();
                    break;
                case DomainControllerProtocol.GET_FILE_REQUEST:
                    handler =  new GetFileOperation();
                    break;
            }
            if(handler != null) {
                clientHandler.runLocalRequestHandler(channel, input, request, handler);
            } else if (proxyHandler != null) {
                // Delegate to the proxy
                proxyHandler.handleMessage(channel, input, header);
            }

        }
    }

    private class RegisterOperation extends AbstractHostRequestHandler {
        String error;

        @Override
        void handleRequest(final String hostId, final DataInput input, final ManagementRequestContext<Void> context) throws IOException {
            context.executeAsync(new ManagementRequestContext.AsyncTask<Void>() {
                @Override
                public void execute(final ManagementRequestContext<Void> context) throws Exception {
                    try {
                        final Channel mgmtChannel = context.getChannel();
                        registry.registerChannel(hostId, mgmtChannel, new ProxyCreatedCallback() {
                            @Override
                            public void proxyCreated(final ManagementMessageHandler handler) {
                                proxyHandler = handler;
                                mgmtChannel.addCloseHandler(new CloseHandler<Channel>() {
                                    @Override
                                    public void handleClose(Channel closed, IOException exception) {
                                        handler.shutdownNow();
                                    }
                                });
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

    static class LocalOperationHandler extends ModelControllerClientOperationHandler {

        LocalOperationHandler(ModelController controller, ExecutorService executorService) {
            super(controller, executorService);
        }

        protected ManagementRequestHandler<ModelNode, Void> getRequestHandler(byte operationType) {
            return super.getRequestHandler(operationType);
        }

        @Override
        protected ManagementRequestHandler<ModelNode, Void> getFallbackHandler() {
            // Return null here
            return null;
        }

       void runLocalRequestHandler(final Channel channel, final DataInput input, final ManagementRequestHeader header, final ManagementRequestHandler<ModelNode, Void> handler) {
           final ActiveOperation<ModelNode, Void> support = super.registerActiveOperation(header.getBatchId(), null);
           super.handleMessage(channel, input, header, support, handler);
       }

    }
}
