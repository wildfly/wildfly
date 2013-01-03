/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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
package org.jboss.as.domain.management.connections.ldap;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;

import javax.net.SocketFactory;
import javax.net.ssl.SSLSocketFactory;

/**
 * An SSLSocketFactory that delegates to a SSLSocketFactory set on a ThreadLocal, if the SSLSocketFactory is not set then the
 * default implementation is used.
 *
 * The purpose of this class is to allow custom configuration to be used when only the name of the SSLSocketFactory to be
 * instantiated later can be specified - this does assume that the SSLSocketFactory is not instantiated in a different thread.
 *
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
public class ThreadLocalSSLSocketFactory extends SSLSocketFactory {

    private static final ThreadLocal<SSLSocketFactory> socketFactory = new ThreadLocal<SSLSocketFactory>();

    private final SSLSocketFactory delegate;

    public ThreadLocalSSLSocketFactory() {
        SSLSocketFactory socketFactory = ThreadLocalSSLSocketFactory.socketFactory.get();
        if (socketFactory == null) {
            socketFactory = (SSLSocketFactory) SSLSocketFactory.getDefault();
        }
        delegate = socketFactory;
    }

    /**
     * Set the SSLSocketFactory to be used when an instance of this class is created on this thread.
     *
     * This method has the default level of access to prevent it's use from other packages, should this need to be opened up
     * appropriate permissions should be set to prevent code in other packages from changing the SSLSocketFactory in use.
     *
     * @param factory - The SSLSocketFactory to set.
     */
    static void setSSLSocketFactory(final SSLSocketFactory factory) {
        socketFactory.set(factory);
    }

    /**
     * Remove the previously set SSLSocketFactory.
     *
     * As with setSSLSocketFactory visibility of this method is reduced to prevent modification from other packages.
     */
    static void removeSSLSocketFactory() {
        socketFactory.remove();
    }

    public static SocketFactory getDefault() {
        return new ThreadLocalSSLSocketFactory();
    }

    @Override
    public Socket createSocket(Socket socket, String host, int port, boolean autoClose) throws IOException {
        return delegate.createSocket(socket, host, port, autoClose);
    }

    @Override
    public String[] getDefaultCipherSuites() {
        return delegate.getDefaultCipherSuites();
    }

    @Override
    public String[] getSupportedCipherSuites() {
        return delegate.getSupportedCipherSuites();
    }

    @Override
    public Socket createSocket(String host, int port) throws IOException {
        return delegate.createSocket(host, port);
    }

    @Override
    public Socket createSocket(InetAddress address, int port) throws IOException {
        return delegate.createSocket(address, port);
    }

    @Override
    public Socket createSocket(String host, int port, InetAddress localAddress, int localPort) throws IOException {
        return delegate.createSocket(host, port, localAddress, localPort);
    }

    @Override
    public Socket createSocket(InetAddress address, int port, InetAddress localAddress, int localPort) throws IOException {
        return delegate.createSocket(address, port, localAddress, localPort);
    }

}
