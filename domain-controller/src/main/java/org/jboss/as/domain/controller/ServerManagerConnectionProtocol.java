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
import java.util.HashMap;
import java.util.Map;

/**
 * Protocol used by the domain controller to communicate with server manager instances.
 * 
 * @author John E. Bailey
 */
public class ServerManagerConnectionProtocol {
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

    /**
     * Outgoing commands to be sent to the server manager.
     */
    enum OutgoingCommand {
        CONFIRM_REGISTRATION((byte) 0) {
            void doExecute(ServerManagerConnection serverManagerConnection, Marshaller marshaller) throws Exception {
                marshaller.writeByte(command);
            }},
        UPDATE_DOMAIN((byte) 1) {
            void doExecute(final ServerManagerConnection serverManagerConnection, final Marshaller marshaller) throws Exception {
                marshaller.writeObject(serverManagerConnection.getDomainController().getDomain());
            }
        },;

        final Byte command;

        OutgoingCommand(Byte command) {
            this.command = command;
        }

        void execute(final ServerManagerConnection serverManagerConnection, final OutputStream outputStream) throws Exception {
            Marshaller marshaller = null;
            try {
                marshaller = MARSHALLER_FACTORY.createMarshaller(CONFIG);
                marshaller.start(Marshalling.createByteOutput(outputStream));
                marshaller.writeByte(command);
                doExecute(serverManagerConnection, marshaller);
                marshaller.finish();
            } finally {
                safeClose(marshaller);
            }
        }

        abstract void doExecute(final ServerManagerConnection serverManagerConnection, final Marshaller marshaller) throws Exception;
    }

    /**
     * Incoming commands from the server manager.
     */
    enum IncomingCommand {
        REGISTER((byte) 0) {
            protected void execute(final ServerManagerConnection serverManagerConnection, final Unmarshaller unmarshaller) throws Exception {
                log.infof("Server manager registered [%s]", serverManagerConnection.getId());
                serverManagerConnection.confirmRegistration();
                serverManagerConnection.updateDomain();
            }
        },
        UNREGISTER(Byte.MAX_VALUE) {
            protected void execute(final ServerManagerConnection serverManagerConnection, final Unmarshaller unmarshaller) throws Exception {
                serverManagerConnection.unregistered();
                log.infof("Server manager unregistered [%s]", serverManagerConnection.getId());

            }
        },;

        final Byte command;

        IncomingCommand(Byte command) {
            this.command = command;
        }

        static void processNext(final ServerManagerConnection serverManagerConnection, final InputStream inputStream) throws Exception {
            Unmarshaller unmarshaller = null;
            try {
                unmarshaller = MARSHALLER_FACTORY.createUnmarshaller(CONFIG);
                unmarshaller.start(Marshalling.createByteInput(inputStream));
                final byte commandByte = unmarshaller.readByte();
                final IncomingCommand command = IncomingCommand.commandFor(commandByte);
                if (command == null) {
                    throw new RuntimeException("Invalid command byte received: " + commandByte);
                }
                command.execute(serverManagerConnection, unmarshaller); // TODO: How to handle failed commands?
                unmarshaller.finish();
            } finally {
                safeClose(unmarshaller);
            }
        }

        protected abstract void execute(final ServerManagerConnection serverManagerConnection, final Unmarshaller unmarshaller) throws Exception;

        static Map<Byte, IncomingCommand> COMMANDS = new HashMap<Byte, IncomingCommand>();

        static {
            for (IncomingCommand command : values()) {
                COMMANDS.put(command.command, command);
            }
        }

        static IncomingCommand commandFor(final byte commandByte) {
            return COMMANDS.get(Byte.valueOf(commandByte));
        }
    }

    private static void safeClose(final Closeable closeable) {
        if (closeable != null) try {
            closeable.close();
        } catch (Throwable ignored) {
            // todo: log me
        }
    }
}
