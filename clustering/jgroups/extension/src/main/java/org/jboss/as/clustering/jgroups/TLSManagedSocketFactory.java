/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.clustering.jgroups;

import java.io.Closeable;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.channels.spi.SelectorProvider;
import java.util.Map;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSocketFactory;

import org.jboss.as.network.SocketBinding;
import org.jboss.as.network.SocketBindingManager;
import org.jgroups.util.Util;
import org.wildfly.clustering.jgroups.spi.TLSConfiguration;

/**
 * Specialization of {@link ManagedSocketFactory} that creates a TLS-secured sockets instead of standard sockets using
 * client and server {@link SSLContext}s managed by the application server; e.g. Elytron subsystem.
 *
 * @author Radoslav Husar
 */
public class TLSManagedSocketFactory extends ManagedSocketFactory {

    private final SSLContext clientSSLContext;
    private final SSLContext serverSSLContext;

    public TLSManagedSocketFactory(SelectorProvider provider, SocketBindingManager manager, Map<String, SocketBinding> socketBindings, TLSConfiguration tlsConfiguration) {
        super(provider, manager, socketBindings);

        this.clientSSLContext = tlsConfiguration.getClientSSLContext();
        this.serverSSLContext = tlsConfiguration.getServerSSLContext();
    }

    @Override
    public Socket createSocket(String name) throws IOException {
        SSLSocketFactory factory = this.clientSSLContext.getSocketFactory();
        Socket socket = factory.createSocket();
        SocketBinding binding = this.bindings.get(name);
        if (binding != null) {
            Closeable socketRegistration = this.manager.getNamedRegistry().registerSocket(binding.getName(), socket);
            this.closeables.put(socket, socketRegistration);
        }
        return socket;
    }

    @Override
    public ServerSocket createServerSocket(String name) throws IOException {
        SSLServerSocketFactory factory = this.serverSSLContext.getServerSocketFactory();
        ServerSocket serverSocket = factory.createServerSocket();
        SocketBinding binding = this.bindings.get(name);
        if (binding != null) {
            Closeable registeredSocket = this.manager.getNamedRegistry().registerSocket(binding.getName(), serverSocket);
            this.closeables.put(serverSocket, registeredSocket);
        }
        return serverSocket;
    }

    @Override
    public void close(Socket socket) throws IOException {
        Util.close(this.closeables.remove(socket));
        super.close(socket);
    }

    @Override
    public void close(ServerSocket socket) throws IOException {
        Util.close(this.closeables.remove(socket));
        super.close(socket);
    }

}
