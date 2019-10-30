/*
 * Copyright 2016 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.wildfly.iiop.openjdk.security;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.AccessController;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLServerSocketFactory;

import com.sun.corba.se.spi.orb.ORB;
import org.jboss.as.controller.capability.RuntimeCapability;
import org.jboss.as.server.CurrentServiceContainer;
import org.jboss.msc.service.ServiceContainer;
import org.jboss.msc.service.ServiceName;
import org.wildfly.iiop.openjdk.Constants;
import org.wildfly.security.manager.WildFlySecurityManager;

/**
 * A {@link com.sun.corba.se.spi.transport.ORBSocketFactory} implementation that uses Elytron supplied {@link SSLContext}s
 * to create client and server side SSL sockets.
 *
 * @author <a href="mailto:sguilhen@redhat.com">Stefan Guilhen</a>
 */
public class SSLSocketFactory extends SocketFactoryBase {

    private static final String SSL_CONTEXT_CAPABILITY = "org.wildfly.security.ssl-context";

    private static final RuntimeCapability<Void> SSL_CONTEXT_RUNTIME_CAPABILITY = RuntimeCapability
        .Builder.of(SSL_CONTEXT_CAPABILITY, true, SSLContext.class)
        .build();

    private static String serverSSLContextName = null;

    public static void setServerSSLContextName(final String serverSSLContextName) {
        SSLSocketFactory.serverSSLContextName = serverSSLContextName;
    }

    private static String clientSSLContextName = null;

    public static void setClientSSLContextName(final String clientSSLContextName) {
        SSLSocketFactory.clientSSLContextName = clientSSLContextName;
    }

    private SSLContext serverSSLContext = null;

    private SSLContext clientSSLContext = null;

    @Override
    public void setORB(ORB orb) {
        super.setORB(orb);

        ServiceContainer container = this.currentServiceContainer();
        final ServiceName serverContextServiceName = SSL_CONTEXT_RUNTIME_CAPABILITY.getCapabilityServiceName(serverSSLContextName);
        this.serverSSLContext = (SSLContext) container.getRequiredService(serverContextServiceName).getValue();

        final ServiceName clientContextServiceName = SSL_CONTEXT_RUNTIME_CAPABILITY.getCapabilityServiceName(clientSSLContextName);
        this.clientSSLContext = (SSLContext) container.getRequiredService(clientContextServiceName).getValue();
    }

    @Override
    public ServerSocket createServerSocket(String type, InetSocketAddress inetSocketAddress) throws IOException {
        if (type.equals(Constants.SSL_SOCKET_TYPE)) {
            return createSSLServerSocket(inetSocketAddress.getPort(), 1000,
                    InetAddress.getByName(inetSocketAddress.getHostName()));
        } else {
            return super.createServerSocket(type, inetSocketAddress);
        }
    }

    @Override
    public Socket createSocket(String type, InetSocketAddress inetSocketAddress) throws IOException {
        if (type.contains(Constants.SSL_SOCKET_TYPE)){
            return createSSLSocket(inetSocketAddress.getHostName(), inetSocketAddress.getPort());
        } else {
            return super.createSocket(type, inetSocketAddress);
        }
    }

    public Socket createSSLSocket(String host, int port) throws IOException {
        InetAddress address = InetAddress.getByName(host);
        javax.net.ssl.SSLSocketFactory socketFactory = this.clientSSLContext.getSocketFactory();
        return socketFactory.createSocket(address, port);
    }

    public ServerSocket createSSLServerSocket(int port, int backlog, InetAddress inetAddress) throws IOException {
        SSLServerSocketFactory serverSocketFactory = this.serverSSLContext.getServerSocketFactory();
        return serverSocketFactory.createServerSocket(port, backlog, inetAddress);
    }

    private ServiceContainer currentServiceContainer() {
        if(WildFlySecurityManager.isChecking()) {
            return AccessController.doPrivileged(CurrentServiceContainer.GET_ACTION);
        }
        return CurrentServiceContainer.getServiceContainer();
    }
}
