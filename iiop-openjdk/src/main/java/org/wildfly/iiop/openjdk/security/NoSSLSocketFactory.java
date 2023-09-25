/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.iiop.openjdk.security;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;

import org.wildfly.iiop.openjdk.Constants;
import org.wildfly.iiop.openjdk.logging.IIOPLogger;

/**
 * <p>
 *  This class is responsible for creating Sockets used by IIOP subsystem.
 * <p>
 *
 * @author <a href="mailto:tadamski@redhat.com">Tomasz Adamski</a>
 */

public class NoSSLSocketFactory extends SocketFactoryBase {


    @Override
    public ServerSocket createServerSocket(String type, InetSocketAddress inetSocketAddress) throws IOException {
        //we can only warn here because of backward compatibility
        if (type.equals(Constants.SSL_SOCKET_TYPE)) {
            IIOPLogger.ROOT_LOGGER.cannotCreateSSLSocket();
        }
        return super.createServerSocket(type, inetSocketAddress);
    }

    @Override
    public Socket createSocket(String type, InetSocketAddress inetSocketAddress) throws IOException {
        //we can only warn here because of backward compatibility
        if (type.contains(Constants.SSL_SOCKET_TYPE)){
            IIOPLogger.ROOT_LOGGER.cannotCreateSSLSocket();
        }
        return super.createSocket(type, inetSocketAddress);
    }

}

