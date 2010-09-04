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

package org.jboss.as.domain.controller;

import org.jboss.logging.Logger;
import org.jboss.marshalling.Marshaller;
import org.jboss.marshalling.MarshallerFactory;
import org.jboss.marshalling.Marshalling;
import org.jboss.marshalling.MarshallingConfiguration;
import org.jboss.marshalling.ModularClassTable;
import org.jboss.marshalling.Unmarshaller;
import org.jboss.modules.ModuleClassLoader;
import org.jboss.modules.ModuleLoadException;

import java.io.Closeable;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

/**
 * Responsible for managing the communication with a single server manager instance.
 * 
 * @author John E. Bailey
 */
public class ServerManagerConnection implements Runnable {
    private static final Logger log = Logger.getLogger("org.jboss.as.domain.controller");
    private static final MarshallerFactory MARSHALLER_FACTORY;
    private static final MarshallingConfiguration CONFIG;

    static {
        try {
            MARSHALLER_FACTORY = Marshalling.getMarshallerFactory("river", ModuleClassLoader.forModuleName("org.jboss.marshalling:jboss-marshalling-river"));
        } catch (ModuleLoadException e) {
            throw new RuntimeException(e);
        }
        final MarshallingConfiguration config = new MarshallingConfiguration();
        config.setClassTable(ModularClassTable.getInstance());
        CONFIG = config;
    }

    private final String id;
    private final Socket socket;
    private final DomainController domainController;

    /**
     * Create a new instance.
     *
     * @param id The server manager identifier
     * @param domainController The comain controller
     * @param socket The server managers socket
     */
    public ServerManagerConnection(final String id, final DomainController domainController, final Socket socket) {
        this.id = id;
        this.domainController = domainController;
        this.socket = socket;
    }

    @Override
    public void run() {
        Unmarshaller unmarshaller = null;
        try {
            final InputStream inputStream = socket.getInputStream();
            unmarshaller = MARSHALLER_FACTORY.createUnmarshaller(CONFIG);
            for (;;) {
                if(!socket.isConnected())
                    break;
                unmarshaller.start(Marshalling.createByteInput(inputStream));

                byte commandByte = unmarshaller.readByte();
                ProtocolCommand command = ProtocolCommand.commandFor(commandByte);
                if(command == null) {
                    throw new RuntimeException("Invalid command byte received: " + commandByte);
                }
                command.execute(id, socket, unmarshaller); // TODO: How to handle failed commands?
            }
        } catch (Throwable t) {
            throw new RuntimeException(t);
        } finally {
            safeClose(unmarshaller);
            if(socket != null) {
                try {
                    socket.close();
                } catch (Throwable ignored) {
                    // todo: log me
                }
            }
        }
    }

    private static void safeClose(final Closeable closeable) {
        if (closeable != null) try {
            closeable.close();
        } catch (Throwable ignored) {
            // todo: log me
        }
    }


    enum ProtocolCommand {
        REGISTER((byte)1) {
            void doExecute(final String id, final Socket socket, final Unmarshaller unmarshaller, final Marshaller marshaller) throws Exception {
                log.infof("Server manager registered [%s]", id);
                unmarshaller.finish();
            }},
        ;

        final Byte command;

        ProtocolCommand(Byte command) {
            this.command = command;
        }

        void execute(final String id, final Socket socket, final Unmarshaller unmarshaller) throws Exception {
            if(socket == null) throw new IllegalArgumentException("Socket is null");
            if(unmarshaller == null) throw new IllegalArgumentException("Unmarshaller is null");
            Marshaller marshaller = null;
            try {
                marshaller = MARSHALLER_FACTORY.createMarshaller(CONFIG);
                final OutputStream outputStream = socket.getOutputStream();
                marshaller.start(Marshalling.createByteOutput(outputStream));
                doExecute(id, socket, unmarshaller, marshaller);
            } finally {
                unmarshaller.finish();
                if(marshaller != null) marshaller.finish();
            }
        }

        abstract void doExecute(final String id, final Socket socket, final Unmarshaller unmarshaller, final Marshaller marshaller) throws Exception;

        static Map<Byte, ProtocolCommand> COMMANDS = new HashMap<Byte, ProtocolCommand>();
        static {
            for(ProtocolCommand command : values()) {
                COMMANDS.put(command.command, command);
            }
        }

        static ProtocolCommand commandFor(final byte commandByte) {
            return COMMANDS.get(Byte.valueOf(commandByte));
        }
    }
}