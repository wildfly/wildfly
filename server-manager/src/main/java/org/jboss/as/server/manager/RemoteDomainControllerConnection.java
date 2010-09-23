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

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import org.jboss.as.model.DomainModel;
import org.jboss.as.model.HostModel;
import org.jboss.as.server.manager.management.DomainControllerOperationHandler;
import org.jboss.as.server.manager.management.ManagementOperationHandler;
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
 * Connection to a remote domain controller.
 *
 * @author John Bailey
 */
public class RemoteDomainControllerConnection implements DomainControllerConnection {
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

    private final String serverManagerId;
    private final InetAddress dcAddress;
    private final int dcPort;

    /**
     * Create an instance.
     *
     * @param serverManagerId The identifier of the server manager
     * @param dcAddress       The domain controller port
     * @param dcPort          The domain controller port
     */
    public RemoteDomainControllerConnection(final String serverManagerId, final InetAddress dcAddress, final int dcPort) {
        this.serverManagerId = serverManagerId;
        this.dcAddress = dcAddress;
        this.dcPort = dcPort;
    }

    /**
     * {@inheritDoc}
     */
    public DomainModel register(final HostModel hostModel) {
        return execute(new RegisterOperatation(hostModel));
    }

    /**
     * {@inheritDoc}
     */
    public void unregister(final HostModel hostModel) {
        execute(new UnregisterOperatation(hostModel));
    }

    private <T> T execute(final DomainControllerOperatation<T> operatation) {
        Socket socket = null;
        Marshaller marshaller = null;
        Unmarshaller unmarshaller = null;
        try {
            socket = new Socket(dcAddress, dcPort);
            final InputStream socketIn = new BufferedInputStream(socket.getInputStream());
            final OutputStream socketOut = new BufferedOutputStream(socket.getOutputStream());

            socketOut.write(ManagementOperationHandler.SIGNATURE);
            socketOut.write(DomainControllerOperationHandler.DOMAIN_CONTROLLER_HANDLER);

            marshaller = MARSHALLER_FACTORY.createMarshaller(CONFIG);
            marshaller.start(Marshalling.createByteOutput(socketOut));

            marshaller.writeUTF(serverManagerId);
            marshaller.writeByte(operatation.getCommandByte());

            operatation.sendRequest(marshaller);
            marshaller.finish();

            unmarshaller = MARSHALLER_FACTORY.createUnmarshaller(CONFIG);
            unmarshaller.start(Marshalling.createByteInput(socketIn));
            final T result = operatation.readResponse(unmarshaller);
            unmarshaller.finish();
            return result;
        } catch (Throwable t) {
            throw new RuntimeException("Failed to execute server manager request " + operatation, t);
        } finally {
            safeClose(marshaller);
            safeClose(unmarshaller);
            safeClose(socket);
        }
    }

    private interface DomainControllerOperatation<T> {
        byte getCommandByte();

        void sendRequest(final Marshaller marshaller) throws Exception;

        T readResponse(final Unmarshaller unmarshaller) throws Exception;
    }

    private class RegisterOperatation implements DomainControllerOperatation<DomainModel> {
        private final HostModel hostModel;

        private RegisterOperatation(HostModel hostModel) {
            this.hostModel = hostModel;
        }

        public final byte getCommandByte() {
            return (byte) 0;
        }

        public void sendRequest(final Marshaller marshaller) throws Exception {
            marshaller.writeObject(hostModel);
        }

        public DomainModel readResponse(Unmarshaller unmarshaller) throws Exception {
            final DomainModel domainModel = unmarshaller.readObject(DomainModel.class);

            byte response = unmarshaller.readByte();
            if (response != getCommandByte()) {
                // TODO: Handle invalid response..
            }
            return domainModel;
        }
    }


    private class UnregisterOperatation implements DomainControllerOperatation<Void> {
        private final HostModel hostModel;

        private UnregisterOperatation(HostModel hostModel) {
            this.hostModel = hostModel;
        }

        public final byte getCommandByte() {
            return Byte.MAX_VALUE;
        }

        public void sendRequest(Marshaller marshaller) throws Exception {
            marshaller.writeObject(hostModel);
        }

        public Void readResponse(Unmarshaller unmarshaller) throws Exception {
             byte response = unmarshaller.readByte();
            if(response != getCommandByte()) {
                // TODO: Handle invalid response..
            }
            return null;
        }
    }

    private void safeClose(final Socket socket) {
        if (socket == null)
            return;
        try {
            socket.shutdownOutput();
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            socket.shutdownInput();
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void safeClose(final Closeable closeable) {
        if (closeable != null) try {
            closeable.close();
        } catch (Throwable ignored) {
            // todo: log me
        }
    }
}
