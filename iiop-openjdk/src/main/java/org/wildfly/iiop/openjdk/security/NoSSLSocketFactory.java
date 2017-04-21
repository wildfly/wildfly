/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2016, Red Hat, Inc., and individual contributors
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

