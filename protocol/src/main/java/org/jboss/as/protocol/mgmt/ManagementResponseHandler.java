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
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 * @version $Revision: 1.1 $
 */
public abstract class ManagementResponseHandler<T> {

    private ManagementResponseContext context;

    /**
     * Get the response context. This is only available in the readResponse(DataInput) method.
     * @return the context
     * @throws IllegalStateException if an attempt is made to read it outside of the readResponse(DataInput) method.
     */
    protected ManagementResponseContext getResponseContext() {
        if (context == null) {
            throw new IllegalArgumentException("Only allowed from within readResponse()");
        }
        return context;
    }

    /**
     * Set the response context
     * @param context the context to set
     */
    void setResponseContext(ManagementResponseContext context) {
        this.context = context;
    }

    /**
     * Read the response body
     *
     * @param input the input
     */
    protected abstract T readResponse(DataInput input) throws IOException;
}
