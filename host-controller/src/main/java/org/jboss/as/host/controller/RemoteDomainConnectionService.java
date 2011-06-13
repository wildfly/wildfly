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

import static org.jboss.as.protocol.old.ProtocolUtils.expectHeader;

import java.io.BufferedOutputStream;
import java.io.DataInput;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.URI;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

import org.jboss.as.controller.HashUtil;
import org.jboss.as.controller.ModelController;
import org.jboss.as.controller.OperationResult;
import org.jboss.as.controller.ResultHandler;
import org.jboss.as.controller.client.Operation;
import org.jboss.as.controller.remote.ModelControllerClientToModelControllerAdapter;
import org.jboss.as.controller.remote.TransactionalModelControllerOperationHandler;
import org.jboss.as.domain.controller.DomainController;
import org.jboss.as.domain.controller.DomainControllerSlave;
import org.jboss.as.domain.controller.FileRepository;
import org.jboss.as.domain.controller.MasterDomainControllerClient;
import org.jboss.as.host.controller.mgmt.DomainControllerProtocol;
import org.jboss.as.protocol.ProtocolChannelClient;
import org.jboss.as.protocol.mgmt.FlushableDataOutput;
import org.jboss.as.protocol.mgmt.ManagementChannel;
import org.jboss.as.protocol.mgmt.ManagementChannelFactory;
import org.jboss.as.protocol.mgmt.ManagementClientChannelStrategy;
import org.jboss.as.protocol.mgmt.ManagementRequest;
import org.jboss.as.protocol.mgmt.ManagementRequestHandler;
import org.jboss.as.protocol.old.Connection.ClosedCallback;
import org.jboss.dmr.ModelNode;
import org.jboss.logging.Logger;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;

/**
 * Establishes the connection from a slave {@link DomainController} to the master {@link DomainController}
 *
 * @author Kabir Khan
 */
public class RemoteDomainConnectionService implements MasterDomainControllerClient, Service<MasterDomainControllerClient>, ClosedCallback {

    private static final Logger log = Logger.getLogger("org.jboss.as.domain.controller");
    private final InetAddress host;
    private final int port;
    private final String name;
    private final RemoteFileRepository remoteFileRepository;

    private volatile ProtocolChannelClient<ManagementChannel> channelClient;
    /** Used to invoke ModelController ops on the master */
    private volatile ModelController masterProxy;
    /** Handler for transactional operations */
    private volatile TransactionalModelControllerOperationHandler txOperationHandler;
    private final AtomicBoolean shutdown = new AtomicBoolean();
    private volatile ManagementChannel channel;
    private volatile ReconnectInfo reconnectInfo;
    private final ExecutorService executor = Executors.newCachedThreadPool();

    public RemoteDomainConnectionService(final String name, final InetAddress host, final int port, final FileRepository localRepository){
        this.name = name;
        this.host = host;
        this.port = port;
        this.remoteFileRepository = new RemoteFileRepository(localRepository);
    }

    /** {@inheritDoc} */
    @Override
    public void register(final String hostName, final InetAddress ourAddress, final int ourPort, final DomainControllerSlave slave) {
        // TODO egregious hack. Fix properly as part of AS7-794
        IllegalStateException ise = null;
        boolean connected = false;
        long timeout = System.currentTimeMillis() + 5000;
        while (!connected && System.currentTimeMillis() < timeout) {
            try {
               connect(hostName, ourAddress, ourPort, slave);
               connected = true;
            }
            catch (IllegalStateException e) {
                ise = e;
                try {
                    Thread.sleep(100);
                } catch (InterruptedException inter) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException("Interrupted while trying to connect to master", inter);
                }
            }
        }

        if (!connected) {
            throw (ise != null) ? ise : new IllegalStateException("Could not connect to master within 5000 ms");
        }

        reconnectInfo = new ReconnectInfo(hostName, ourAddress, ourPort, slave);
    }

    private synchronized void connect(final String hostName, final InetAddress ourAddress, final int ourPort, final DomainControllerSlave slave) {
        txOperationHandler = new SlaveDomainControllerOperationHandler(slave);
        ProtocolChannelClient<ManagementChannel> client;
        try {
            ProtocolChannelClient.Configuration<ManagementChannel> configuration = new ProtocolChannelClient.Configuration<ManagementChannel>();
            configuration.setEndpointName("endpoint");
            configuration.setUriScheme("remote");
            configuration.setUri(new URI("remote://" + host.getHostAddress() + ":" + port));
            configuration.setExecutor(executor);
            configuration.setChannelFactory(new ManagementChannelFactory(txOperationHandler));
            client = ProtocolChannelClient.create(configuration);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        try {
            client.connect();
            this.channelClient = client;

            if (reconnectInfo != null) {
                unregister();
            }

            //TODO rename to management
            ManagementChannel channel = client.openChannel("domain");
            this.channel = channel;
            channel.startReceiving();

            masterProxy = new ModelControllerClientToModelControllerAdapter(channel, executor);
        } catch (IOException e) {
            log.warnf("Could not connect to remote domain controller %s:%d", host.getHostAddress(), port);
            //TODO remove this line
            e.printStackTrace();
            throw new IllegalStateException(e);
        }

        try {
            ModelNode node = new RegisterModelControllerRequest().executeForResult(executor, ManagementClientChannelStrategy.create(channel));
            if (reconnectInfo == null) {
                //TODO update the domain model from the reconnected host
                slave.setInitialDomainModel(node);
            }
        } catch (Exception e) {
            log.warnf("Error retrieving domain model from remote domain controller %s:%d: %s", host.getHostAddress(), port, e.getMessage());
            throw new IllegalStateException(e);
        }
    }

    /** {@inheritDoc} */
    @Override
    public synchronized void unregister() {
        try {
            new UnregisterModelControllerRequest().executeForResult(executor, ManagementClientChannelStrategy.create(channel));
        } catch (Exception e) {
            log.errorf(e, "Error unregistering from master");
        }
        finally {
            channel.stopReceiving();
            channelClient.close();
        }
    }

    /** {@inheritDoc} */
    @Override
    public synchronized FileRepository getRemoteFileRepository() {
        return remoteFileRepository;
    }

    @Override
    public OperationResult execute(Operation operation, ResultHandler handler) {
        return masterProxy.execute(operation, handler);
    }

    @Override
    public ModelNode execute(Operation operation) throws CancellationException {
        return masterProxy.execute(operation);
    }

    /** {@inheritDoc} */
    @Override
    public synchronized void start(StartContext context) throws StartException {
    }

    /** {@inheritDoc} */
    @Override
    public synchronized void stop(StopContext context) {
        shutdown.set(true);
        channelClient.close();
    }

    /** {@inheritDoc} */
    @Override
    public synchronized MasterDomainControllerClient getValue() throws IllegalStateException, IllegalArgumentException {
        return this;
    }

    private abstract class RegistryRequest<T> extends ManagementRequest<T>{

        protected T readResponse(DataInput input) throws IOException {
            return null;
        }
    }

    private class RegisterModelControllerRequest extends RegistryRequest<ModelNode> {

        RegisterModelControllerRequest() {
        }

        @Override
        protected byte getRequestCode() {
            return DomainControllerProtocol.REGISTER_HOST_CONTROLLER_REQUEST;
        }

        /** {@inheritDoc} */
        @Override
        protected void writeRequest(final int protocolVersion, final FlushableDataOutput output) throws IOException {
            output.write(DomainControllerProtocol.PARAM_HOST_ID);
            output.writeUTF(name);
        }

        /** {@inheritDoc} */
        @Override
        protected ModelNode readResponse(DataInput input) throws IOException {
            expectHeader(input, DomainControllerProtocol.PARAM_MODEL);
            ModelNode node = new ModelNode();
            node.readExternal(input);

            if (node.hasDefined("protocol-error")){
                log.error(node.get("protocol-error").asString());
                log.error("Exiting");
            }
            return node;
        }
    }

    private class UnregisterModelControllerRequest extends RegistryRequest<Void> {
        @Override
        protected byte getRequestCode() {
            return DomainControllerProtocol.UNREGISTER_HOST_CONTROLLER_REQUEST;
        }

        /** {@inheritDoc} */
        @Override
        protected void writeRequest(final int protocolVersion, final FlushableDataOutput output) throws IOException {
            output.write(DomainControllerProtocol.PARAM_HOST_ID);
            output.writeUTF(name);
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
        protected final void writeRequest(final int protocolVersion, final FlushableDataOutput output) throws IOException {
            super.writeRequest(protocolVersion, output);
            log.debugf("Requesting files for path %s", filePath);
            output.writeByte(DomainControllerProtocol.PARAM_ROOT_ID);
            output.writeByte(rootId);
            output.writeByte(DomainControllerProtocol.PARAM_FILE_PATH);
            output.writeUTF(filePath);
        }

        @Override
        protected final File readResponse(final DataInput input) throws IOException {
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
                            while (totalRead < length) {
                                int len = Math.min((int) (length - totalRead), buffer.length);
                                input.readFully(buffer, 0, len);
                                fileOut.write(buffer, 0, len);
                                totalRead += len;
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
            String hex = deploymentHash == null ? "" : HashUtil.bytesToHexString(deploymentHash);
            return getFile(hex, DomainControllerProtocol.PARAM_ROOT_ID_DEPLOYMENT).listFiles();
        }

        @Override
        public File getDeploymentRoot(byte[] deploymentHash) {
            String hex = deploymentHash == null ? "" : HashUtil.bytesToHexString(deploymentHash);
            return getFile(hex, DomainControllerProtocol.PARAM_ROOT_ID_DEPLOYMENT);
        }

        private File getFile(final String relativePath, final byte repoId) {
            try {
                return new GetFileRequest(repoId, relativePath, localFileRepository).executeForResult(executor, ManagementClientChannelStrategy.create(channel));
            } catch (Exception e) {
                throw new RuntimeException("Failed to get file from remote repository", e);
            }
        }
    }

    private class SlaveDomainControllerOperationHandler extends TransactionalModelControllerOperationHandler {

        SlaveDomainControllerOperationHandler(final DomainControllerSlave slave) {
            super(slave);
        }

        @Override
        public ManagementRequestHandler getRequestHandler(byte id) {

            if (id == DomainControllerProtocol.IS_ACTIVE_REQUEST) {
                return new IsActiveOperation();
            }
            else {
                return super.getRequestHandler(id);
            }
        }
    }

    private class IsActiveOperation extends ManagementRequestHandler {
        @Override
        protected void readRequest(DataInput input) throws IOException {
        }

        @Override
        protected void writeResponse(FlushableDataOutput output) throws IOException {
        }
    }

    @Override
    public void connectionClosed() {
        if (!shutdown.get()) {
            //The remote host went down, try reconnecting
            final ReconnectInfo reconnectInfo = this.reconnectInfo;
            if (reconnectInfo == null) {
                log.error("Null reconnect info, cannot try to reconnect");
                return;
            }
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        Thread.sleep(3000);
                    } catch (InterruptedException e) {
                    }

                    while (!shutdown.get()) {
                        log.debug("Attempting reconnection to master...");
                        try {
                            connect(reconnectInfo.getHostName(), reconnectInfo.getOurAddress(), reconnectInfo.getOurPort(), reconnectInfo.getSlave());
                            log.info("Connected to master");
                            break;
                        } catch (Exception e) {
                        }
                        try {
                            Thread.sleep(3000);
                        } catch (InterruptedException e) {
                        }
                    }
                }
            }).start();
        }
    }

    private static class ReconnectInfo {
        final String hostName;
        final InetAddress ourAddress;
        final int ourPort;
        final DomainControllerSlave slave;

        public ReconnectInfo(String hostName, InetAddress ourAddress, int ourPort, DomainControllerSlave slave) {
            super();
            this.hostName = hostName;
            this.ourAddress = ourAddress;
            this.ourPort = ourPort;
            this.slave = slave;
        }

        public String getHostName() {
            return hostName;
        }

        public InetAddress getOurAddress() {
            return ourAddress;
        }

        public int getOurPort() {
            return ourPort;
        }

        public DomainControllerSlave getSlave() {
            return slave;
        }


    }
}
