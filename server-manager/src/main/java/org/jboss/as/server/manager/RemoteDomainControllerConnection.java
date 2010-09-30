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
import java.util.concurrent.ScheduledExecutorService;
import org.jboss.as.model.DomainModel;
import org.jboss.as.server.manager.management.ByteDataInput;
import org.jboss.as.server.manager.management.ByteDataOutput;
import org.jboss.as.server.manager.management.ManagementException;
import org.jboss.as.server.manager.management.ManagementProtocol;
import static org.jboss.as.server.manager.management.ManagementUtils.expectHeader;
import static org.jboss.as.server.manager.management.ManagementUtils.unmarshal;
import org.jboss.as.server.manager.management.AbstractManagementRequest;
import org.jboss.logging.Logger;

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
    private final int connectionRetryLimit;
    private final long connectionRetryInterval;
    private final long connectTimeout;
    private final InetAddress localManagementAddress;
    private final int localManagementPort;
    private final RemoteFileRepository remoteFileRepository;
    private final ScheduledExecutorService executorService;

    /**
     * Create an instance.
     *
     * @param serverManagerId  The identifier of the server manager
     * @param dcAddress  The domain controller port
     * @param dcPort  The domain controller port
     * @param localManagementAddress The local management address
     * @param localManagementPort The local management port
     * @param localFileRepository  The local file repository
     * @param connectionRetryLimit  The number of times to retry operations
     * @param connectionRetryInterval  The interval between connection retries
     * @param connectTimeout  The timeout for connecting to the remote DC (in seconds)
     * @param executorService The executor service
     */
    public RemoteDomainControllerConnection(final String serverManagerId, final InetAddress dcAddress, final int dcPort, final InetAddress localManagementAddress, final int localManagementPort, final FileRepository localFileRepository, final int connectionRetryLimit, final long connectionRetryInterval, final long connectTimeout, final ScheduledExecutorService executorService) {
        this.serverManagerId = serverManagerId;
        this.dcAddress = dcAddress;
        this.dcPort = dcPort;
        this.localManagementAddress = localManagementAddress;
        this.localManagementPort = localManagementPort;
        this.remoteFileRepository = new RemoteFileRepository(this, localFileRepository);
        this.connectionRetryLimit = connectionRetryLimit;
        this.connectionRetryInterval = connectionRetryInterval;
        this.connectTimeout = connectTimeout;
        this.executorService = executorService;
    }

    /** {@inheritDoc} */
    public DomainModel register() {
        try {
            return new RegisterOperation(localManagementAddress, localManagementPort, this).executeForResult();
        } catch (ManagementException e) {
            throw new RuntimeException("Failed to register with the domain controller", e);
        }
    }

    /** {@inheritDoc} */
    public void unregister() {
        try {
            new UnregisterOperation(this).execute();
        } catch (ManagementException e) {
            throw new RuntimeException("Failed to register with the domain controller", e);
        }
    }

    /** {@inheritDoc} */
    public FileRepository getRemoteFileRepository() {
        return remoteFileRepository;
    }

    private abstract static class DomainControllerRequest<T> extends AbstractManagementRequest<T> {
        protected final String serverManagerId;

        private DomainControllerRequest(final RemoteDomainControllerConnection connection) {
            super(connection.dcAddress, connection.dcPort, connection.connectionRetryLimit, connection.connectionRetryInterval, connection.connectTimeout, connection.executorService);
            this.serverManagerId = connection.serverManagerId;
        }

        protected byte getHandlerId() {
            return ManagementProtocol.DOMAIN_CONTROLLER_REQUEST;
        }

        protected void sendRequest(int protocolVersion, ByteDataOutput output) throws ManagementException {
            super.sendRequest(protocolVersion, output);
            try {
                output.writeByte(ManagementProtocol.PARAM_SERVER_MANAGER_ID);
                output.writeUTF(serverManagerId);
            } catch (Exception e) {
                throw new ManagementException("Failed to write local management connection information in request", e);
            }
        }
    }

    private static class RegisterOperation extends DomainControllerRequest<DomainModel> {
        private final InetAddress localManagementAddress;
        private final int localManagementPort;

        private RegisterOperation(final InetAddress localManagementAddress, final int localManagementPort, final RemoteDomainControllerConnection connection) {
            super(connection);
            this.localManagementAddress = localManagementAddress;
            this.localManagementPort = localManagementPort;
        }

        public final byte getRequestCode() {
            return ManagementProtocol.REGISTER_REQUEST;
        }

        protected final byte getResponseCode() {
            return ManagementProtocol.REGISTER_RESPONSE;
        }

        protected void sendRequest(int protocolVersion, ByteDataOutput output) throws ManagementException {
            super.sendRequest(protocolVersion, output);
            try {
                output.writeByte(ManagementProtocol.PARAM_SERVER_MANAGER_HOST);
                final byte[] address = localManagementAddress.getAddress();
                output.writeInt(address.length);
                output.write(address);
                output.writeByte(ManagementProtocol.PARAM_SERVER_MANAGER_PORT);
                output.writeInt(localManagementPort);
            } catch (Exception e) {
                throw new ManagementException("Failed to write local management connection information in request", e);
            }
        }

        protected final DomainModel receiveResponse(final int protocolVersion, final ByteDataInput input) throws ManagementException {
            try {
                expectHeader(input, ManagementProtocol.PARAM_DOMAIN_MODEL);
                log.infof("Registered with remote domain controller");
                return unmarshal(input, DomainModel.class);
            } catch (Exception e) {
                throw new ManagementException("Failed to read domain model from response", e);
            }
        }
    }

    private static class UnregisterOperation extends DomainControllerRequest<Void> {
        private UnregisterOperation(final RemoteDomainControllerConnection connection) {
            super(connection);
        }

        public final byte getRequestCode() {
            return ManagementProtocol.UNREGISTER_REQUEST;
        }

        protected final byte getResponseCode() {
            return ManagementProtocol.UNREGISTER_RESPONSE;
        }

        protected Void receiveResponse(int protocolVersion, ByteDataInput input) throws ManagementException {
            log.infof("Unregistered with remote domain controller");
            return null;
        }
    }

    private static class GetFileOperation extends DomainControllerRequest<File> {
        private final byte rootId;
        private final String filePath;
        private final FileRepository localFileRepository;

        private GetFileOperation(final byte rootId, final String filePath, final FileRepository localFileRepository, final RemoteDomainControllerConnection connection) {
            super(connection);
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

        protected final void sendRequest(final int protocolVersion, final ByteDataOutput output) throws ManagementException {
            super.sendRequest(protocolVersion, output);
            try {
                output.writeByte(ManagementProtocol.PARAM_ROOT_ID);
                output.writeByte(rootId);
                output.writeByte(ManagementProtocol.PARAM_FILE_PATH);
                output.writeUTF(filePath);
            } catch (IOException e) {
                throw new ManagementException("Failed to send sync file request", e);
            }
        }

        protected final File receiveResponse(final int protocolVersion, final ByteDataInput input) throws ManagementException {
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
                            throw new ManagementException("Unable to create local directory: " + localPath);
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
                                throw new ManagementException("Unable to create local directory " + localPath.getParent());
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
                                throw new ManagementException("Did not read the entire file. Missing: " + (length - totalRead));
                            }
                            expectHeader(input, ManagementProtocol.FILE_END);
                        }
                    }
                }
            } catch (IOException e) {
                throw new ManagementException("Failed to process sync file response", e);
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
                return new GetFileOperation(repoId, relativePath, localFileRepository, connection).executeForResult();
            } catch (Exception e) {
                throw new RuntimeException("Failed to get file from remote repository", e);
            }
        }
    }


}
