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

package org.jboss.as.server.mgmt.domain;

import org.jboss.as.controller.ControlledProcessState;
import org.jboss.as.controller.ModelController;
import org.jboss.as.controller.client.OperationAttachments;
import org.jboss.as.controller.client.OperationMessageHandler;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.network.NetworkUtils;
import org.jboss.as.protocol.mgmt.AbstractManagementRequest;
import org.jboss.as.protocol.mgmt.ActiveOperation;
import org.jboss.as.protocol.mgmt.FlushableDataOutput;
import org.jboss.as.protocol.mgmt.ManagementChannelHandler;
import org.jboss.as.protocol.mgmt.ManagementRequestContext;
import org.jboss.as.repository.RemoteFileRequestAndHandler;
import org.jboss.as.server.ServerLogger;
import org.jboss.as.server.ServerMessages;
import org.jboss.as.server.operations.ServerRestartRequiredHandler;
import org.jboss.dmr.ModelNode;

import java.io.Closeable;
import java.io.DataInput;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * Client used to interact with the local host controller.
 *
 * @author Emanuel Muckenhuber
 */
public class HostControllerClient implements Closeable {

    private final String serverName;
    private final HostControllerConnection connection;
    private final ManagementChannelHandler channelHandler;
    private final RemoteFileRepositoryExecutorImpl executor;
    private volatile ModelController controller;

    HostControllerClient(final String serverName, final ManagementChannelHandler channelHandler, final HostControllerConnection connection) {
        this.serverName = serverName;
        this.connection = connection;
        this.channelHandler = channelHandler;
        this.executor = new RemoteFileRepositoryExecutorImpl();
    }

    /**
     * Get the server name.
     *
     * @return the server name
     */
    public String getServerName() {
        return serverName;
    }

    /**
     * Resolve the boot updates and register at the local HC.
     *
     * @param controller the model controller
     * @param callback the completed callback
     * @throws Exception for any error
     */
    void resolveBootUpdates(final ModelController controller, final ActiveOperation.CompletedCallback<ModelNode> callback) throws Exception {
        connection.openConnection(controller, callback);
        // Keep a reference to the the controller
        this.controller = controller;
    }

    public void reconnect(final String hostName, final int port, final byte[] authKey) throws IOException, URISyntaxException {
        final String host = NetworkUtils.formatPossibleIpv6Address(hostName);
        final URI uri = new URI("remote://" + host + ":" + port);
        // In case the server is out of sync after the reconnect, set reload required
        if(! connection.reConnect(uri, authKey)) {
            // It would be nicer if we'd have direct access to the ControlledProcessState
            final ModelNode operation = new ModelNode();
            operation.get(ModelDescriptionConstants.OP).set(ServerRestartRequiredHandler.OPERATION_NAME); // TODO only require reload
            operation.get(ModelDescriptionConstants.OP_ADDR).setEmptyList();
            controller.execute(operation, OperationMessageHandler.DISCARD, ModelController.OperationTransactionControl.COMMIT, OperationAttachments.EMPTY);
        }
    }
    /**
     * Get the remote file repository.
     *
     * @return the remote file repository
     */
    RemoteFileRepositoryExecutor getRemoteFileRepository() {
        return executor;
    }

    @Override
    public void close() throws IOException {
        if(connection != null) {
            connection.close();
        }
    }

    private static class GetFileRequest extends AbstractManagementRequest<File, Void> {
        private final String hash;
        private final File localDeploymentFolder;

        private GetFileRequest(final String hash, final File localDeploymentFolder) {
            this.hash = hash;
            this.localDeploymentFolder = localDeploymentFolder;
        }

        @Override
        public byte getOperationType() {
            return DomainServerProtocol.GET_FILE_REQUEST;
        }

        @Override
        protected void sendRequest(ActiveOperation.ResultHandler<File> resultHandler, ManagementRequestContext<Void> context, FlushableDataOutput output) throws IOException {
            //The root id does not matter here
            ServerToHostRemoteFileRequestAndHandler.INSTANCE.sendRequest(output, (byte)0, hash);
        }

        @Override
        public void handleRequest(DataInput input, ActiveOperation.ResultHandler<File> resultHandler, ManagementRequestContext<Void> context) throws IOException {
            try {
                File first = new File(localDeploymentFolder, hash.substring(0,2));
                File localPath = new File(first, hash.substring(2));
                ServerToHostRemoteFileRequestAndHandler.INSTANCE.handleResponse(input, localPath, ServerLogger.ROOT_LOGGER, resultHandler, context);
                resultHandler.done(null);
            } catch (RemoteFileRequestAndHandler.CannotCreateLocalDirectoryException e) {
                resultHandler.failed(ServerMessages.MESSAGES.cannotCreateLocalDirectory(e.getDir()));
            } catch (RemoteFileRequestAndHandler.DidNotReadEntireFileException e) {
                resultHandler.failed(ServerMessages.MESSAGES.didNotReadEntireFile(e.getMissing()));
            }
        }
    }

    private class RemoteFileRepositoryExecutorImpl implements RemoteFileRepositoryExecutor {
        public File getFile(final String relativePath, final byte repoId, final File localDeploymentFolder) {
            try {
                return channelHandler.executeRequest(new GetFileRequest(relativePath, localDeploymentFolder), null).getResult().get();
            } catch (Exception e) {
                throw ServerMessages.MESSAGES.failedToGetFileFromRemoteRepository(e);
            }
        }
    }

}
