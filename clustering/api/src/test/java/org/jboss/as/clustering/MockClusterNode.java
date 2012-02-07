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
package org.jboss.as.clustering;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * A MockClusterNode.
 *
 * @author Brian Stansberry
 * @version $Revision$
 */
public class MockClusterNode implements ClusterNode {
    private static final long serialVersionUID = 1L;

    private final InetAddress address;
    private final int port;
    private final String name;

    public MockClusterNode(int port) {
        this(getLocalHost(), port);
    }

    private static InetAddress getLocalHost() throws IllegalStateException {
        InetAddress addr;
        try {
            try {
                addr = InetAddress.getLocalHost();
            } catch (ArrayIndexOutOfBoundsException e) {
                addr = InetAddress.getByName(null);
            }
        } catch (UnknownHostException e) {
            throw new IllegalStateException(e);
        }
        return addr;
    }


    public MockClusterNode(InetAddress addr, int port) {
        this.address = addr;
        this.port = port;
        this.name = addr.getHostAddress() + ":" + port;
    }

    @Override
    public InetAddress getIpAddress() {
        return address;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public int getPort() {
        return port;
    }

    @Override
    public int compareTo(ClusterNode o) {
        return o == null ? -1 : name.compareTo(o.getName());
    }
}
