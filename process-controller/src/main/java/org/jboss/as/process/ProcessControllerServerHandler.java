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

package org.jboss.as.process;

import static org.jboss.as.process.ProcessLogger.ROOT_LOGGER;
import static org.jboss.as.process.ProcessLogger.SERVER_LOGGER;
import static org.jboss.as.process.protocol.StreamUtils.readBoolean;
import static org.jboss.as.process.protocol.StreamUtils.readFully;
import static org.jboss.as.process.protocol.StreamUtils.readInt;
import static org.jboss.as.process.protocol.StreamUtils.readUTFZBytes;
import static org.jboss.as.process.protocol.StreamUtils.readUnsignedByte;
import static org.jboss.as.process.protocol.StreamUtils.safeClose;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.jboss.as.process.protocol.Connection;
import org.jboss.as.process.protocol.ConnectionHandler;
import org.jboss.as.process.protocol.MessageHandler;
import org.jboss.as.process.protocol.StreamUtils;

/**
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public final class ProcessControllerServerHandler implements ConnectionHandler {

    private final ProcessController processController;

    public ProcessControllerServerHandler(final ProcessController controller) {
        processController = controller;
    }

    public MessageHandler handleConnected(final Connection connection) throws IOException {
        SERVER_LOGGER.tracef("Received connection from %s", connection.getPeerAddress());
        return new InitMessageHandler(processController);
    }

    private static class InitMessageHandler implements MessageHandler {

        private final ProcessController processController;

        public InitMessageHandler(final ProcessController processController) {
            this.processController = processController;
        }

        public void handleMessage(final Connection connection, final InputStream dataStream) throws IOException {
            final int cmd = readUnsignedByte(dataStream);
            if (cmd != Protocol.AUTH) {
                SERVER_LOGGER.receivedUnknownGreetingCode(Integer.valueOf(cmd), connection.getPeerAddress());
                connection.close();
                return;
            }
            final int version = StreamUtils.readUnsignedByte(dataStream);
            if (version < 1) {
                SERVER_LOGGER.receivedInvalidVersion(connection.getPeerAddress());
                connection.close();
                return;
            }
            final byte[] authCode = new byte[16];
            StreamUtils.readFully(dataStream, authCode);
            final ManagedProcess process = processController.getServerByAuthCode(authCode);
            if (process == null) {
                SERVER_LOGGER.receivedUnknownCredentials(connection.getPeerAddress());
                StreamUtils.safeClose(connection);
                return;
            }
            SERVER_LOGGER.tracef("Received authentic connection from %s", connection.getPeerAddress());
            connection.setMessageHandler(new ConnectedMessageHandler(processController, process.isPrivileged()));
            processController.addManagedConnection(connection);
            dataStream.close();
        }

        public void handleShutdown(final Connection connection) throws IOException {
            SERVER_LOGGER.tracef("Received end-of-stream for connection");
            processController.removeManagedConnection(connection);
            connection.shutdownWrites();
        }

        public void handleFailure(final Connection connection, final IOException e) throws IOException {
            SERVER_LOGGER.tracef(e, "Received failure of connection");
            processController.removeManagedConnection(connection);
            connection.close();
        }

        public void handleFinished(final Connection connection) throws IOException {
            SERVER_LOGGER.tracef("Connection finished");
            processController.removeManagedConnection(connection);
            // nothing
        }

        private static class ConnectedMessageHandler implements MessageHandler {

            private final boolean isPrivileged;

            private final ProcessController processController;

            public ConnectedMessageHandler(final ProcessController processController, final boolean isHostController) {
                this.processController = processController;
                this.isPrivileged = isHostController;
            }

            public void handleMessage(final Connection connection, final InputStream dataStream) throws IOException {
                ProcessMessageHandler.OperationType operationType = null;
                String processName = null;
                try {
                    final int cmd = StreamUtils.readUnsignedByte(dataStream);
                    switch (cmd) {
                        case Protocol.SEND_STDIN: {
                            // HostController only
                            if (isPrivileged) {
                                operationType = ProcessMessageHandler.OperationType.SEND_STDIN;
                                processName = readUTFZBytes(dataStream);
                                SERVER_LOGGER.tracef("Received send_stdin for process %s", processName);
                                processController.sendStdin(processName, dataStream);
                            } else {
                                SERVER_LOGGER.tracef("Ignoring send_stdin message from untrusted source");
                            }
                            dataStream.close();
                            break;
                        }
                        case Protocol.ADD_PROCESS: {
                            if (isPrivileged) {
                                operationType = ProcessMessageHandler.OperationType.ADD;
                                processName = readUTFZBytes(dataStream);
                                final byte[] authKey = new byte[16];
                                readFully(dataStream, authKey);
                                final int commandCount = readInt(dataStream);
                                final String[] command = new String[commandCount];
                                for (int i = 0; i < commandCount; i ++) {
                                    command[i] = readUTFZBytes(dataStream);
                                }
                                final int envCount = readInt(dataStream);
                                final Map<String, String> env = new HashMap<String, String>();
                                for (int i = 0; i < envCount; i ++) {
                                    env.put(readUTFZBytes(dataStream), readUTFZBytes(dataStream));
                                }
                                final String workingDirectory = readUTFZBytes(dataStream);
                                SERVER_LOGGER.tracef("Received add_process for process %s", processName);
                                processController.addProcess(processName, Arrays.asList(command), env, workingDirectory, false, false);
                            } else {
                                SERVER_LOGGER.tracef("Ignoring add_process message from untrusted source");
                            }
                            dataStream.close();
                            break;
                        }
                        case Protocol.START_PROCESS: {
                            if (isPrivileged) {
                                operationType = ProcessMessageHandler.OperationType.START;
                                processName = readUTFZBytes(dataStream);
                                processController.startProcess(processName);
                                SERVER_LOGGER.tracef("Received start_process for process %s", processName);
                            } else {
                                SERVER_LOGGER.tracef("Ignoring start_process message from untrusted source");
                            }
                            dataStream.close();
                            break;
                        }
                        case Protocol.STOP_PROCESS: {
                            if (isPrivileged) {
                                operationType = ProcessMessageHandler.OperationType.STOP;
                                processName = readUTFZBytes(dataStream);
                                // HostController only
                                processController.stopProcess(processName);
                            } else {
                                SERVER_LOGGER.tracef("Ignoring stop_process message from untrusted source");
                            }
                            dataStream.close();
                            break;
                        }
                        case Protocol.REMOVE_PROCESS: {
                            if (isPrivileged) {
                                operationType = ProcessMessageHandler.OperationType.REMOVE;
                                processName = readUTFZBytes(dataStream);
                                processController.removeProcess(processName);
                            } else {
                                SERVER_LOGGER.tracef("Ignoring remove_process message from untrusted source");
                            }
                            dataStream.close();
                            break;
                        }
                        case Protocol.REQUEST_PROCESS_INVENTORY: {
                            if (isPrivileged) {
                                operationType = ProcessMessageHandler.OperationType.INVENTORY;
                                processController.sendInventory();
                            } else {
                                SERVER_LOGGER.tracef("Ignoring request_process_inventory message from untrusted source");
                            }
                            dataStream.close();
                            break;
                        }
                        case Protocol.RECONNECT_PROCESS: {
                            if (isPrivileged) {
                                operationType = ProcessMessageHandler.OperationType.REMOVE;
                                processName = readUTFZBytes(dataStream);
                                final String hostName = readUTFZBytes(dataStream);
                                final int port = readInt(dataStream);
                                final boolean managementSubsystemEndpoint = readBoolean(dataStream);
                                final byte[] asAuthKey = new byte[16];
                                readFully(dataStream, asAuthKey);
                                processController.sendReconnectProcess(processName, hostName, port, managementSubsystemEndpoint, asAuthKey);
                            } else {
                                SERVER_LOGGER.tracef("Ignoring reconnect_process message from untrusted source");
                            }
                            dataStream.close();
                            break;
                        } case Protocol.SHUTDOWN: {
                            if (isPrivileged) {
                                final int exitCode = readInt(dataStream);
                                new Thread(new Runnable() {
                                    public void run() {
                                        processController.shutdown();
                                        System.exit(exitCode);
                                    }
                                }).start();
                            } else {
                                SERVER_LOGGER.tracef("Ignoring shutdown message from untrusted source");
                            }
                            break;
                        }
                        default: {
                            SERVER_LOGGER.receivedUnknownMessageCode(Integer.valueOf(cmd));
                            // unknown
                            dataStream.close();
                        }
                    }
                } catch(IOException e) {
                    if(operationType != null && processName != null) {
                        safeClose(dataStream);
                        try {
                            final OutputStream os = connection.writeMessage();
                            try {
                                os.write(Protocol.OPERATION_FAILED);
                                os.write(operationType.getCode());
                                StreamUtils.writeUTFZBytes(os, processName);
                                os.close();
                            } finally {
                                safeClose(os);
                            }
                        } catch (IOException ignore) {
                            ROOT_LOGGER.debugf(ignore, "failed to write operation failed message");
                        }
                    }
                    throw e;
                } finally {
                    safeClose(dataStream);
                }
            }

            public void handleShutdown(final Connection connection) throws IOException {
                SERVER_LOGGER.tracef("Received end-of-stream for connection");
                processController.removeManagedConnection(connection);
                connection.shutdownWrites();
            }

            public void handleFailure(final Connection connection, final IOException e) throws IOException {
                SERVER_LOGGER.tracef(e, "Received failure of connection");
                processController.removeManagedConnection(connection);
                connection.close();
            }

            public void handleFinished(final Connection connection) throws IOException {
                SERVER_LOGGER.tracef("Connection finished");
                processController.removeManagedConnection(connection);
                connection.close();
            }
        }
    }
}
