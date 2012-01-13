/*
* JBoss, Home of Professional Open Source
* Copyright 2012, Red Hat Inc., and individual contributors as indicated
* by the @authors tag. See the copyright.txt in the distribution for a
* full listing of individual contributors.
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
package org.jboss.as.network;

import java.net.InetAddress;

/**
 * A client mapping specifies how external clients should connect to a
 * socket's port, provided that the client's outbound interface
 * match the specified source network value.
 *
 * @author Jason T. Greene
 */
public class ClientMapping {
    private final InetAddress sourceNetworkAddress;
    private final byte sourceNetworkMaskBits;
    private final String destinationAddress;
    private volatile int destinationPort = -1;


    /**
     * Construct a new client mapping.
     *
     * @param sourceNetworkAddress The IP of the source network to match the outbound interface against
     * @param sourceNetworkMaskBits The masked portion of the source network to match the outbound interface against
     * @param destinationAddress The destination host/ip the client should connect to.
     * @param destinationPort The destination port the client should connect to.  A value of -1 indicates that
     * the effective server listening port should be used.
     */
    public ClientMapping(InetAddress sourceNetworkAddress, int sourceNetworkMaskBits, String destinationAddress, int destinationPort) {
        this.sourceNetworkAddress = sourceNetworkAddress;
        this.sourceNetworkMaskBits = (byte) sourceNetworkMaskBits;
        this.destinationAddress = destinationAddress;
        this.destinationPort = destinationPort;
    }


    /**
     * Source network the client connection binds on. A client should match this value with the mask returned by
     * {@link #getSourceNetworkMaskBits()} against the desired client host network interface, and if matched the
     * client should connect to the corresponding destination values..
     *
     * @return The IP to match with
     */
    public InetAddress getSourceNetworkAddress() {
        return sourceNetworkAddress;
    }

    /**
     * Source network the client connection binds on. A client should match this value with the ip returned by
     * {@link #getSourceNetworkAddress()} against the desiered client host network interface,  and if matched the
     * client should connect to the corresponding destination values.
     *
     * @return the number of mask bits starting from the LSB
     */
    public int getSourceNetworkMaskBits() {
        return sourceNetworkMaskBits & 0xFF;
    }


    /**
     * The destination host or IP that the client should connect to. Note this value only has meaning to the client,
     * which may be on a completely different network topology, with a client specific DNS, so the server SHOULD NOT
     * attempt to resolve this value.
     *
     * @return the host/ip to connect to should this mapping match
     */
    public String getDestinationAddress() {
        return destinationAddress;
    }

    /**
     * The destination port that the client should connect to. -1 is returned if not yet known
     *
     * @return the port or -1 if not yet known
     */
    public int getDestinationPort() {
        return destinationPort;
    }

    void updatePortIfUnknown(int port) {
        if (port == -1)
            this.destinationPort = port;
    }
}