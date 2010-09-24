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

package org.jboss.as.server.manager.management;

import java.io.DataOutput;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import org.jboss.as.domain.controller.DomainController;
import org.jboss.as.domain.controller.DomainControllerClient;
import org.jboss.as.domain.controller.RemoteDomainControllerClient;
import org.jboss.as.server.manager.FileRepository;
import static org.jboss.as.server.manager.management.ManagementProtocolUtils.expectHeader;
import static org.jboss.as.server.manager.management.MarshallingUtils.marshall;
import org.jboss.logging.Logger;
import org.jboss.marshalling.SimpleDataInput;
import org.jboss.marshalling.SimpleDataOutput;


/**
 * {@link org.jboss.as.server.manager.management.ManagementOperationHandler} implementation used to handle request
 * intended for the domain controller.
 *
 * @author John Bailey
 */
public class DomainControllerOperationHandler implements ManagementOperationHandler {
    private static final Logger log = Logger.getLogger("org.jboss.as.management");


    private final DomainController domainController;
    private final FileRepository localFileRepository;

    public DomainControllerOperationHandler(final DomainController domainController, final FileRepository localFileRepository) {
        this.domainController = domainController;
        this.localFileRepository = localFileRepository;
    }

    /** {@inheritDoc} */
    public final byte getIdentifier() {
        return ManagementProtocol.DOMAIN_CONTROLLER_REQUEST;
    }

    /**
     * Handles the request.  Starts by reading the server manager id and proceeds to read the requested command byte.
     * Once the command is available it will get the appropriate operation and execute it.
     *
     * @param input  The operation input
     * @param output The operation output
     * @throws ManagementOperationException If any problems occur performing the operation
     */
    public void handleRequest(final int protocolVersion, final SimpleDataInput input, final SimpleDataOutput output) throws ManagementOperationException {
        final String serverManagerId;
        try {
            expectHeader(input, ManagementProtocol.PARAM_SERVER_MANAGER_ID);
            serverManagerId = input.readUTF();
        } catch (IOException e) {
            throw new ManagementOperationException("ServerManager Request failed.  Unable to read signature", e);
        }
        final byte commandCode;
        try {
            expectHeader(input, ManagementProtocol.REQUEST_OPERATION);
            commandCode = input.readByte();
        } catch (IOException e) {
            throw new ManagementOperationException("ServerManager Request failed to read command code", e);
        }

        final Operation operation = operationFor(commandCode);
        if (operation == null) {
            throw new ManagementOperationException("Invalid command code " + commandCode + " received from server manager " + serverManagerId);
        }
        try {
            log.debugf("Received DomainController operation [%s] from ServerManager [%s]", operation, serverManagerId);
            operation.handle(serverManagerId, input, output);
        } catch (Exception e) {
            throw new ManagementOperationException("Failed to execute domain controller operation", e);
        }
    }

    private interface Operation {
        byte getRequestCode();

        void handle(final String serverManagerId, final SimpleDataInput input, final SimpleDataOutput output) throws ManagementOperationException;
    }

    private Operation operationFor(final byte commandByte) {
        switch (commandByte) {
            case ManagementProtocol.REGISTER_REQUEST:
                return new RegisterOperation();
            case ManagementProtocol.SYNC_FILE_REQUEST:
                return new GetFileOperation();
            case ManagementProtocol.UNREGISTER_REQUEST:
                return new UnregisterOperation();
            default: {
                return null;
            }
        }
    }

    private abstract class RequestResponseOperation implements Operation {
        public void handle(String serverManagerId, SimpleDataInput input, SimpleDataOutput output) throws ManagementOperationException {
            try {
                expectHeader(input, ManagementProtocol.REQUEST_START);
                readRequest(serverManagerId, input);
                expectHeader(input, ManagementProtocol.REQUEST_END);
            } catch (IOException e) {
                throw new ManagementOperationException("Domain controller request failed.  Unable to read request.", e);
            }
            try {
                output.writeByte(ManagementProtocol.RESPONSE_START);
                output.writeByte(getResponseCode());
                sendResponse(serverManagerId, output);
                output.writeByte(ManagementProtocol.RESPONSE_END);
                output.flush();
            } catch (IOException e) {
                throw new ManagementOperationException("Failed to send response", e);
            }
        }

        protected abstract byte getResponseCode();

        protected void readRequest(String serverManagerId, SimpleDataInput input) throws ManagementOperationException{
        }

        protected void sendResponse(String serverManagerId, SimpleDataOutput output) throws ManagementOperationException {
        }
    }

    private class RegisterOperation extends RequestResponseOperation {
        public final byte getRequestCode() {
            return ManagementProtocol.REGISTER_REQUEST;
        }

        protected byte getResponseCode() {
            return ManagementProtocol.REGISTER_RESPONSE;
        }

        protected void readRequest(final String serverManagerId, final SimpleDataInput input) throws ManagementOperationException {
            log.infof("Server manager registered [%s]", serverManagerId);
            final DomainControllerClient client = new RemoteDomainControllerClient(serverManagerId);
            domainController.addClient(client);
        }

        protected void sendResponse(final String serverManagerId, final SimpleDataOutput output) throws ManagementOperationException {
            try {
                output.writeByte(ManagementProtocol.PARAM_DOMAIN_MODEL);
                marshall(output, domainController.getDomainModel());
            } catch (Exception e) {
                throw new ManagementOperationException("Unable to write domain configuration to server manager", e);
            }
        }
    }

    private class UnregisterOperation extends RequestResponseOperation {
        public final byte getRequestCode() {
            return ManagementProtocol.UNREGISTER_REQUEST;
        }

        protected byte getResponseCode() {
            return ManagementProtocol.UNREGISTER_RESPONSE;
        }

        protected void readRequest(String serverManagerId, SimpleDataInput input) throws ManagementOperationException {
            log.infof("Server manager unregistered [%s]", serverManagerId);
            domainController.removeClient(serverManagerId);
        }
    }

    private class GetFileOperation extends RequestResponseOperation {
        private File localPath;

        public final byte getRequestCode() {
            return ManagementProtocol.SYNC_FILE_REQUEST;
        }

        protected byte getResponseCode() {
            return ManagementProtocol.SYNC_FILE_RESPONSE;
        }

        protected void readRequest(String serverManagerId, SimpleDataInput input) throws ManagementOperationException {
            final byte rootId;
            final String filePath;
            try {
                expectHeader(input, ManagementProtocol.PARAM_ROOT_ID);
                rootId = input.readByte();
                expectHeader(input, ManagementProtocol.PARAM_FILE_PATH);
                filePath = input.readUTF();
            } catch (Exception e) {
                throw new ManagementOperationException("Unable to read file request attributes", e);
            }

            log.infof("Server manager [%s] requested file [%s] from root [%d]", serverManagerId, filePath, rootId);
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
        }

        protected void sendResponse(String serverManagerId, SimpleDataOutput output) throws ManagementOperationException {
            try {
                output.writeByte(ManagementProtocol.PARAM_NUM_FILES);
                if (localPath == null || !localPath.exists()) {
                    output.writeInt(-1);
                } else if (localPath.isFile()) {
                    output.writeInt(1);
                    writeFile(localPath, output);
                } else {
                    final List<File> childFiles = getChildFiles(localPath);
                    output.writeInt(childFiles.size());
                    for (File child : childFiles) {
                        writeFile(child, output);
                    }
                }
                output.writeByte(getRequestCode());
            } catch (Exception e) {
                throw new ManagementOperationException("Unable to write response to server manager", e);
            }
        }

        private List<File> getChildFiles(final File base) {
            final List<File> childFiles = new ArrayList<File>();
            getChildFiles(base, childFiles);
            return childFiles;
        }

        private void getChildFiles(final File base, final List<File> childFiles) {
            for(File child : base.listFiles()) {
                if(child.isFile()) {
                    childFiles.add(child);
                } else {
                    getChildFiles(child, childFiles);
                }
            }
        }

        private String getRelativePath(final File parent, final File child) {
            return child.getAbsolutePath().substring((int)parent.getAbsolutePath().length());
        }

        private void writeFile(final File file, final DataOutput output) throws IOException {
            output.writeByte(ManagementProtocol.FILE_START);
            output.writeByte(ManagementProtocol.PARAM_FILE_PATH);
            output.writeUTF(getRelativePath(localPath, file));
            output.writeByte(ManagementProtocol.PARAM_FILE_SIZE);
            output.writeLong(file.length());
            InputStream inputStream = null;
            try {
                inputStream = new FileInputStream(file);
                byte[] buffer = new byte[8192];
                int len;
                while ((len = inputStream.read(buffer)) != -1) {
                    output.write(buffer, 0, len);
                }
            } finally {
                if(inputStream != null) {
                    try {
                        inputStream.close();
                    } catch(IOException ignored){}
                }
            }
            output.writeByte(ManagementProtocol.FILE_END);
        }
    }
}
