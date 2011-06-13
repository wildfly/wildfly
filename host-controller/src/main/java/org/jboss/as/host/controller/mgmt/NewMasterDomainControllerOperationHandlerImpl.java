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
import static org.jboss.as.protocol.old.ProtocolUtils.expectHeader;

import java.io.DataInput;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;

import org.jboss.as.controller.HashUtil;
import org.jboss.as.controller.NewModelController;
import org.jboss.as.controller.NewModelController.OperationTransactionControl;
import org.jboss.as.controller.client.OperationMessageHandler;
import org.jboss.as.controller.remote.NewModelControllerClientOperationHandler;
import org.jboss.as.domain.controller.FileRepository;
import org.jboss.as.domain.controller.NewDomainController;
import org.jboss.as.domain.controller.UnregisteredHostChannelRegistry;
import org.jboss.as.domain.controller.operations.ReadMasterDomainModelHandler;
import org.jboss.as.protocol.mgmt.FlushableDataOutput;
import org.jboss.as.protocol.mgmt.ManagementRequestHandler;
import org.jboss.dmr.ModelNode;

/**
 * Handles for requests from slave DC to master DC on the 'domain' channel.
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 * @version $Revision: 1.1 $
 */
public class NewMasterDomainControllerOperationHandlerImpl extends NewModelControllerClientOperationHandler {

    private final NewDomainController domainController;
    private final UnregisteredHostChannelRegistry registry;

    public NewMasterDomainControllerOperationHandlerImpl(final ExecutorService executorService, final NewModelController controller, final UnregisteredHostChannelRegistry registry, final NewDomainController domainController) {
        super(executorService, controller);
        this.domainController = domainController;
        this.registry = registry;
    }

    @Override
    public ManagementRequestHandler getRequestHandler(byte id) {
        switch (id) {
        case DomainControllerProtocol.REGISTER_HOST_CONTROLLER_REQUEST:
            return new RegisterOperation();
        case DomainControllerProtocol.UNREGISTER_HOST_CONTROLLER_REQUEST:
            return new UnregisterOperation();
        case DomainControllerProtocol.GET_FILE_REQUEST:
            return new GetFileOperation();
        }
        return super.getRequestHandler(id);
    }

    private abstract class RegistryOperation extends ManagementRequestHandler {
        String hostId;

        RegistryOperation() {
        }

        @Override
        protected void readRequest(final DataInput input) throws IOException {
            expectHeader(input, DomainControllerProtocol.PARAM_HOST_ID);
            hostId = input.readUTF();
        }
    }

    private class RegisterOperation extends RegistryOperation {

        @Override
        protected void readRequest(final DataInput input) throws IOException {
            expectHeader(input, DomainControllerProtocol.PARAM_HOST_ID);
            hostId = input.readUTF();
        }


        @Override
        protected void writeResponse(final FlushableDataOutput output) throws IOException {

            String error = null;
            try {
                registry.registerChannel(hostId, getContext().getChannel());

                ModelNode op = new ModelNode();
                op.get(OP).set(ReadMasterDomainModelHandler.OPERATION_NAME);
                op.get(OP_ADDR).setEmptyList();
                op.get(HOST).set(hostId);
                ModelNode result = controller.execute(op, OperationMessageHandler.logging, OperationTransactionControl.COMMIT, null);
                if (result.hasDefined(FAILURE_DESCRIPTION)) {
                    error = result.get(FAILURE_DESCRIPTION).asString();
                }
            } catch (Exception e) {
                error = e.getMessage();
            }

            if (error != null) {
                output.write(DomainControllerProtocol.PARAM_ERROR);
                output.writeUTF(error);
            } else {
                output.write(DomainControllerProtocol.PARAM_OK);
            }
        }
    }

    private class UnregisterOperation extends RegistryOperation {
        @Override
        protected void writeResponse(final FlushableDataOutput output) throws IOException {
            domainController.unregisterRemoteHost(hostId);
        }
    }

    private class GetFileOperation extends RegistryOperation {
        private File localPath;

        @Override
        protected void readRequest(final DataInput input) throws IOException {
            final byte rootId;
            final String filePath;
            final FileRepository localFileRepository = domainController.getFileRepository();
            expectHeader(input, DomainControllerProtocol.PARAM_ROOT_ID);
            rootId = input.readByte();
            expectHeader(input, DomainControllerProtocol.PARAM_FILE_PATH);
            filePath = input.readUTF();

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
                    throw new IOException(String.format("Invalid root id [%d]", rootId));
                }
            }
        }

        @Override
        protected void writeResponse(final FlushableDataOutput output) throws IOException {
            output.writeByte(DomainControllerProtocol.PARAM_NUM_FILES);
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
        }

        private List<File> getChildFiles(final File base) {
            final List<File> childFiles = new ArrayList<File>();
            getChildFiles(base, childFiles);
            return childFiles;
        }

        private void getChildFiles(final File base, final List<File> childFiles) {
            for (File child : base.listFiles()) {
                if (child.isFile()) {
                    childFiles.add(child);
                } else {
                    getChildFiles(child, childFiles);
                }
            }
        }

        private String getRelativePath(final File parent, final File child) {
            return child.getAbsolutePath().substring(parent.getAbsolutePath().length());
        }

        private void writeFile(final File file, final FlushableDataOutput output) throws IOException {
            output.writeByte(DomainControllerProtocol.FILE_START);
            output.writeByte(DomainControllerProtocol.PARAM_FILE_PATH);
            output.writeUTF(getRelativePath(localPath, file));
            output.writeByte(DomainControllerProtocol.PARAM_FILE_SIZE);
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
                if (inputStream != null) {
                    try {
                        inputStream.close();
                    } catch (IOException ignored) {
                    }
                }
            }
            output.writeByte(DomainControllerProtocol.FILE_END);
            output.flush();
        }
    }

}
