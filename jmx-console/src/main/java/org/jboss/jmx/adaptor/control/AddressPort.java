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
package org.jboss.jmx.adaptor.control;

import java.net.InetAddress;
import java.lang.reflect.Method;
import java.io.IOException;

/**
 * A utility class for parsing cluster member addresses
 *
 * @author Scott.Stark@jboss.org
 * @author Dimitris.Andreadis@jboss.org
 */
public class AddressPort {
    InetAddress addr;
    Integer port;

    /**
     * Use reflection to access the address InetAddress and port if they exist in the Address implementation
     */
    public static AddressPort getMemberAddress(Object addr) throws IOException {
        AddressPort info = null;
        try {
            Class[] parameterTypes = {};
            Object[] args = {};
            Method getIpAddress = addr.getClass().getMethod("getIpAddress", parameterTypes);
            InetAddress inetAddr = (InetAddress) getIpAddress.invoke(addr, args);
            Method getPort = addr.getClass().getMethod("getPort", parameterTypes);
            Integer port = (Integer) getPort.invoke(addr, args);
            info = new AddressPort(inetAddr, port);
        } catch (Exception e) {
            if (addr instanceof String) {
                // Parse as a host:port string
                String hostAddr = (String) addr;
                int colon = hostAddr.indexOf(':');
                String host = hostAddr;
                Integer port = new Integer(0);
                if (colon > 0) {
                    host = hostAddr.substring(0, colon);
                    port = Integer.valueOf(hostAddr.substring(colon + 1));
                }
                info = new AddressPort(InetAddress.getByName(host), port);
            } else {
                throw new IOException("Failed to parse addrType=" + addr.getClass() + ", msg=" + e.getMessage());
            }
        }
        return info;
    }

    AddressPort(InetAddress addr, Integer port) {
        this.addr = addr;
        this.port = port;
    }

    public Integer getPort() {
        return port;
    }

    public InetAddress getInetAddress() {
        return addr;
    }

    public String getHostAddress() {
        return addr.getHostAddress();
    }

    public String getHostName() {
        return addr.getHostName();
    }

    public String toString() {
        return "{host(" + addr + "), port(" + port + ")}";
    }
}