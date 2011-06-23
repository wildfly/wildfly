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

import org.jboss.remoting3.HandleableCloseable.Key;

/**
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 * @version $Revision: 1.1 $
 */
public abstract class ManagementResponseHandler<T> {

    public static final ManagementResponseHandler<Void> EMPTY_RESPONSE = new ManagementResponseHandler<Void>() {
        @Override
        protected Void readResponse(DataInput input) throws IOException {
            return null;
        }
    };

    private volatile ManagementResponseHeader responseHeader;

    private volatile ManagementChannel channel;

    private volatile Key closeKey;

    void setCloseKey(Key closeKey) {
        this.closeKey = closeKey;
    }

    void removeCloseHandler() {
        if (closeKey != null) {
            closeKey.remove();
        }
    }

    void setContextInfo(ManagementResponseHandler<?> other) {
        this.closeKey = other.closeKey;
        setContextInfo(other.responseHeader, other.channel);
    }

    void setContextInfo(ManagementResponseHeader responseHeader, ManagementChannel channel) {
        this.responseHeader = responseHeader;
        this.channel = channel;
    }

    protected ManagementResponseHeader getResponseHeader() {
        return responseHeader;
    }

    protected ManagementChannel getChannel() {
        return channel;
    }

    /**
     * Read the response body
     *
     * @param input the input
     */
    protected abstract T readResponse(DataInput input) throws IOException;
}
