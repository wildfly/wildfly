/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
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

package org.jboss.as.server.manager;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.jboss.as.model.DomainModel;
import org.jboss.as.server.manager.management.ManagementOperationException;
import org.jboss.as.server.manager.management.ManagementProtocol;
import static org.jboss.as.server.manager.management.ManagementProtocolUtils.expectHeader;
import org.jboss.as.server.manager.management.ManagementRequestProtocolHeader;
import org.jboss.as.server.manager.management.ManagementResponseProtocolHeader;
import static org.jboss.as.server.manager.management.MarshallingUtils.unmarshall;
import org.jboss.logging.Logger;
import org.jboss.marshalling.Marshalling;
import org.jboss.marshalling.SimpleDataInput;
import org.jboss.marshalling.SimpleDataOutput;

/**
 * Connection to a remote domain controller.
 *
 * @author John Bailey
 */
public class RemoteDomainControllerConnection implements DomainControllerConnection {
    private static final Logger log = Logger.getLogger("org.jboss.as.management");
    private final String serverManagerId;
    private final InetAddress dcAddress;
    private final int dcPort;
    private final RemoteFileRepository remoteFileRepository;
    private final ScheduledExecutorService executorService = Executors.newScheduledThreadPool(10);
    private final int connectionRetryLimit;
    private final long connectionRetryInterval;
    private final long connectTimeout;

    /**
     * Create an instance.
     *
     * @param serverManagerId  The identifier of the server manager
     * @param dcAddress  The domain controller port
     * @param dcPort  The domain controller port
     * @param localFileRepository  The local file repository
     * @param connectionRetryLimit  The number of times to retry operations
     * @param connectionRetryInterval  The interval between connection retries
     * @param connectTimeout  The timeout for connecting to the remote DC (in seconds)
     */
    public RemoteDomainControllerConnection(final String serverManagerId, final InetAddress dcAddress, final int dcPort, final FileRepository localFileRepository, final int connectionRetryLimit, final long connectionRetryInterval, final long connectTimeout) {
        this.serverManagerId = serverManagerId;
        this.dcAddress = dcAddress;
        this.dcPort = dcPort;
        this.remoteFileRepository = new RemoteFileRepository(this, localFileRepository);
        this.connectionRetryLimit = connectionRetryLimit;
        this.connectionRetryInterval = connectionRetryInterval;
        this.connectTimeout = connectTimeout;
    }

    /**
     * {@inheritDoc}
     */
    public DomainModel register() {
        try {
            return execute(new RegisterOperation()).get();
        } catch (Exception e) {
            throw new RuntimeException("Failed to register with the domain controller", e);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void unregister() {
        execute(new UnregisterOperation());
    }

    /**
     * {@inheritDoc}
     */
    public FileRepository getRemoteFileRepository() {
        return remoteFileRepository;
    }

    private <T> Future<T> execute(final DomainControllerOperation<T> operation) {
        final InitiatingFuture<T> initiatingFuture = new InitiatingFuture<T>();
        executorService.execute(new InitiateRequestTask<T>(operation, initiatingFuture));
        try {
            return initiatingFuture.get();
        } catch (Exception e) {
            throw new RuntimeException("Failed to execute remote domain controller operation: " +  operation, e);
        }
    }

    private class InitiateRequestTask<T> implements Runnable {
        private final DomainControllerOperation<T> operation;
        private final InitiatingFuture<T> initiatingFuture;
        private int requestId;

        private InitiateRequestTask(final DomainControllerOperation<T> operation, final InitiatingFuture<T> initiatingFuture) {
            this.operation = operation;
            this.initiatingFuture = initiatingFuture;
        }

        public void run() {
            final int requestId = this.requestId++;
            Socket socket = null;
            try {
                socket = new Socket();
                int timeout = (int)TimeUnit.SECONDS.toMillis(connectTimeout);
                socket.connect(new InetSocketAddress(dcAddress, dcPort), timeout);
                socket.setSoTimeout(timeout);
                SimpleDataInput input = new SimpleDataInput(Marshalling.createByteInput(socket.getInputStream()));
                SimpleDataOutput output = new SimpleDataOutput(Marshalling.createByteOutput(socket.getOutputStream()));

                // Start by writing the header
                final ManagementRequestProtocolHeader managementRequestHeader = new ManagementRequestProtocolHeader(ManagementProtocol.VERSION, requestId, (byte)ManagementProtocol.DOMAIN_CONTROLLER_REQUEST);
                managementRequestHeader.write(output);
                output.flush();

                // Now read the response header
                final ManagementResponseProtocolHeader responseHeader = new ManagementResponseProtocolHeader(input);

                if (requestId != responseHeader.getResponseId()) {
                    // TODO: Exception???
                    safeClose(socket);
                    return;
                }
                // Schedule execution the operation
                Future<T> resultFuture = executorService.submit(new ExecuteTask<T>(operation, responseHeader.getVersion(), socket, input, output));
                initiatingFuture.set(resultFuture);
            } catch (Throwable e) {
                safeClose(socket);
                if(requestId < connectionRetryLimit) {
                    executorService.schedule(this, connectionRetryInterval, TimeUnit.SECONDS);
                } else {
                    initiatingFuture.setException(new Exception("Failed to initiate request to remote domain controller", e));
                }
            }
        }
    }

    private final class InitiatingFuture<T> {
        private volatile Future<T> operationFuture;
        private volatile Exception exception;

        Future<T> get() throws Exception {
            boolean intr = false;
            try {
                synchronized (this) {
                    while (this.operationFuture == null && exception == null) {
                        try {
                            wait();
                        } catch (InterruptedException e) {
                            intr = true;
                        }
                    }
                }
                if (exception != null) {
                    throw exception;
                }
                return operationFuture;
            } finally {
                if (intr) Thread.currentThread().interrupt();
            }
        }

        void set(final Future<T> operationFuture) {
            synchronized (this) {
                this.operationFuture = operationFuture;
                notifyAll();
            }
        }

        void setException(final Exception exception) {
            synchronized (this) {
                this.exception = exception;
                notifyAll();
            }
        }
    }

    private class ExecuteTask<T> implements Callable<T> {
        private final DomainControllerOperation<T> operation;
        private final int protocolVersion;
        private final Socket socket;
        private final SimpleDataInput input;
        private final SimpleDataOutput output;

        private ExecuteTask(final DomainControllerOperation<T> operation, final int protocolVersion, final Socket socket, final SimpleDataInput input, final SimpleDataOutput output) {
            this.operation = operation;
            this.protocolVersion = protocolVersion;
            this.socket = socket;
            this.input = input;
            this.output = output;
        }

        public T call() throws Exception {
            try {
                return operation.execute(protocolVersion, serverManagerId, output, input);
            } finally {
                safeClose(socket);
            }
        }
    }

    private static interface DomainControllerOperation<T> {
        byte getRequestCode();

        T execute(final int protocolVersion, final String serverManagerId, final SimpleDataOutput output, final SimpleDataInput input) throws ManagementOperationException;
    }

    private abstract static class RequestResponseOperation<T> implements DomainControllerOperation<T> {
        public final T execute(final int protocolVersion, final String serverManagerId, final SimpleDataOutput output, final SimpleDataInput input) throws ManagementOperationException {
            try {
                // First send request
                output.writeByte(ManagementProtocol.PARAM_SERVER_MANAGER_ID);
                output.writeUTF(serverManagerId);
                output.writeByte(ManagementProtocol.REQUEST_OPERATION);
                output.writeByte(getRequestCode());
                output.writeByte(ManagementProtocol.REQUEST_START);
                sendRequest(protocolVersion, output);
                output.writeByte(ManagementProtocol.REQUEST_END);
                output.flush();

                // Now process the response
                expectHeader(input, ManagementProtocol.RESPONSE_START);
                byte responseCode = input.readByte();
                if (responseCode != getResponseCode()) {
                    throw new ManagementOperationException("Invalid response code.  Expecting '" + getResponseCode() + "' received '" + responseCode + "'");
                }
                final T result = receiveResponse(protocolVersion, input);
                expectHeader(input, ManagementProtocol.RESPONSE_END);
                return result;
            } catch (IOException e) {
                throw new ManagementOperationException("Failed to execute remote domain controller operation", e);
            }
        }

        protected abstract byte getResponseCode();

        protected void sendRequest(final int protocolVersion, final SimpleDataOutput output) throws ManagementOperationException {
        }

        protected T receiveResponse(final int protocolVersion, final SimpleDataInput input) throws ManagementOperationException {
            return null;
        }
    }

    private static class RegisterOperation extends RequestResponseOperation<DomainModel> {
        public final byte getRequestCode() {
            return ManagementProtocol.REGISTER_REQUEST;
        }

        protected final byte getResponseCode() {
            return ManagementProtocol.REGISTER_RESPONSE;
        }

        protected final DomainModel receiveResponse(final int protocolVersion, final SimpleDataInput input) throws ManagementOperationException {
            try {
                expectHeader(input, ManagementProtocol.PARAM_DOMAIN_MODEL);
                log.infof("Registered with remote domain controller");
                return unmarshall(input, DomainModel.class);
            } catch (Exception e) {
                throw new ManagementOperationException("Failed to read domain model from response", e);
            }
        }
    }

    private static class UnregisterOperation extends RequestResponseOperation<Void> {
        public final byte getRequestCode() {
            return ManagementProtocol.UNREGISTER_REQUEST;
        }

        protected final byte getResponseCode() {
            return ManagementProtocol.UNREGISTER_RESPONSE;
        }

        protected Void receiveResponse(int protocolVersion, SimpleDataInput input) throws ManagementOperationException {
            log.infof("Unregistered with remote domain controller");
            return null;
        }
    }

    private static class GetFileOperation extends RequestResponseOperation<File> {
        private final byte rootId;
        private final String filePath;
        private final FileRepository localFileRepository;

        private GetFileOperation(byte rootId, String filePath, final FileRepository localFileRepository) {
            this.rootId = rootId;
            this.filePath = filePath;
            this.localFileRepository = localFileRepository;
        }

        public final byte getRequestCode() {
            return ManagementProtocol.SYNC_FILE_REQUEST;
        }

        protected final byte getResponseCode() {
            return ManagementProtocol.SYNC_FILE_RESPONSE;
        }

        protected final void sendRequest(final int protocolVersion, final SimpleDataOutput output) throws ManagementOperationException {
            try {
                output.writeByte(ManagementProtocol.PARAM_ROOT_ID);
                output.writeByte(rootId);
                output.writeByte(ManagementProtocol.PARAM_FILE_PATH);
                output.writeUTF(filePath);
            } catch (IOException e) {
                throw new ManagementOperationException("Failed to send sync file request", e);
            }
        }

        protected final File receiveResponse(final int protocolVersion, final SimpleDataInput input) throws ManagementOperationException {
            final File localPath;
            switch (rootId) {
                case 0: {
                    localPath = localFileRepository.getFile(filePath);
                    break;
                }
                case 1: {
                    localPath = localFileRepository.getConfigurationFile(filePath);
                    break;
                }
                case 2: {
                    localPath = localFileRepository.getDeploymentFile(filePath);
                    break;
                }
                default: {
                    localPath = null;
                }
            }
            try {
                expectHeader(input, ManagementProtocol.PARAM_NUM_FILES);
                int numFiles = input.readInt();
                switch (numFiles) {
                    case -1: { // Not found on DC
                        break;
                    }
                    case 0: { // Found on DC, but was an empty dir
                        if (!localPath.mkdirs()) {
                            throw new ManagementOperationException("Unable to create local directory: " + localPath);
                        }
                        break;
                    }
                    default: { // Found on DC
                        for (int i = 0; i < numFiles; i++) {
                            expectHeader(input, ManagementProtocol.FILE_START);
                            expectHeader(input, ManagementProtocol.PARAM_FILE_PATH);
                            final String path = input.readUTF();
                            expectHeader(input, ManagementProtocol.PARAM_FILE_SIZE);
                            final long length = input.readLong();
                            log.debugf("Received file [%s] of length %d", path, length);
                            final File file = new File(localPath, path);
                            if (!file.getParentFile().exists() && !file.getParentFile().mkdirs()) {
                                throw new ManagementOperationException("Unable to create local directory " + localPath.getParent());
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
                                throw new ManagementOperationException("Did not read the entire file. Missing: " + (length - totalRead));
                            }
                            expectHeader(input, ManagementProtocol.FILE_END);
                        }
                    }
                }
            } catch (IOException e) {
                throw new ManagementOperationException("Failed to process sync file response", e);
            }
            return localPath;
        }
    }

    private static class RemoteFileRepository implements FileRepository {
        private final RemoteDomainControllerConnection connection;
        private final FileRepository localFileRepository;

        private RemoteFileRepository(final RemoteDomainControllerConnection connection, final FileRepository localFileRepository) {
            this.connection = connection;
            this.localFileRepository = localFileRepository;
        }

        public final File getFile(String relativePath) {
            return getFile(relativePath, (byte)0);
        }

        public final File getConfigurationFile(String relativePath) {
            return getFile(relativePath, (byte)1);
        }

        public final File getDeploymentFile(String relativePath) {
            return getFile(relativePath, (byte)2);
        }

        private File getFile(final String relativePath, final byte repoId) {
            try {
                return connection.execute(new GetFileOperation(repoId, relativePath, localFileRepository)).get();
            } catch (Exception e) {
                throw new RuntimeException("Failed to get file from remote repository", e);
            }
        }
    }

    private void safeClose(final Socket socket) {
        if (socket == null)
            return;
        try {
            socket.shutdownOutput();
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            socket.shutdownInput();
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
