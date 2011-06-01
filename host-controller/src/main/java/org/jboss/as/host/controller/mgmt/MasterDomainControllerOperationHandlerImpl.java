/*
* JBoss, Home of Professional Open Source.
* Copyright 2006, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.as.host.controller.mgmt;

import static org.jboss.as.protocol.old.ProtocolUtils.expectHeader;

import java.io.DataInput;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.jboss.as.controller.HashUtil;
import org.jboss.as.controller.remote.ModelControllerOperationHandlerImpl;
import org.jboss.as.domain.controller.DomainController;
import org.jboss.as.domain.controller.FileRepository;
import org.jboss.as.protocol.mgmt.FlushableDataOutput;
import org.jboss.as.protocol.mgmt.ManagementRequestHandler;
import org.jboss.dmr.ModelNode;
import org.jboss.logging.Logger;

/**
 * ModelController operation handler that also has the operations for HC->DC.
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 * @version $Revision: 1.1 $
 */
public class MasterDomainControllerOperationHandlerImpl extends ModelControllerOperationHandlerImpl {

    private static final Logger log = Logger.getLogger("org.jboss.as.host.controller");

    public MasterDomainControllerOperationHandlerImpl(DomainController modelController) {
        super(modelController);
    }

    @Override
    protected DomainController getController() {
        return (DomainController)super.getController();
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
        default:
            return super.getRequestHandler(id);
        }
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
            try {
                getController().addClient(new RemoteDomainControllerSlaveClient(hostId, getChannel()));
                node = getController().getDomainModel();
            } catch (IllegalArgumentException e){
                log.error(e);
                node = new ModelNode();
                node.get("protocol-error").set(e.getMessage());
            }
            output.write(DomainControllerProtocol.PARAM_MODEL);
            node.writeExternal(output);
        }
    }

    private class UnregisterOperation extends RegistryOperation {
        @Override
        protected void writeResponse(final FlushableDataOutput output) throws IOException {
            getController().removeClient(hostId);
        }
    }

    private class GetFileOperation extends RegistryOperation {
        private File localPath;

        @Override
        protected void readRequest(final DataInput input) throws IOException {
            final byte rootId;
            final String filePath;
            final FileRepository localFileRepository = getController().getFileRepository();
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
