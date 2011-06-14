/*
* JBoss, Home of Professional Open Source.
* Copyright 2006, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.as.controller.client;

import java.io.IOException;
import java.net.InetAddress;
import java.net.URISyntaxException;

import org.jboss.as.protocol.mgmt.ManagementClientChannelStrategy;
import org.jboss.remoting3.Endpoint;

/**
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 * @version $Revision: 1.1 $
 */
class NewEstablishChannelModelControllerClient extends NewAbstractModelControllerClient {
    private final Endpoint endpoint;
    private final String hostName;
    private final int port;

    public NewEstablishChannelModelControllerClient(final String hostName, int port){
        this(null, hostName, port);
    }

    public NewEstablishChannelModelControllerClient(final InetAddress address, int port){
        this(null, address, port);
    }

    public NewEstablishChannelModelControllerClient(final Endpoint endpoint, final String hostName, int port){
        this.endpoint = endpoint;
        this.hostName = hostName;
        this.port = port;
    }

    public NewEstablishChannelModelControllerClient(final Endpoint endpoint, final InetAddress address, int port){
        this.endpoint = endpoint;
        this.hostName = address.getHostName();
        this.port = port;
    }

    @Override
    ManagementClientChannelStrategy getClientChannelStrategy() throws URISyntaxException, IOException{
        if (endpoint == null) {
            return ManagementClientChannelStrategy.create(hostName, port, executor, this);
        } else {
            return ManagementClientChannelStrategy.create(hostName, port, endpoint, this);
        }
    }
}
