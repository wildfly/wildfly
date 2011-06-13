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
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.remote.NewModelControllerClientOperationHandler;
import org.jboss.as.controller.remote.NewRemoteProxyController;
import org.jboss.as.domain.controller.FileRepository;
import org.jboss.as.domain.controller.NewDomainController;
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

    public NewMasterDomainControllerOperationHandlerImpl(final ExecutorService executorService, final NewModelController controller, final NewDomainController domainController) {
        super(executorService, controller);
        this.domainController = domainController;
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
            ModelNode node;
                final PathAddress addr = PathAddress.pathAddress(PathElement.pathElement(ModelDescriptionConstants.HOST, hostId));
                final NewRemoteProxyController proxy = NewRemoteProxyController.create(executorService, addr, getContext().getChannel());
                domainController.registerRemoteHost(proxy);

                //TODO Get domain model back to the remote slave
//
//                domainController.registerRemoteHost(RemoteProxyController.create(getContext().getChannel(), executorService, PathAddress.pathAddress(PathElement.pathElement(ModelDescriptionConstants.HOST, hostId)));
//                addClient(new RemoteDomainControllerSlaveClient(hostId, getContext().getChannel()));
//                node = getController().getDomainModel();
////            } catch (IllegalArgumentException e){
////                log.error(e);
////                node = new ModelNode();
////                node.get("protocol-error").set(e.getMessage());
////            }
//            output.write(DomainControllerProtocol.PARAM_MODEL);
//            node.writeExternal(output);
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
