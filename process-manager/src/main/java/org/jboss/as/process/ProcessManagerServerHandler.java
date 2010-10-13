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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import org.jboss.as.protocol.Connection;
import org.jboss.as.protocol.ConnectionHandler;
import org.jboss.as.protocol.MessageHandler;
import org.jboss.as.protocol.StreamUtils;
import org.jboss.logging.Logger;

import static org.jboss.as.protocol.StreamUtils.*;

/**
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public final class ProcessManagerServerHandler implements ConnectionHandler {

    private static final Logger log = Logger.getLogger("org.jboss.as.process-manager.server");

    private final ProcessManager processManager;

    public ProcessManagerServerHandler(final ProcessManager manager) {
        processManager = manager;
    }

    public MessageHandler handleConnected(final Connection connection) throws IOException {
        return new InitMessageHandler(processManager);
    }

    private static class InitMessageHandler implements MessageHandler {

        private final ProcessManager processManager;

        public InitMessageHandler(final ProcessManager processManager) {
            this.processManager = processManager;
        }

        public void handleMessage(final Connection connection, final InputStream dataStream) throws IOException {
            if (readUnsignedByte(dataStream) != Protocol.AUTH) {
                connection.close();
                return;
            }
            final int version = StreamUtils.readUnsignedByte(dataStream);
            if (version < 1) {
                connection.close();
                return;
            }
            final byte[] authCode = new byte[16];
            StreamUtils.readFully(dataStream, authCode);
            dataStream.close();
            final OutputStream os = connection.writeMessage();
            os.write(1); // our version
            os.close();
            final ManagedProcess process = processManager.getServerByAuthCode(authCode);
            if (process == null) {
                log.warnf("Received connection with unknown credentials from %s", connection.getPeerAddress());
                StreamUtils.safeClose(connection);
                return;
            }
            connection.setMessageHandler(new ConnectedMessageHandler(processManager, process.isInitial()));
        }

        public void handleShutdown(final Connection connection) throws IOException {
            connection.shutdownWrites();
        }

        public void handleFailure(final Connection connection, final IOException e) throws IOException {
            connection.close();
        }

        public void handleFinished(final Connection connection) throws IOException {
            // nothing
        }

        private static class ConnectedMessageHandler implements MessageHandler {

            private final boolean isServerManager;

            private final ProcessManager processManager;

            public ConnectedMessageHandler(final ProcessManager processManager, final boolean isServerManager) {
                this.processManager = processManager;
                this.isServerManager = isServerManager;
            }

            public void handleMessage(final Connection connection, final InputStream dataStream) throws IOException {
                try {

                    final int cmd = StreamUtils.readUnsignedByte(dataStream);
                    switch (cmd) {
                        case Protocol.SEND_STDIN: {
                            // SM only
                            if (isServerManager) {
                                final String processName = readUTFZBytes(dataStream);
                                processManager.sendStdin(processName, dataStream);
                            }
                            dataStream.close();
                            break;
                        }
                        case Protocol.ADD_PROCESS: {
                            if (isServerManager) {
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
                                processManager.addProcess(processName, Arrays.asList(command), env, workingDirectory, false);
                            }
                            dataStream.close();
                            break;
                        }
                        case Protocol.START_PROCESS: {
                            if (isServerManager) {
                                final String processName = readUTFZBytes(dataStream);
                                processManager.startProcess(processName);
                            }
                            dataStream.close();
                            break;
                        }
                        case Protocol.STOP_PROCESS: {
                            if (isServerManager) {
                                final String processName = readUTFZBytes(dataStream);
                                // SM only
                                processManager.stopProcess(processName);
                            }
                            dataStream.close();
                            break;
                        }
                        case Protocol.REMOVE_PROCESS: {
                            if (isServerManager) {
                                final String processName = readUTFZBytes(dataStream);
                                processManager.removeProcess(processName);
                            }
                            dataStream.close();
                            break;
                        }
                        case Protocol.REQUEST_PROCESS_INVENTORY: {
                            if (isServerManager) {
                                processManager.sendInventory();
                            }
                            dataStream.close();
                            break;
                        }
                        default: {
                            // unknown
                            dataStream.close();
                        }
                    }
                } finally {
                    safeClose(dataStream);
                }
            }

            public void handleShutdown(final Connection connection) throws IOException {
                connection.shutdownWrites();
            }

            public void handleFailure(final Connection connection, final IOException e) throws IOException {
                connection.close();
            }

            public void handleFinished(final Connection connection) throws IOException {
                // nothing
            }
        }
    }
}
