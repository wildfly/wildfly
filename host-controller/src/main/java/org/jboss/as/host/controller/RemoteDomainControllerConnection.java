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

package org.jboss.as.host.controller;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.util.concurrent.ScheduledExecutorService;

import java.util.concurrent.ThreadFactory;
import org.jboss.as.domain.controller.FileRepository;
import org.jboss.as.domain.controller.mgmt.DomainControllerProtocol;
import org.jboss.as.model.DeploymentUnitElement;
import org.jboss.as.model.DomainModel;
import org.jboss.as.protocol.ByteDataInput;
import org.jboss.as.protocol.ByteDataOutput;
import org.jboss.as.protocol.ProtocolUtils;
import static org.jboss.as.protocol.ProtocolUtils.unmarshal;
import org.jboss.as.protocol.mgmt.ManagementException;
import org.jboss.as.protocol.mgmt.ManagementRequest;
import org.jboss.as.protocol.SimpleByteDataInput;
import org.jboss.as.protocol.SimpleByteDataOutput;
import org.jboss.as.protocol.StreamUtils;
import static org.jboss.as.protocol.StreamUtils.safeClose;
import static org.jboss.as.protocol.ProtocolUtils.expectHeader;
import org.jboss.as.protocol.mgmt.ManagementRequestConnectionStrategy;
import org.jboss.logging.Logger;
import org.jboss.marshalling.Marshalling;
import org.jboss.marshalling.Unmarshaller;

/**
 * Connection to a remote domain controller.
 *
 * @author John Bailey
 */
public class RemoteDomainControllerConnection implements DomainControllerConnection {
    private static final Logger log = Logger.getLogger("org.jboss.as.management");
    private final String hostControllerId;
    private final InetAddress dcAddress;
    private final int dcPort;
    private final long connectTimeout;
    private final InetAddress localManagementAddress;
    private final int localManagementPort;
    private final RemoteFileRepository remoteFileRepository;
    private final ScheduledExecutorService executorService;
    private final ThreadFactory threadFactory;

    /**
     * Create an instance.
     *
     * @param hostControllerId  The identifier of the host controller
     * @param dcAddress  The domain controller port
     * @param dcPort  The domain controller port
     * @param localManagementAddress The local management address
     * @param localManagementPort The local management port
     * @param localFileRepository  The local file repository
     * @param connectTimeout  The timeout for connecting to the remote DC (in seconds)
     * @param executorService The executor service
     * @param threadFactory The thread factory
     */
    public RemoteDomainControllerConnection(final String hostControllerId, final InetAddress dcAddress, final int dcPort, final InetAddress localManagementAddress, final int localManagementPort, final FileRepository localFileRepository, final long connectTimeout, final ScheduledExecutorService executorService, final ThreadFactory threadFactory) {
        this.hostControllerId = hostControllerId;
        this.dcAddress = dcAddress;
        this.dcPort = dcPort;
        this.localManagementAddress = localManagementAddress;
        this.localManagementPort = localManagementPort;
        this.remoteFileRepository = new RemoteFileRepository(this, localFileRepository);
        this.connectTimeout = connectTimeout;
        this.executorService = executorService;
        this.threadFactory = threadFactory;
    }

    /** {@inheritDoc} */
    public DomainModel register() {
        try {
            return new RegisterOperation(localManagementAddress, localManagementPort, this).executeForResult(getConnectionStrategy());
        } catch (Exception e) {
            throw new ManagementException("Failed to register with the domain controller", e);
        }
    }

    /** {@inheritDoc} */
    public void unregister() {
        try {
            new UnregisterOperation(this).execute(getConnectionStrategy());
        } catch (Exception e) {
            throw new ManagementException("Failed to register with the domain controller", e);
        }
    }

    /** {@inheritDoc} */
    public FileRepository getRemoteFileRepository() {
        return remoteFileRepository;
    }

    private abstract static class DomainControllerRequest<T> extends ManagementRequest<T> {
        protected final String hostControllerId;

        private DomainControllerRequest(final RemoteDomainControllerConnection connection) {
            this.hostControllerId = connection.hostControllerId;
        }

        @Override
        protected byte getHandlerId() {
            return DomainControllerProtocol.DOMAIN_CONTROLLER_REQUEST;
        }

        @Override
        protected void sendRequest(final int protocolVersion, final OutputStream output) throws IOException {
            output.write(DomainControllerProtocol.PARAM_HOST_CONTROLLER_ID);
            StreamUtils.writeUTFZBytes(output, hostControllerId);
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

        @Override
        public final byte getRequestCode() {
            return DomainControllerProtocol.REGISTER_REQUEST;
        }

        @Override
        protected final byte getResponseCode() {
            return DomainControllerProtocol.REGISTER_RESPONSE;
        }

        @Override
        protected void sendRequest(int protocolVersion, OutputStream outputStream) throws IOException {
            super.sendRequest(protocolVersion, outputStream);
            ByteDataOutput output = null;
            try {
                output = new SimpleByteDataOutput(outputStream);
                output.writeByte(DomainControllerProtocol.PARAM_HOST_CONTROLLER_HOST);
                final byte[] address = localManagementAddress.getAddress();
                output.writeInt(address.length);
                output.write(address);
                output.writeByte(DomainControllerProtocol.PARAM_HOST_CONTROLLER_PORT);
                output.writeInt(localManagementPort);
                output.close();
            } finally {
                safeClose(output);
            }
        }

        @Override
        protected final DomainModel receiveResponse(final InputStream input) throws IOException {
            final Unmarshaller unmarshaller = getUnmarshaller();
            unmarshaller.start(Marshalling.createByteInput(input));
            expectHeader(unmarshaller, DomainControllerProtocol.PARAM_DOMAIN_MODEL);
            log.infof("Registered with remote domain controller");
            final DomainModel domainModel = unmarshal(unmarshaller, DomainModel.class);
            unmarshaller.finish();
            return domainModel;
        }
    }

    private static class UnregisterOperation extends DomainControllerRequest<Void> {
        private UnregisterOperation(final RemoteDomainControllerConnection connection) {
            super(connection);
        }

        @Override
        public final byte getRequestCode() {
            return DomainControllerProtocol.UNREGISTER_REQUEST;
        }

        @Override
        protected final byte getResponseCode() {
            return DomainControllerProtocol.UNREGISTER_RESPONSE;
        }

        @Override
        protected Void receiveResponse(final InputStream input) throws IOException {
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

        @Override
        public final byte getRequestCode() {
            return DomainControllerProtocol.SYNC_FILE_REQUEST;
        }

        @Override
        protected final byte getResponseCode() {
            return DomainControllerProtocol.SYNC_FILE_RESPONSE;
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
                safeClose(output);
            }
        }

        @Override
        protected final File receiveResponse(final InputStream inputStream) throws IOException {
            final File localPath;
            switch (rootId) {
                case (byte)DomainControllerProtocol.PARAM_ROOT_ID_FILE: {
                    localPath = localFileRepository.getFile(filePath);
                    break;
                }
                case (byte)DomainControllerProtocol.PARAM_ROOT_ID_CONFIGURATION: {
                    localPath = localFileRepository.getConfigurationFile(filePath);
                    break;
                }
                case (byte)DomainControllerProtocol.PARAM_ROOT_ID_DEPLOYMENT: {
                    byte[] hash = DeploymentUnitElement.hexStringToBytes(filePath);
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
            } finally {
                safeClose(input);
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

        @Override
        public final File getFile(String relativePath) {


            return getFile(relativePath, (byte)DomainControllerProtocol.PARAM_ROOT_ID_FILE);
        }

        @Override
        public final File getConfigurationFile(String relativePath) {
            return getFile(relativePath, (byte)DomainControllerProtocol.PARAM_ROOT_ID_CONFIGURATION);
        }

        @Override
        public final File[] getDeploymentFiles(byte[] deploymentHash) {
            String hex = DeploymentUnitElement.bytesToHexString(deploymentHash);
            return getFile(hex, (byte)DomainControllerProtocol.PARAM_ROOT_ID_DEPLOYMENT).listFiles();
        }

        @Override
        public File getDeploymentRoot(byte[] deploymentHash) {
            String hex = DeploymentUnitElement.bytesToHexString(deploymentHash);
            return getFile(hex, (byte)DomainControllerProtocol.PARAM_ROOT_ID_DEPLOYMENT);
        }

        private File getFile(final String relativePath, final byte repoId) {
            try {
                return new GetFileOperation(repoId, relativePath, localFileRepository, connection).executeForResult(connection.getConnectionStrategy());
            } catch (Exception e) {
                throw new RuntimeException("Failed to get file from remote repository", e);
            }
        }
    }

    private static Unmarshaller getUnmarshaller() throws IOException {
        return ProtocolUtils.getUnmarshaller(ProtocolUtils.MODULAR_CONFIG);
    }

    private ManagementRequestConnectionStrategy getConnectionStrategy() {
        return new ManagementRequestConnectionStrategy.EstablishConnectingStrategy(dcAddress, dcPort, connectTimeout, executorService, threadFactory);
    }
}
