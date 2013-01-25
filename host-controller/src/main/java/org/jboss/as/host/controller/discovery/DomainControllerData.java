/*
* JBoss, Home of Professional Open Source.
* Copyright 2013, Red Hat Middleware LLC, and individual contributors
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

package org.jboss.as.host.controller.discovery;

import java.io.DataInput;
import java.io.DataOutput;

/**
 * Encapsulates information about a domain controller (i.e., host and port).
 *
 * @author Farah Juma
 */
public class DomainControllerData {

    protected String host;
    protected int port;

    public DomainControllerData() {
    }

    /**
     * Create the DomainControllerData.
     *
     * @param host the host name of the domain controller
     * @param port the port number of the domain controller
     */
    public DomainControllerData(String host, int port) {
        this.host = host;
        this.port = port;
    }

    /**
     *  Gets the domain controller's host name.
     *
     *  @return the host name
     */
    public String getHost() {
        return host;
    }

    /**
     *  Gets the domain controller's port number.
     *
     *  @return the port number
     */
    public int getPort() {
        return port;
    }

    /**
     * Write the domain controller's data to an output stream.
     *
     * @param outstream the output stream
     * @throws Exception
     */
    public void writeTo(DataOutput outstream) throws Exception {
        S3Util.writeString(host, outstream);
        outstream.writeInt(port);
    }

    /**
     * Read the domain controller's data from an input stream.
     *
     * @param instream the input stream
     * @throws Exception
     */
    public void readFrom(DataInput instream) throws Exception {
        host = S3Util.readString(instream);
        port = instream.readInt();
    }

    public String toString() {
        StringBuilder sb=new StringBuilder();
        sb.append("master_host=" + getHost());
        sb.append(",master_port=" + getPort());
        return sb.toString();
    }
}
