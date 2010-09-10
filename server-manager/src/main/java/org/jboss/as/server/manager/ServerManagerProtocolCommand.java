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

/**
* Commands from the server manager to a server.
* These commands will be sent via the ProcessManager as MSG_BYTES. The format of the bytes is:<br>
* 0 byte: message id
* 0..n bytes: The data that goes with the command
* <p>
* Checksums and length of data are handled at the ManagedProcess/MessageHandler level
*
*
* @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
* @version $Revision: 1.1 $
*/
public enum ServerManagerProtocolCommand {

    //The SM->Server commands
    /** Message sent from ServerManager to Server containing the bytes of a {@link org.jboss.as.model.ServerModel} */
    START_SERVER((byte)1, true),
    /** Message sent from ServerManager to Server. No data */
    STOP_SERVER((byte)2),

    //The Server->SM commands
    /** Message sent from Server to ServerManager when the server is started. No data */
    SERVER_STARTED((byte)51),
    /** Message sent from Server to ServerManager when the server is stopped. No data */
    SERVER_STOPPED((byte)52),
    /** Message sent from Server to ServerManager when the server is available. No data */
    SERVER_AVAILABLE((byte)53),
    /** Message sent from Server to ServerManager when the server could not start normally. No data */
    SERVER_START_FAILED((byte)54);

    private static final Map<Byte, ServerManagerProtocolCommand> COMMANDS;
    static {
        Map<Byte, ServerManagerProtocolCommand> cmds = new HashMap<Byte, ServerManagerProtocolCommand>();
        cmds.put(START_SERVER.getId(), START_SERVER);
        cmds.put(STOP_SERVER.getId(), STOP_SERVER);
        cmds.put(SERVER_STARTED.getId(), SERVER_STARTED);
        cmds.put(SERVER_STOPPED.getId(), SERVER_STOPPED);
        cmds.put(SERVER_AVAILABLE.getId(), SERVER_AVAILABLE);
        cmds.put(SERVER_START_FAILED.getId(), SERVER_START_FAILED);
        COMMANDS = Collections.unmodifiableMap(cmds);
    }

    private final byte id;
    private boolean hasData;

    private ServerManagerProtocolCommand(byte id) {
        this(id, false);
    }

    private ServerManagerProtocolCommand(byte id, boolean hasData) {
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

    static ServerManagerProtocolCommand parse(byte id) {
        ServerManagerProtocolCommand cmd = COMMANDS.get(id);
        if (cmd == null)
            throw new IllegalArgumentException("Unknown command " + id);
        return cmd;
    }

    public static Command readCommand(byte[] bytes) throws IOException {
        ByteArrayInputStream in = new ByteArrayInputStream(bytes);
        byte command = (byte)in.read();
        byte[] data = bytes.length > 1 ? Arrays.copyOfRange(bytes, 1, bytes.length) : Command.NO_DATA;
        return new Command(ServerManagerProtocolCommand.parse(command), data);
    }

    public static class Command {
        private static final byte[] NO_DATA = new byte[0];

        private final ServerManagerProtocolCommand command;
        private final byte[] data;

        private Command(ServerManagerProtocolCommand command, byte[] data) {
            if (command == null)
                throw new IllegalArgumentException("Null command");
            if (data == null)
                throw new IllegalArgumentException("Null data");
            this.command = command;
            this.data = data;
        }

        public ServerManagerProtocolCommand getCommand() {
            return command;
        }

        public byte[] getData() {
            return data;
        }
    }
}
