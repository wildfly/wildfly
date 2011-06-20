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

import static org.jboss.as.protocol.old.StreamUtils.readFully;
import static org.jboss.as.protocol.old.StreamUtils.readInt;
import static org.jboss.as.protocol.old.StreamUtils.readUTFZBytes;
import static org.jboss.as.protocol.old.StreamUtils.readUnsignedByte;
import static org.jboss.as.protocol.old.StreamUtils.safeClose;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.jboss.as.protocol.old.Connection;
import org.jboss.as.protocol.old.ConnectionHandler;
import org.jboss.as.protocol.old.MessageHandler;
import org.jboss.as.protocol.old.StreamUtils;
import org.jboss.logging.Logger;

/**
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public final class ProcessControllerServerHandler implements ConnectionHandler {

    private static final Logger log = Logger.getLogger("org.jboss.as.process-controller.server");

    private final ProcessController processController;

    public ProcessControllerServerHandler(final ProcessController controller) {
        processController = controller;
    }

    public MessageHandler handleConnected(final Connection connection) throws IOException {
        log.tracef("Received connection from %s", connection.getPeerAddress());
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
                log.warnf("Received unrecognized greeting code 0x%02x from %s", Integer.valueOf(cmd), connection.getPeerAddress());
                connection.close();
                return;
            }
            final int version = StreamUtils.readUnsignedByte(dataStream);
            if (version < 1) {
                log.warnf("Received connection with invalid version from %s", connection.getPeerAddress());
                connection.close();
                return;
            }
            final byte[] authCode = new byte[16];
            StreamUtils.readFully(dataStream, authCode);
            final ManagedProcess process = processController.getServerByAuthCode(authCode);
            if (process == null) {
                log.warnf("Received connection with unknown credentials from %s", connection.getPeerAddress());
                StreamUtils.safeClose(connection);
                return;
            }
            log.tracef("Received authentic connection from %s", connection.getPeerAddress());
            connection.setMessageHandler(new ConnectedMessageHandler(processController, process.isInitial()));
            processController.addManagedConnection(connection);
            dataStream.close();
        }

        public void handleShutdown(final Connection connection) throws IOException {
            log.tracef("Received end-of-stream for connection");
            processController.removeManagedConnection(connection);
            connection.shutdownWrites();
        }

        public void handleFailure(final Connection connection, final IOException e) throws IOException {
            log.tracef(e, "Received failure of connection");
            processController.removeManagedConnection(connection);
            connection.close();
        }

        public void handleFinished(final Connection connection) throws IOException {
            log.tracef("Connection finished");
            processController.removeManagedConnection(connection);
            // nothing
        }

        private static class ConnectedMessageHandler implements MessageHandler {

            private final boolean isHostController;

            private final ProcessController processController;

            public ConnectedMessageHandler(final ProcessController processController, final boolean isHostController) {
                this.processController = processController;
                this.isHostController = isHostController;
            }

            public void handleMessage(final Connection connection, final InputStream dataStream) throws IOException {
                try {

                    final int cmd = StreamUtils.readUnsignedByte(dataStream);
                    switch (cmd) {
                        case Protocol.SEND_STDIN: {
                            // HostController only
                            if (isHostController) {
                                final String processName = readUTFZBytes(dataStream);
                                log.tracef("Received send_stdin for process %s", processName);
                                processController.sendStdin(processName, dataStream);
                            } else {
                                log.tracef("Ignoring send_stdin message from untrusted source");
                            }
                            dataStream.close();
                            break;
                        }
                        case Protocol.ADD_PROCESS: {
                            if (isHostController) {
                                final String processName = readUTFZBytes(dataStream);
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
                                log.tracef("Received add_process for process %s", processName);
                                processController.addProcess(processName, Arrays.asList(command), env, workingDirectory, false);
                            } else {
                                log.tracef("Ignoring add_process message from untrusted source");
                            }
                            dataStream.close();
                            break;
                        }
                        case Protocol.START_PROCESS: {
                            if (isHostController) {
                                final String processName = readUTFZBytes(dataStream);
                                processController.startProcess(processName);
                                log.tracef("Received start_process for process %s", processName);
                            } else {
                                log.tracef("Ignoring start_process message from untrusted source");
                            }
                            dataStream.close();
                            break;
                        }
                        case Protocol.STOP_PROCESS: {
                            if (isHostController) {
                                final String processName = readUTFZBytes(dataStream);
                                // HostController only
                                processController.stopProcess(processName);
                            } else {
                                log.tracef("Ignoring stop_process message from untrusted source");
                            }
                            dataStream.close();
                            break;
                        }
                        case Protocol.REMOVE_PROCESS: {
                            if (isHostController) {
                                final String processName = readUTFZBytes(dataStream);
                                processController.removeProcess(processName);
                            } else {
                                log.tracef("Ignoring remove_process message from untrusted source");
                            }
                            dataStream.close();
                            break;
                        }
                        case Protocol.REQUEST_PROCESS_INVENTORY: {
                            if (isHostController) {
                                processController.sendInventory();
                            } else {
                                log.tracef("Ignoring request_process_inventory message from untrusted source");
                            }
                            dataStream.close();
                            break;
                        }
                        case Protocol.RECONNECT_PROCESS: {
                            if (isHostController) {
                                final String processName = readUTFZBytes(dataStream);
                                final String hostName = readUTFZBytes(dataStream);
                                final int port = readInt(dataStream);
                                processController.sendReconnectProcess(processName, hostName, port);
                            } else {
                                log.tracef("Ignoring reconnect_process message from untrusted source");
                            }
                            dataStream.close();
                            break;
                        } case Protocol.SHUTDOWN: {
                            if (isHostController) {
                                new Thread(new Runnable() {
                                    public void run() {
                                        processController.shutdown();
                                        System.exit(0);
                                    }
                                }).start();
                            } else {
                                log.tracef("Ignoring shutdown message from untrusted source");
                            }
                            break;
                        }
                        default: {
                            log.warnf("Received unknown message with code 0x%02x", Integer.valueOf(cmd));
                            // unknown
                            dataStream.close();
                        }
                    }
                } finally {
                    safeClose(dataStream);
                }
            }

            public void handleShutdown(final Connection connection) throws IOException {
                log.tracef("Received end-of-stream for connection");
                connection.shutdownWrites();
            }

            public void handleFailure(final Connection connection, final IOException e) throws IOException {
                log.tracef(e, "Received failure of connection");
                connection.close();
            }

            public void handleFinished(final Connection connection) throws IOException {
                log.tracef("Connection finished");
                // nothing
            }
        }
    }
}
