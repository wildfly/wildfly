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
package org.jboss.as.protocol.mgmt;

import java.io.DataInput;
import java.io.IOException;

/**
 * Base class for handling a management request
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 * @version $Revision: 1.1 $
 */
public abstract class ManagementRequestHandler {

    private volatile ManagementChannel channel;
    private volatile ManagementRequestHeader requestHeader;

    void setContextInfo(ManagementChannel channel, ManagementRequestHeader requestHeader) {
        this.channel = channel;
        this.requestHeader = requestHeader;
    }

    /**
     * Read the request body for this management request
     *
     * @param input the data input
     */
    protected void readRequest(DataInput input) throws IOException {
    }

    /**
     * Do the work for the request here while not using any in/output
     */
    protected void processRequest() throws RequestProcessingException {

    }

    /**
     * Write the response body for this management response
     *
     * @param output the data output
     */
    protected void writeResponse(FlushableDataOutput output) throws IOException {
    }


    /**
     * Get the channel
     * @return the channel
     */
    public ManagementChannel getChannel() {
        return channel;
    }

    /**
     * Get the header
     * @return the header
     */
    public ManagementRequestHeader getHeader() {
        return requestHeader;
    }
}
