/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2014, Red Hat, Inc., and individual contributors
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

package org.jboss.as.domain.management.security;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.KeyManagementException;
import java.security.SecureRandom;

import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLContextSpi;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSessionContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;

/**
 * A wrapper around a standard {@link SSLContext} so that additional settings can be applied to the {@link SSLEngine} as it is created.
 *
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
class WrapperSSLContext extends SSLContext {

    WrapperSSLContext(final SSLContext toWrap, final String[] enabledCipherSuites, final String[] enabledProtocols) {
        super(new WrapperSpi(toWrap, enabledCipherSuites, enabledProtocols), toWrap.getProvider(), toWrap.getProtocol());
    }

    private static class WrapperSpi extends SSLContextSpi {

        private final String[] enabledCipherSuites;
        private final String[] enabledProtocols;

        private final SSLContext wrapped;

        private WrapperSpi(final SSLContext wrapped, final String[] enabledCipherSuites, final String[] enabledProtocols) {
            this.wrapped = wrapped;
            this.enabledCipherSuites = enabledCipherSuites;
            this.enabledProtocols = enabledProtocols;
        }

        @Override
        protected SSLEngine engineCreateSSLEngine() {
            SSLEngine engine = wrapped.createSSLEngine();
            setSslParams(engine);
            return engine;
        }

        @Override
        protected SSLEngine engineCreateSSLEngine(String host, int port) {
            SSLEngine engine = wrapped.createSSLEngine(host, port);
            setSslParams(engine);
            return engine;
        }

        private void setSslParams(final SSLEngine engine) {
            if (enabledCipherSuites.length > 0) {
                engine.setEnabledCipherSuites(enabledCipherSuites);
            }
            if (enabledProtocols.length > 0) {
                engine.setEnabledProtocols(enabledProtocols);
            }
        }

        @Override
        protected SSLSessionContext engineGetClientSessionContext() {
            return wrapped.getClientSessionContext();
        }

        @Override
        protected SSLSessionContext engineGetServerSessionContext() {
            return wrapped.getServerSessionContext();
        }

        @Override
        protected SSLServerSocketFactory engineGetServerSocketFactory() {
            return new WrapperSSLServerSocketFactory(wrapped.getServerSocketFactory());
        }

        @Override
        protected SSLSocketFactory engineGetSocketFactory() {
            return new WrapperSSLSocketFactory(wrapped.getSocketFactory());
        }

        @Override
        protected void engineInit(KeyManager[] km, TrustManager[] tm, SecureRandom sr) throws KeyManagementException {
            wrapped.init(km, tm, sr);
        }

        private class WrapperSSLSocketFactory extends SSLSocketFactory {

            private final SSLSocketFactory wrapped;

            private WrapperSSLSocketFactory(final SSLSocketFactory wrapped) {
                this.wrapped = wrapped;
            }

            @Override
            public Socket createSocket(Socket s, String host, int port, boolean autoClose) throws IOException {
                // TODO Auto-generated method stub
                return null;
            }

            @Override
            public String[] getDefaultCipherSuites() {
                return wrapped.getDefaultCipherSuites();
            }

            @Override
            public String[] getSupportedCipherSuites() {
                return wrapped.getSupportedCipherSuites();
            }

            @Override
            public Socket createSocket(String host, int port) throws IOException {
                Socket socket = wrapped.createSocket(host,port);
                setSslParams(socket);
                return socket;
            }

            @Override
            public Socket createSocket(InetAddress host, int port) throws IOException {
                Socket socket = wrapped.createSocket(host, port);
                setSslParams(socket);
                return socket;
            }

            @Override
            public Socket createSocket(String host, int port, InetAddress localHost, int localPort) throws IOException {
                Socket socket = wrapped.createSocket(host, port, localHost, localPort);
                setSslParams(socket);
                return socket;
            }

            @Override
            public Socket createSocket(InetAddress address, int port, InetAddress localAddress, int localPort)
                    throws IOException {
                Socket socket = createSocket(address, port, localAddress, localPort);
                setSslParams(socket);
                return socket;
            }

            private void setSslParams(final Socket socket) {
                if (socket instanceof SSLSocket) {
                    SSLSocket sslSocket = (SSLSocket) socket;
                    if (enabledCipherSuites.length > 0) {
                        sslSocket.setEnabledCipherSuites(enabledCipherSuites);
                    }
                    if (enabledProtocols.length > 0) {
                        sslSocket.setEnabledProtocols(enabledProtocols);
                    }
                }
            }

        }

        private class WrapperSSLServerSocketFactory extends SSLServerSocketFactory {

            private final SSLServerSocketFactory wrapped;

            private WrapperSSLServerSocketFactory(final SSLServerSocketFactory wrapped) {
                this.wrapped = wrapped;
            }

            @Override
            public String[] getDefaultCipherSuites() {
                return wrapped.getDefaultCipherSuites();
            }

            @Override
            public String[] getSupportedCipherSuites() {
                return wrapped.getSupportedCipherSuites();
            }

            @Override
            public ServerSocket createServerSocket(int port) throws IOException {
                ServerSocket socket = wrapped.createServerSocket(port);
                setSslParams(socket);
                return socket;
            }

            @Override
            public ServerSocket createServerSocket(int port, int backlog) throws IOException {
                ServerSocket socket = wrapped.createServerSocket(port, backlog);
                setSslParams(socket);
                return socket;
            }

            @Override
            public ServerSocket createServerSocket(int port, int backlog, InetAddress ifAddress) throws IOException {
                ServerSocket socket = wrapped.createServerSocket(port, backlog, ifAddress);
                setSslParams(socket);
                return socket;
            }

            private void setSslParams(final ServerSocket socket) {
                if (socket instanceof SSLServerSocket) {
                    SSLServerSocket sslSocket = (SSLServerSocket) socket;
                    if (enabledCipherSuites.length > 0) {
                        sslSocket.setEnabledCipherSuites(enabledCipherSuites);
                    }
                    if (enabledProtocols.length > 0) {
                        sslSocket.setEnabledProtocols(enabledProtocols);
                    }
                }
            }
        }

    }

}
