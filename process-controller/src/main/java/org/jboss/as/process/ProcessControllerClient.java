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

import static org.jboss.as.process.ProcessLogger.CLIENT_LOGGER;
import static org.jboss.as.process.ProcessMessages.MESSAGES;
import static org.jboss.as.process.protocol.StreamUtils.readFully;
import static org.jboss.as.process.protocol.StreamUtils.readInt;
import static org.jboss.as.process.protocol.StreamUtils.readLong;
import static org.jboss.as.process.protocol.StreamUtils.readUTFZBytes;
import static org.jboss.as.process.protocol.StreamUtils.readUnsignedByte;
import static org.jboss.as.process.protocol.StreamUtils.safeClose;
import static org.jboss.as.process.protocol.StreamUtils.writeBoolean;
import static org.jboss.as.process.protocol.StreamUtils.writeInt;
import static org.jboss.as.process.protocol.StreamUtils.writeUTFZBytes;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

import org.jboss.as.process.protocol.Connection;
import org.jboss.as.process.protocol.MessageHandler;
import org.jboss.as.process.protocol.ProtocolClient;
import org.jboss.as.process.protocol.StreamUtils;

/**
 * A client to the Process Controller.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public final class ProcessControllerClient implements Closeable {

    private final Connection connection;

    ProcessControllerClient(final Connection connection) {
        this.connection = connection;
    }

    public static ProcessControllerClient connect(final ProtocolClient.Configuration configuration, final byte[] authCode, final ProcessMessageHandler messageHandler) throws IOException {
        if (configuration == null) {
            throw MESSAGES.nullVar("configuration");
        }
        if (authCode == null) {
            throw MESSAGES.nullVar("authCode");
        }
        if (messageHandler == null) {
            throw MESSAGES.nullVar("messageHandler");
        }
        configuration.setMessageHandler(new MessageHandler() {
            public void handleMessage(final Connection connection, final InputStream dataStream) throws IOException {
                final ProcessControllerClient client = (ProcessControllerClient) connection.getAttachment();
                final int cmd = readUnsignedByte(dataStream);
                switch (cmd) {
                    case Protocol.PROCESS_ADDED: {
                        final String processName = readUTFZBytes(dataStream);
                        dataStream.close();
                        CLIENT_LOGGER.tracef("Received process_added for process %s", processName);
                        messageHandler.handleProcessAdded(client, processName);
                        break;
                    }
                    case Protocol.PROCESS_STARTED: {
                        final String processName = readUTFZBytes(dataStream);
                        dataStream.close();
                        CLIENT_LOGGER.tracef("Received process_started for process %s", processName);
                        messageHandler.handleProcessStarted(client, processName);
                        break;
                    }
                    case Protocol.PROCESS_STOPPED: {
                        final String processName = readUTFZBytes(dataStream);
                        final long uptimeMillis = readLong(dataStream);
                        dataStream.close();
                        CLIENT_LOGGER.tracef("Received process_stopped for process %s", processName);
                        messageHandler.handleProcessStopped(client, processName, uptimeMillis);
                        break;
                    }
                    case Protocol.PROCESS_REMOVED: {
                        final String processName = readUTFZBytes(dataStream);
                        dataStream.close();
                        CLIENT_LOGGER.tracef("Received process_removed for process %s", processName);
                        messageHandler.handleProcessRemoved(client, processName);
                        break;
                    }
                    case Protocol.PROCESS_INVENTORY: {
                        final int cnt = readInt(dataStream);
                        final Map<String, ProcessInfo> inventory = new HashMap<String, ProcessInfo>();
                        for (int i = 0; i < cnt; i++) {
                            final String processName = readUTFZBytes(dataStream);
                            final byte[] processAuthCode = new byte[16];
                            final boolean processRunning = StreamUtils.readBoolean(dataStream);
                            readFully(dataStream, processAuthCode);
                            inventory.put(processName, new ProcessInfo(processName, authCode, processRunning));
                        }
                        dataStream.close();
                        CLIENT_LOGGER.tracef("Received process_inventory");
                        messageHandler.handleProcessInventory(client, inventory);
                        break;
                    }
                    default: {
                        CLIENT_LOGGER.receivedUnknownMessageCode(Integer.valueOf(cmd));
                        // ignore
                        dataStream.close();
                        break;
                    }
                }
            }

            public void handleShutdown(final Connection connection) throws IOException {
                final ProcessControllerClient client = (ProcessControllerClient) connection.getAttachment();
                messageHandler.handleConnectionShutdown(client);
            }

            public void handleFailure(final Connection connection, final IOException cause) throws IOException {
                final ProcessControllerClient client = (ProcessControllerClient) connection.getAttachment();
                messageHandler.handleConnectionFailure(client, cause);
            }

            public void handleFinished(final Connection connection) throws IOException {
                final ProcessControllerClient client = (ProcessControllerClient) connection.getAttachment();
                messageHandler.handleConnectionFinished(client);
            }
        });
        final ProtocolClient client = new ProtocolClient(configuration);
        final Connection connection = client.connect();
        boolean ok = false;
        try {
            final OutputStream os = connection.writeMessage();
            try {
                os.write(Protocol.AUTH);
                os.write(1);
                os.write(authCode);
                final ProcessControllerClient processControllerClient = new ProcessControllerClient(connection);
                connection.attach(processControllerClient);
                CLIENT_LOGGER.trace("Sent initial greeting message");
                os.close();
                ok = true;
                return processControllerClient;
            } finally {
                safeClose(os);
            }
        } finally {
            if (! ok) {
                safeClose(connection);
            }
        }
    }

    public OutputStream sendStdin(String processName) throws IOException {
        final OutputStream os = connection.writeMessage();
        boolean ok = false;
        try {
            os.write(Protocol.SEND_STDIN);
            writeUTFZBytes(os, processName);
            ok = true;
            return os;
        } finally {
            if (! ok) {
                safeClose(os);
            }
        }
    }

    public void addProcess(String processName, byte[] authKey, String[] cmd, String workingDir, Map<String, String> env) throws IOException {
        if (processName == null) {
            throw MESSAGES.nullVar("processName");
        }
        if (authKey == null) {
            throw MESSAGES.nullVar("authKey");
        }
        if (cmd == null) {
            throw MESSAGES.nullVar("cmd");
        }
        if (workingDir == null) {
            throw MESSAGES.nullVar("workingDir");
        }
        if (env == null) {
            throw MESSAGES.nullVar("env");
        }
        if (cmd.length < 1) {
            throw MESSAGES.invalidCommandLen();
        }
        if (authKey.length != 16) {
            throw MESSAGES.invalidAuthKeyLen();
        }
        final OutputStream os = connection.writeMessage();
        try {
            os.write(Protocol.ADD_PROCESS);
            writeUTFZBytes(os, processName);
            os.write(authKey);
            writeInt(os, cmd.length);
            for (String c : cmd) {
                writeUTFZBytes(os, c);
            }
            writeInt(os, env.size());
            for (String key : env.keySet()) {
                final String value = env.get(key);
                writeUTFZBytes(os, key);
                if (value != null) {
                    writeUTFZBytes(os, value);
                } else {
                    writeUTFZBytes(os, "");
                }
            }
            writeUTFZBytes(os, workingDir);
            os.close();
        } finally {
            safeClose(os);
        }
    }

    public void startProcess(String processName) throws IOException {
        if (processName == null) {
            throw MESSAGES.nullVar("processName");
        }
        final OutputStream os = connection.writeMessage();
        try {
            os.write(Protocol.START_PROCESS);
            writeUTFZBytes(os, processName);
            os.close();
        } finally {
            safeClose(os);
        }
    }

    public void stopProcess(String processName) throws IOException {
        if (processName == null) {
            throw MESSAGES.nullVar("processName");
        }
        final OutputStream os = connection.writeMessage();
        try {
            os.write(Protocol.STOP_PROCESS);
            writeUTFZBytes(os, processName);
            os.close();
        } finally {
            safeClose(os);
        }
    }

    public void removeProcess(String processName) throws IOException {
        if (processName == null) {
            throw MESSAGES.nullVar("processName");
        }
        final OutputStream os = connection.writeMessage();
        try {
            os.write(Protocol.REMOVE_PROCESS);
            writeUTFZBytes(os, processName);
            os.close();
        } finally {
            safeClose(os);
        }
    }

    public void requestProcessInventory() throws IOException {
        final OutputStream os = connection.writeMessage();
        try {
            os.write(Protocol.REQUEST_PROCESS_INVENTORY);
            os.close();
        } finally {
            safeClose(os);
        }
    }

    public void reconnectProcess(final String processName, final String hostName, final int port, final boolean managementSubsystemEndpoint, final byte[] authKey) throws IOException {
        if (processName == null){
            throw MESSAGES.nullVar("processName");
        }
        final OutputStream os = connection.writeMessage();
        try{
            os.write(Protocol.RECONNECT_PROCESS);
            writeUTFZBytes(os, processName);
            writeUTFZBytes(os, hostName);
            writeInt(os, port);
            writeBoolean(os, managementSubsystemEndpoint);
            os.write(authKey);
            os.close();
        } finally {
            safeClose(os);
        }
    }

    public void shutdown() throws IOException {
        final OutputStream os = connection.writeMessage();
        try {
            os.write(Protocol.SHUTDOWN);
            os.close();
        } finally {
            safeClose(os);
        }
    }

    public void close() throws IOException {
        connection.close();
    }
}
