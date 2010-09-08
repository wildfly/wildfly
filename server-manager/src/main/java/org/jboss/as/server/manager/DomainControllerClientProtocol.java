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

import java.io.Closeable;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

import org.jboss.as.model.DomainModel;
import org.jboss.logging.Logger;
import org.jboss.marshalling.Marshaller;
import org.jboss.marshalling.MarshallerFactory;
import org.jboss.marshalling.Marshalling;
import org.jboss.marshalling.MarshallingConfiguration;
import org.jboss.marshalling.ModularClassTable;
import org.jboss.marshalling.Unmarshaller;
import org.jboss.modules.ModuleClassLoader;
import org.jboss.modules.ModuleLoadException;

/**
 * Protocol used by domain controller clients.
 *
 * @author John E. Bailey
 */
public class DomainControllerClientProtocol {
    private static final Logger log = Logger.getLogger("org.jboss.as.server.manager");

    private static final MarshallerFactory MARSHALLER_FACTORY;
    private static final MarshallingConfiguration CONFIG;

    static {
        try {
            MARSHALLER_FACTORY = Marshalling.getMarshallerFactory("river", ModuleClassLoader.forModuleName("org.jboss.marshalling.river"));
        } catch (ModuleLoadException e) {
            throw new RuntimeException(e);
        }
        final MarshallingConfiguration config = new MarshallingConfiguration();
        config.setClassTable(ModularClassTable.getInstance());
        CONFIG = config;
    }

    /**
     * Outgoing commands to be sent to the domain controller.
     */
    enum OutgoingCommand {
        REGISTER((byte) 0) {
            @Override
            protected void doExecute(final DomainControllerClientService domainControllerClientService, final Marshaller marshaller) throws Exception {
                marshaller.writeObject(domainControllerClientService.serverManager.getHostConfig());
            }
        },
        UNREGISTER(Byte.MAX_VALUE) {
            @Override
            protected void doExecute(final DomainControllerClientService domainControllerClientService, final Marshaller marshaller) throws Exception {
            }
        },;

        final Byte command;

        OutgoingCommand(final Byte command) {
            this.command = command;
        }

        public void execute(final DomainControllerClientService domainControllerClientService, final OutputStream outputStream) throws Exception {
            Marshaller marshaller = null;
            try {
                marshaller = MARSHALLER_FACTORY.createMarshaller(CONFIG);
                marshaller.start(Marshalling.createByteOutput(outputStream));
                marshaller.writeByte(command);
                doExecute(domainControllerClientService, marshaller);
                marshaller.finish();
            } finally {
                safeClose(marshaller);
            }
        }

        protected abstract void doExecute(final DomainControllerClientService domainControllerClientService, final Marshaller marshaller) throws Exception;
    }

    /**
     * Incoming commands received from the domain controller.
     */
    enum IncomingCommand {
        REGISTRATION_RESPONSE((byte) 0) {
            @Override
            protected void execute(final DomainControllerClientService domainControllerClientService, final Unmarshaller unmarshaller) throws Exception {
                log.info("Registered with domain controller");
            }
        },
        DOMAIN_UPDATE((byte) 1) {
            @Override
            protected void execute(final DomainControllerClientService domainControllerClientService, final Unmarshaller unmarshaller) throws Exception {
                log.info("Received domain update from domain controller");
                final DomainModel domain = unmarshaller.readObject(DomainModel.class);
                domainControllerClientService.serverManager.setDomain(domain);
            }
        },;

        final Byte command;

        IncomingCommand(final Byte command) {
            this.command = command;
        }

        public static void processNext(final DomainControllerClientService domainControllerClientService, final InputStream inputStream) throws Exception {
            Unmarshaller unmarshaller = null;
            try {
                unmarshaller = MARSHALLER_FACTORY.createUnmarshaller(CONFIG);
                unmarshaller.start(Marshalling.createByteInput(inputStream));

                final byte commandByte = unmarshaller.readByte();
                final IncomingCommand command = IncomingCommand.commandFor(commandByte);
                if (command == null) {
                    throw new RuntimeException("Invalid command byte received: " + commandByte);
                }
                command.execute(domainControllerClientService, unmarshaller);
                unmarshaller.finish();
            } finally {
                safeClose(unmarshaller);
            }
        }

        protected abstract void execute(final DomainControllerClientService domainControllerClientService, final Unmarshaller unmarshaller) throws Exception;

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
