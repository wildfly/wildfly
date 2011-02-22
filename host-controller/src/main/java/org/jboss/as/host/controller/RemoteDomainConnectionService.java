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

package org.jboss.as.host.controller;

import static org.jboss.as.protocol.ProtocolUtils.expectHeader;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.concurrent.CancellationException;
import java.util.concurrent.Executors;

import javax.net.SocketFactory;

import org.jboss.as.controller.HashUtil;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationResult;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.ProxyController;
import org.jboss.as.controller.ResultHandler;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.controller.remote.ModelControllerOperationHandler;
import org.jboss.as.controller.remote.RemoteProxyController;
import org.jboss.as.domain.controller.DomainControllerSlave;
import org.jboss.as.domain.controller.FileRepository;
import org.jboss.as.domain.controller.MasterDomainControllerClient;
import org.jboss.as.host.controller.mgmt.DomainControllerProtocol;
import org.jboss.as.protocol.ByteDataInput;
import org.jboss.as.protocol.ByteDataOutput;
import org.jboss.as.protocol.Connection;
import org.jboss.as.protocol.MessageHandler;
import org.jboss.as.protocol.ProtocolClient;
import org.jboss.as.protocol.SimpleByteDataInput;
import org.jboss.as.protocol.SimpleByteDataOutput;
import org.jboss.as.protocol.StreamUtils;
import org.jboss.as.protocol.mgmt.ManagementHeaderMessageHandler;
import org.jboss.as.protocol.mgmt.ManagementRequest;
import org.jboss.as.protocol.mgmt.ManagementRequestConnectionStrategy;
import org.jboss.dmr.ModelNode;
import org.jboss.logging.Logger;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;

/**
 * @author Kabir Khan
 */
class RemoteDomainConnectionService implements MasterDomainControllerClient, Service<MasterDomainControllerClient> {

    private static final Logger log = Logger.getLogger("org.jboss.as.domain.controller");
    private static final int CONNECTION_TIMEOUT = 5000;
    private final InetAddress host;
    private final int port;
    private final String name;
    private final RemoteFileRepository remoteFileRepository;

    private volatile Connection connection;
    private volatile ProxyController client;
    private volatile ModelControllerOperationHandler operationHandler;

    RemoteDomainConnectionService(final String name, final InetAddress host, final int port, final FileRepository localRepository){
        this.name = name;
        this.host = host;
        this.port = port;
        this.remoteFileRepository = new RemoteFileRepository(localRepository);
    }

    /** {@inheritDoc} */
    @Override
    public synchronized void register(final String hostName, final DomainControllerSlave slave) {
        final ProtocolClient.Configuration config = new ProtocolClient.Configuration();
        config.setMessageHandler(initialMessageHandler);
        config.setConnectTimeout(CONNECTION_TIMEOUT);
        config.setReadExecutor(Executors.newCachedThreadPool()); //TODO inject
        config.setSocketFactory(SocketFactory.getDefault());
        config.setServerAddress(new InetSocketAddress(host, port));
        config.setThreadFactory(Executors.defaultThreadFactory()); //TODO inject

        final ProtocolClient protocolClient = new ProtocolClient(config);

        try {
            connection = protocolClient.connect();
            client = RemoteProxyController.create(ModelControllerClient.Type.DOMAIN, connection, PathAddress.EMPTY_ADDRESS);
            operationHandler = ModelControllerOperationHandler.Factory.create(ModelControllerClient.Type.HOST, slave, initialMessageHandler);
        } catch (IOException e) {
            log.warnf("Could not connect to remote domain controller %s:%d", host.getHostAddress(), port);
            throw new IllegalStateException(e);
        }

        try {
            ModelNode node = new RegisterModelControllerRequest().executeForResult(new ManagementRequestConnectionStrategy.ExistingConnectionStrategy(connection));
            slave.setInitialDomainModel(node);
        } catch (Exception e) {
            log.warnf("Error retrieving domain model from remote domain controller %s:%d: %s", host.getHostAddress(), port, e.getMessage());
            throw new IllegalStateException(e);
        }
    }

    /** {@inheritDoc} */
    @Override
    public synchronized void unregister() {
        try {
            new UnregisterModelControllerRequest().executeForResult(new ManagementRequestConnectionStrategy.ExistingConnectionStrategy(connection));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /** {@inheritDoc} */
    @Override
    public synchronized FileRepository getRemoteFileRepository() {
        return remoteFileRepository;
    }

    @Override
    public OperationResult execute(ModelNode operation, ResultHandler handler) {
        return client.execute(operation, handler);
    }

    @Override
    public ModelNode execute(ModelNode operation) throws CancellationException, OperationFailedException {
        return client.execute(operation);
    }

    /** {@inheritDoc} */
    @Override
    public synchronized void start(StartContext context) throws StartException {
    }

    /** {@inheritDoc} */
    @Override
    public synchronized void stop(StopContext context) {
        StreamUtils.safeClose(connection);
    }

    /** {@inheritDoc} */
    @Override
    public synchronized MasterDomainControllerClient getValue() throws IllegalStateException, IllegalArgumentException {
        return this;
    }

    private abstract class RegistryRequest<T> extends ManagementRequest<T> {

        @Override
        protected byte getHandlerId() {
            return ModelControllerClient.Type.DOMAIN.getHandlerId();
        }
    }

    private class RegisterModelControllerRequest extends RegistryRequest<ModelNode> {
        @Override
        protected byte getRequestCode() {
            return DomainControllerProtocol.REGISTER_HOST_CONTROLLER_REQUEST;
        }

        @Override
        protected byte getResponseCode() {
            return DomainControllerProtocol.REGISTER_HOST_CONTROLLER_RESPONSE;
        }

        /** {@inheritDoc} */
        @Override
        protected void sendRequest(final int protocolVersion, final OutputStream output) throws IOException {
            output.write(DomainControllerProtocol.PARAM_HOST_ID);
            StreamUtils.writeUTFZBytes(output, name);
        }

        /** {@inheritDoc} */
        @Override
        protected ModelNode receiveResponse(InputStream input) throws IOException {
            expectHeader(input, DomainControllerProtocol.PARAM_MODEL);
            ModelNode node = new ModelNode();
            node.readExternal(input);
            return node;
        }
    }

    private class UnregisterModelControllerRequest extends RegistryRequest<Void> {
        @Override
        protected byte getRequestCode() {
            return DomainControllerProtocol.UNREGISTER_HOST_CONTROLLER_REQUEST;
        }

        @Override
        protected byte getResponseCode() {
            return DomainControllerProtocol.UNREGISTER_HOST_CONTROLLER_RESPONSE;
        }

        /** {@inheritDoc} */
        @Override
        protected void sendRequest(final int protocolVersion, final OutputStream output) throws IOException {
            output.write(DomainControllerProtocol.PARAM_HOST_ID);
            StreamUtils.writeUTFZBytes(output, name);
        }
    }

    private class GetFileRequest extends RegistryRequest<File> {
        private final byte rootId;
        private final String filePath;
        private final FileRepository localFileRepository;

        private GetFileRequest(final byte rootId, final String filePath, final FileRepository localFileRepository) {
            this.rootId = rootId;
            this.filePath = filePath;
            this.localFileRepository = localFileRepository;
        }

        @Override
        public final byte getRequestCode() {
            return DomainControllerProtocol.GET_FILE_REQUEST;
        }

        @Override
        protected final byte getResponseCode() {
            return DomainControllerProtocol.GET_FILE_RESPONSE;
        }

        @Override
        protected final void sendRequest(final int protocolVersion, final OutputStream outputStream) throws IOException {
            super.sendRequest(protocolVersion, outputStream);
            log.debugf("Requesting files for path %s", filePath);
            ByteDataOutput output = null;
            try {
                output = new SimpleByteDataOutput(outputStream);
                output.writeByte(DomainControllerProtocol.PARAM_ROOT_ID);
                output.writeByte(rootId);
                output.writeByte(DomainControllerProtocol.PARAM_FILE_PATH);
                output.writeUTF(filePath);
                output.close();
            } finally {
                StreamUtils.safeClose(output);
            }
        }

        @Override
        protected final File receiveResponse(final InputStream inputStream) throws IOException {
            final File localPath;
            switch (rootId) {
                case DomainControllerProtocol.PARAM_ROOT_ID_FILE: {
                    localPath = localFileRepository.getFile(filePath);
                    break;
                }
                case DomainControllerProtocol.PARAM_ROOT_ID_CONFIGURATION: {
                    localPath = localFileRepository.getConfigurationFile(filePath);
                    break;
                }
                case DomainControllerProtocol.PARAM_ROOT_ID_DEPLOYMENT: {
                    byte[] hash = HashUtil.hexStringToByteArray(filePath);
                    localPath = localFileRepository.getDeploymentRoot(hash);
                    break;
                }
                default: {
                    localPath = null;
                }
            }
            ByteDataInput input = null;
            try {
                input = new SimpleByteDataInput(inputStream);
                expectHeader(input, DomainControllerProtocol.PARAM_NUM_FILES);
                int numFiles = input.readInt();
                log.debugf("Received %d files for %s", numFiles, localPath);
                switch (numFiles) {
                    case -1: { // Not found on DC
                        break;
                    }
                    case 0: { // Found on DC, but was an empty dir
                        if (!localPath.mkdirs()) {
                            throw new IOException("Unable to create local directory: " + localPath);
                        }
                        break;
                    }
                    default: { // Found on DC
                        for (int i = 0; i < numFiles; i++) {
                            expectHeader(input, DomainControllerProtocol.FILE_START);
                            expectHeader(input, DomainControllerProtocol.PARAM_FILE_PATH);
                            final String path = input.readUTF();
                            expectHeader(input, DomainControllerProtocol.PARAM_FILE_SIZE);
                            final long length = input.readLong();
                            log.debugf("Received file [%s] of length %d", path, length);
                            final File file = new File(localPath, path);
                            if (!file.getParentFile().exists() && !file.getParentFile().mkdirs()) {
                                throw new IOException("Unable to create local directory " + localPath.getParent());
                            }
                            long totalRead = 0;
                            OutputStream fileOut = null;
                            try {
                                fileOut = new BufferedOutputStream(new FileOutputStream(file));
                                final byte[] buffer = new byte[8192];
                                int read;
                                while (totalRead < length && (read = input.read(buffer, 0, Math.min((int) (length - totalRead), buffer.length))) != -1) {
                                    if (read > 0) {
                                        fileOut.write(buffer, 0, read);
                                        totalRead += read;
                                    }
                                }
                            } finally {
                                if (fileOut != null) {
                                    fileOut.close();
                                }
                            }
                            if (totalRead != length) {
                                throw new IOException("Did not read the entire file. Missing: " + (length - totalRead));
                            }

                            expectHeader(input, DomainControllerProtocol.FILE_END);
                        }
                    }
                }
                input.close();
            } finally {
                StreamUtils.safeClose(input);
            }
            return localPath;
        }
    }

    private class RemoteFileRepository implements FileRepository {
        private final FileRepository localFileRepository;

        private RemoteFileRepository(final FileRepository localFileRepository) {
            this.localFileRepository = localFileRepository;
        }

        @Override
        public final File getFile(String relativePath) {
            return getFile(relativePath, DomainControllerProtocol.PARAM_ROOT_ID_FILE);
        }

        @Override
        public final File getConfigurationFile(String relativePath) {
            return getFile(relativePath, DomainControllerProtocol.PARAM_ROOT_ID_CONFIGURATION);
        }

        @Override
        public final File[] getDeploymentFiles(byte[] deploymentHash) {
            String hex = HashUtil.bytesToHexString(deploymentHash);
            return getFile(hex, DomainControllerProtocol.PARAM_ROOT_ID_DEPLOYMENT).listFiles();
        }

        @Override
        public File getDeploymentRoot(byte[] deploymentHash) {
            String hex = HashUtil.bytesToHexString(deploymentHash);
            return getFile(hex, DomainControllerProtocol.PARAM_ROOT_ID_DEPLOYMENT);
        }

        private File getFile(final String relativePath, final byte repoId) {
            try {
                return new GetFileRequest(repoId, relativePath, localFileRepository).executeForResult(new ManagementRequestConnectionStrategy.ExistingConnectionStrategy(connection));
            } catch (Exception e) {
                throw new RuntimeException("Failed to get file from remote repository", e);
            }
        }
    }

    private final MessageHandler initialMessageHandler = new ManagementHeaderMessageHandler() {

        @Override
        public void handle(Connection connection, InputStream dataStream) throws IOException {
            super.handle(connection, dataStream);
        }

        @Override
        protected MessageHandler getHandlerForId(byte handlerId) {
            if (handlerId == ModelControllerClient.Type.HOST.getHandlerId()) {
                return operationHandler;
            }
            return null;
        }
    };

}
