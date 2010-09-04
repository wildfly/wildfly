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

import org.jboss.as.services.net.NetworkInterfaceBinding;
import org.jboss.logging.Logger;
import org.jboss.marshalling.Marshaller;
import org.jboss.marshalling.MarshallerFactory;
import org.jboss.marshalling.Marshalling;
import org.jboss.marshalling.MarshallingConfiguration;
import org.jboss.marshalling.ModularClassTable;
import org.jboss.marshalling.Unmarshaller;
import org.jboss.modules.ModuleClassLoader;
import org.jboss.modules.ModuleLoadException;
import org.jboss.msc.inject.InjectionException;
import org.jboss.msc.inject.Injector;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Domain controller client service.  This will connect to a domain controller to manage inter-process communication.
 * Once the connection is established this will register itself with the domain controller and start listening for
 * commands from the domain controller.
 *
 * @author John E. Bailey
 */
public class DomainControllerClientService implements Service<Void> {
    static final ServiceName SERVICE_NAME = ServiceName.JBOSS.append("domain", "controller", "client");
    private static final Logger log = Logger.getLogger("org.jboss.as.server.manager");
    private static final long POLLING_INTERVAL = 10000L;
    
    private final InjectedValue<InetAddress> domainControllerAddress = new InjectedValue<InetAddress>();
    private final InjectedValue<Integer> domainControllerPort = new InjectedValue<Integer>();
    private ExecutorService executor; // TODO: inject
    private Socket socket;
    private final ServerManager serverManager;

    public DomainControllerClientService(final ServerManager serverManager) {
        this.serverManager = serverManager;
    }

    /**
     * Start the service.  Creates and executes a {@link ConnectTask} to establish a connection to the domain controller.
     *
     * @param context The start context
     * @throws StartException
     */
    public synchronized void start(final StartContext context) throws StartException {
        executor = Executors.newSingleThreadExecutor();
        executor.execute(new ConnectTask());
    }

    /**
     * Stop the service.  This will shut down the executor and close the socket.
     *
     * @param context The stop context.
     */
    public synchronized void stop(final StopContext context) {
        try {
            if (executor != null) {
                executor.shutdown();
            }
        } finally {
            if (socket != null) try {
                socket.close();
            } catch (IOException e) {
                log.warn("Failed to close connection to domain controller", e);
            }
        }
    }

    /**
     * No value for this service.
     *
     * @return {@code null}
     */
    public synchronized Void getValue() throws IllegalStateException {
        return null;
    }

    public Injector<NetworkInterfaceBinding> getDomainControllerInterface() {
        return new Injector<NetworkInterfaceBinding>() {
            @Override
            public void inject(NetworkInterfaceBinding value) throws InjectionException {
                domainControllerAddress.inject(value.getAddress());
            }

            @Override
            public void uninject() {
                domainControllerAddress.uninject();
            }
        };
    }

    public Injector<InetAddress> getDomainControllerAddressInjector() {
        return domainControllerAddress;
    }

    public Injector<Integer> getDomainControllerPortInjector() {
        return domainControllerPort;
    }

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
     * Register with the domain controller.
     */
    public void register() {
        final Socket socket = this.socket;
        Marshaller marshaller = null;
        try {
            final OutputStream outputStream = socket.getOutputStream();
            marshaller = MARSHALLER_FACTORY.createMarshaller(CONFIG);
            marshaller.start(Marshalling.createByteOutput(outputStream));
            marshaller.writeByte((byte) 1);
            marshaller.finish();
        } catch (Throwable t) {
            throw new RuntimeException("Failed to register with domain controller", t); // TODO: better exception
        } finally {
            if (marshaller != null) try {
                marshaller.close();
            } catch (Throwable ignored) {
                // todo: log me
            }
        }
    }

    /**
     * Task that attempts to establish a connection to the domain controller.  It will poll
     * until it can establish the connection.  Once connected it will register with with the domain controller to let it
     * know this server manger exists.  Once registered it will start listening for commands from the domain controller.
     */
    private class ConnectTask implements Runnable {
        public void run() {
            for (; ;) {
                try {
                    InetAddress dcAddress = domainControllerAddress.getValue();
                    if (dcAddress.isAnyLocalAddress() || dcAddress.isSiteLocalAddress()) {
                        dcAddress = InetAddress.getLocalHost();
                    }
                    socket = new Socket(dcAddress, domainControllerPort.getValue());
                    register();
                    log.infof("Connected to domain controller [%s:%d]", dcAddress.getHostAddress(), domainControllerPort.getValue());
                    executor.execute(new ListenerTask(socket));
                    break;
                } catch (IOException e) {
                    log.info("Unable to connect to domain controller.  Will retry.");
                    log.trace("Unable to connect to domain controller.", e);
                }
                try {
                    Thread.sleep(POLLING_INTERVAL);
                } catch (InterruptedException e) {
                    throw new RuntimeException("Domain controller connection polling was interrupted.", e);
                }
            }
        }
    }


    /**
     * Task that listens for commands from the domain controller.  When it receives the command it will determine the
     * correct {@link org.jboss.as.server.manager.DomainControllerClientService.ProtocolCommand} and execute it.
     */
    private class ListenerTask implements Runnable {
        private final Socket socket;

        private ListenerTask(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            Unmarshaller unmarshaller = null;
            try {
                final InputStream inputStream = socket.getInputStream();
                unmarshaller = MARSHALLER_FACTORY.createUnmarshaller(CONFIG);
                for (; ;) {
                    if (!socket.isConnected())
                        break;
                    unmarshaller.start(Marshalling.createByteInput(inputStream));

                    byte commandByte = unmarshaller.readByte();
                    ProtocolCommand command = ProtocolCommand.commandFor(commandByte);
                    if (command == null) {
                        throw new RuntimeException("Invalid command byte received: " + commandByte);
                    }
                    command.execute(socket, unmarshaller);
                    // TODO:  What if execution bombs.  How do we recover?
                }
            } catch (Throwable t) {
                throw new RuntimeException(t); // TODO:  Better exceptions
            } finally {
                if (unmarshaller != null) try {
                    unmarshaller.close();
                } catch (Throwable ignored) {
                    // todo: log me
                }
                if (socket != null) try {
                    socket.close();
                } catch (Throwable ignored) {
                    // todo: log me
                }
            }
        }
    }

    private enum ProtocolCommand {
        REGISTRATION_RESPONSE((byte) 1) {
            void doExecute(final Socket socket, final Unmarshaller unmarshaller, final Marshaller marshaller) throws Exception {
                log.info("Registered to domain controller");
            }},;

        final Byte command;

        ProtocolCommand(final Byte command) {
            this.command = command;
        }

        void execute(final Socket socket, final Unmarshaller unmarshaller) throws Exception {
            if(unmarshaller == null) throw new IllegalArgumentException("Unmarshaller is null");
            
            Marshaller marshaller = null;
            try {
                marshaller = MARSHALLER_FACTORY.createMarshaller(CONFIG);
                final OutputStream outputStream = socket.getOutputStream();
                marshaller.start(Marshalling.createByteOutput(outputStream));
                doExecute(socket, unmarshaller, marshaller);
            } finally {
                unmarshaller.finish();
                if(marshaller != null) marshaller.finish();
            }
        }

        abstract void doExecute(Socket socket, Unmarshaller unmarshaller, Marshaller marshaller) throws Exception;

        static Map<Byte, ProtocolCommand> COMMANDS = new HashMap<Byte, ProtocolCommand>();

        static {
            for (ProtocolCommand command : values()) {
                COMMANDS.put(command.command, command);
            }
        }

        static ProtocolCommand commandFor(final byte commandByte) {
            return COMMANDS.get(Byte.valueOf(commandByte));
        }
    }
}
