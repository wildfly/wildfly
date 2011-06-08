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

    private ManagementRequestContext context;

    /**
     * Get the request context
     *
     * @return the context
     */
    public ManagementRequestContext getContext() {
        return context;
    }

    /**
     * Set the context
     *
     * @param context the context to set
     */
    public void setContext(ManagementRequestContext context) {
        this.context = context;
    }

    /**
     * Read the request for this management request
     *
     * @param input the data input
     */
    protected abstract void readRequest(DataInput input) throws IOException;

    /**
     * Write the response for this management response
     *
     * @param output the data output
     */
    protected abstract void writeResponse(FlushableDataOutput output) throws IOException;
}
