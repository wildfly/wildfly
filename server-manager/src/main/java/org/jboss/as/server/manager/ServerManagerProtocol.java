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
package org.jboss.as.server.manager;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.jboss.as.model.ServerModel;
import org.jboss.logging.Logger;

/**
 * Commands from the server manager to a server. The format of the bytes is:<br>
 * 0 byte: message id
 * 0..n bytes: The data that goes with the command
 * <p>
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 * @version $Revision: 1.1 $
 */
public class ServerManagerProtocol {

    private static final Logger log = Logger.getLogger(ServerManagerProtocol.class.getName());

    /**
     * Commands sent from SM->Server
     */
    public enum ServerManagerToServerProtocolCommand {

        /** Message sent from ServerManager to Server containing the bytes of a {@link org.jboss.as.model.ServerModel} */
        START_SERVER((byte)1, true) {

            @Override
            public void handleCommand(ServerManagerToServerCommandHandler handler, Command<ServerManagerToServerProtocolCommand> cmd) throws IOException, ClassNotFoundException {
                handler.handleStartServer(ServerManagerProtocolUtils.unmarshallCommandData(ServerModel.class, cmd));
            }
        },

        /** Message sent from ServerManager to Server. No data */
        STOP_SERVER((byte)2) {

            @Override
            public void handleCommand(ServerManagerToServerCommandHandler handler, Command<ServerManagerToServerProtocolCommand> cmd) {
                handler.handleStopServer();
            }
        };


        private static final Map<Byte, ServerManagerToServerProtocolCommand> COMMANDS;
        static {
            Map<Byte, ServerManagerToServerProtocolCommand> cmds = new HashMap<Byte, ServerManagerToServerProtocolCommand>();
            cmds.put(START_SERVER.getId(), START_SERVER);
            cmds.put(STOP_SERVER.getId(), STOP_SERVER);
            COMMANDS = Collections.unmodifiableMap(cmds);
        }

        private final byte id;
        private final boolean hasData;

        private ServerManagerToServerProtocolCommand(byte id) {
            this(id, false);
        }

        private ServerManagerToServerProtocolCommand(byte id, boolean hasData) {
            this.id = id;
            this.hasData = hasData;
        }

        byte getId() {
            return id;
        }

        public byte[] createCommandBytes(byte[] data) throws IOException{
            int length = data == null ? 0 : data.length;
            if (length == 0 && hasData)
                throw new IllegalArgumentException("Expected some data for " + this);
            else if (length != 0 && !hasData)
                throw new IllegalArgumentException("Expected no data for " + this);

            if (length == 0)
                return new byte[] {id};
            byte[] all = new byte[length + 1];
            all[0] = id;
            System.arraycopy(data, 0, all, 1, length);
            return all;
        }

        static ServerManagerToServerProtocolCommand parse(byte id) {
            ServerManagerToServerProtocolCommand cmd = COMMANDS.get(id);
            if (cmd == null)
                throw new IllegalArgumentException("Unknown command " + id);
            return cmd;
        }

        public static Command<ServerManagerToServerProtocolCommand> readCommand(byte[] bytes) throws IOException {
            ByteArrayInputStream in = new ByteArrayInputStream(bytes);
            byte command = (byte)in.read();
            byte[] data = bytes.length > 1 ? Arrays.copyOfRange(bytes, 1, bytes.length) : Command.NO_DATA;
            return new Command<ServerManagerToServerProtocolCommand>(ServerManagerToServerProtocolCommand.parse(command), data);
        }

        public abstract void handleCommand(ServerManagerToServerCommandHandler handler, Command<ServerManagerToServerProtocolCommand> cmd) throws IOException, ClassNotFoundException;
    }

    /**
     * Commands sent from Server->SM
     */
    public enum ServerToServerManagerProtocolCommand {
        //The Server->SM commands
        /** Message sent from Server to ServerManager when the server is available. No data */
        SERVER_AVAILABLE((byte)51) {
            @Override
            public void handleCommand(String sourceProcessName, ServerToServerManagerCommandHandler handler, Command<ServerToServerManagerProtocolCommand> cmd) {
                handler.handleServerAvailable(sourceProcessName);
            }
        },

        /** Message sent from Server to ServerManager when the server is started. No data */
        SERVER_STARTED((byte)52) {
            @Override
            public void handleCommand(String sourceProcessName, ServerToServerManagerCommandHandler handler, Command<ServerToServerManagerProtocolCommand> cmd) {
                handler.handleServerStarted(sourceProcessName);
            }
        },

        /** Message sent from Server to ServerManager when the server is stopped. No data */
        SERVER_STOPPED((byte)53) {
            @Override
            public void handleCommand(String sourceProcessName, ServerToServerManagerCommandHandler handler, Command<ServerToServerManagerProtocolCommand> cmd) {
                handler.handleServerStopped(sourceProcessName);
            }
        },

        /** Message sent from Server to ServerManager when the server could not start normally. No data */
        SERVER_START_FAILED((byte)54) {
            @Override
            public void handleCommand(String sourceProcessName, ServerToServerManagerCommandHandler handler, Command<ServerToServerManagerProtocolCommand> cmd) {
                handler.handleServerStartFailed(sourceProcessName);
            }
        },

        /** Message sent from Server to ServerManager containing the status of the server when the server is reconnected to SM */
        SERVER_RECONNECT_STATUS((byte)55, true) {
            @Override
            public void handleCommand(String sourceProcessName, ServerToServerManagerCommandHandler handler, Command<ServerToServerManagerProtocolCommand> cmd) throws IOException, ClassNotFoundException {
                handler.handleServerReconnectStatus(sourceProcessName, ServerManagerProtocolUtils.unmarshallCommandData(ServerState.class, cmd));
            }
        };

        private final byte id;
        private final boolean hasData;

        private static final Map<Byte, ServerToServerManagerProtocolCommand> COMMANDS;
        static {
            Map<Byte, ServerToServerManagerProtocolCommand> cmds = new HashMap<Byte, ServerToServerManagerProtocolCommand>();
            cmds.put(SERVER_STARTED.getId(), SERVER_STARTED);
            cmds.put(SERVER_STOPPED.getId(), SERVER_STOPPED);
            cmds.put(SERVER_AVAILABLE.getId(), SERVER_AVAILABLE);
            cmds.put(SERVER_START_FAILED.getId(), SERVER_START_FAILED);
            cmds.put(SERVER_RECONNECT_STATUS.getId(), SERVER_RECONNECT_STATUS);
            COMMANDS = Collections.unmodifiableMap(cmds);
        }

        private ServerToServerManagerProtocolCommand(byte id) {
            this(id, false);
        }

        private ServerToServerManagerProtocolCommand(byte id, boolean hasData) {
            this.id = id;
            this.hasData = hasData;
        }

        byte getId() {
            return id;
        }

        public byte[] createCommandBytes(byte[] data) throws IOException{
            int length = data == null ? 0 : data.length;
            if (length == 0 && hasData)
                throw new IllegalArgumentException("Expected some data for " + this);
            else if (length != 0 && !hasData)
                throw new IllegalArgumentException("Expected no data for " + this);

            if (length == 0)
                return new byte[] {id};
            byte[] all = new byte[length + 1];
            all[0] = id;
            System.arraycopy(data, 0, all, 1, length);
            return all;
        }

        static ServerToServerManagerProtocolCommand parse(byte id) {
            ServerToServerManagerProtocolCommand cmd = COMMANDS.get(id);
            if (cmd == null)
                throw new IllegalArgumentException("Unknown command " + id);
            return cmd;
        }


        public static Command<ServerToServerManagerProtocolCommand> readCommand(byte[] bytes) throws IOException {
            ByteArrayInputStream in = new ByteArrayInputStream(bytes);
            byte command = (byte)in.read();
            byte[] data = bytes.length > 1 ? Arrays.copyOfRange(bytes, 1, bytes.length) : Command.NO_DATA;
            return new Command<ServerToServerManagerProtocolCommand>(ServerToServerManagerProtocolCommand.parse(command), data);
        }

        public abstract void handleCommand(String sourceProcessName, ServerToServerManagerCommandHandler handler, Command<ServerToServerManagerProtocolCommand> cmd) throws IOException, ClassNotFoundException;

    }

    public static class Command<T extends Enum<?>> {
        private static final byte[] NO_DATA = new byte[0];

        private final T command;
        private final byte[] data;

        private Command(T command, byte[] data) {
            if (command == null)
                throw new IllegalArgumentException("Null command");
            if (data == null)
                throw new IllegalArgumentException("Null data");
            this.command = command;
            this.data = data;
        }

        public T getCommand() {
            return command;
        }

        public byte[] getData() {
            return data;
        }
    }

    public abstract static class ServerManagerToServerCommandHandler {
        public void handleCommand(byte[] message) {
            Command<ServerManagerToServerProtocolCommand> cmd = null;
            try {
                cmd = ServerManagerToServerProtocolCommand.readCommand(message);
            } catch (IOException e) {
                log.error("Error reading command", e);
                return;
            }

            try {
                cmd.getCommand().handleCommand(this, cmd);
            } catch (IOException e) {
                log.error("Error unmarshalling command data", e);
            } catch (ClassNotFoundException e) {
                log.error("Error unmarshalling command data", e);
            }
        }

        public abstract void handleStartServer(ServerModel serverModel);
        public abstract void handleStopServer();
    }

    public abstract static class ServerToServerManagerCommandHandler {
        public void handleCommand(String sourceProcessName, byte[] message) {
            Command<ServerToServerManagerProtocolCommand> cmd = null;
            try {
                cmd = ServerToServerManagerProtocolCommand.readCommand(message);
            } catch (IOException e) {
                log.error("Error reading command", e);
                return;
            }

            try {
                cmd.getCommand().handleCommand(sourceProcessName, this, cmd);
            } catch (IOException e) {
                log.error("Error unmarshalling command data", e);
            } catch (ClassNotFoundException e) {
                log.error("Error unmarshalling command data", e);
            }
        }
        public abstract void handleServerAvailable(String sourceProcessName);
        public abstract void handleServerStarted(String sourceProcessName);
        public abstract void handleServerStopped(String sourceProcessName);
        public abstract void handleServerStartFailed(String sourceProcessName);
        public abstract void handleServerReconnectStatus(String sourceProcessName, ServerState state);
    }
}
