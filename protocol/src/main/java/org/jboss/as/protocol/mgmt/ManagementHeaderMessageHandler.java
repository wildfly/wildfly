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

import static org.jboss.as.protocol.StreamUtils.safeClose;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.jboss.as.protocol.ByteDataInput;
import org.jboss.as.protocol.ByteDataOutput;
import org.jboss.as.protocol.Connection;
import org.jboss.as.protocol.MessageHandler;
import org.jboss.as.protocol.SimpleByteDataInput;
import org.jboss.as.protocol.SimpleByteDataOutput;

/**
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 * @version $Revision: 1.1 $
 */
public abstract class ManagementHeaderMessageHandler extends AbstractMessageHandler {

    @Override
    public void handle(Connection connection, InputStream dataStream) throws IOException {
        final ManagementRequestHeader requestHeader;
        final MessageHandler handler;
        ByteDataInput input = null;
        int workingVersion = 1;
        int responseId = ManagementProtocol.REMOTE_EXCEPTION;
        try {
            input = new SimpleByteDataInput(dataStream);

            // Start by reading the request header
            requestHeader = new ManagementRequestHeader(input);

            // Work with the lowest protocol version
            workingVersion = Math.min(ManagementProtocol.VERSION, requestHeader.getVersion());

            byte handlerId = requestHeader.getOperationHandlerId();
            if (handlerId == -1) {
                throw new IOException("Management request failed.  Invalid handler id");
            }
            handler = getHandlerForId(handlerId);
            if (handler == null) {
                String msg = "Management request failed.  No handler found for id " + handlerId;
                throw new IOException(msg);
            }
            connection.setMessageHandler(handler);
            responseId = requestHeader.getRequestId();
        } catch (IOException e) {
            throw e;
        } catch (Throwable t) {
            throw new IOException("Failed to read request header", t);
        } finally {
            safeClose(input);
            safeClose(dataStream);
            writeResponseHeader(connection, workingVersion, responseId);
        }
    }

    private void writeResponseHeader(Connection connection, int version, int responseId) throws IOException {
        OutputStream dataOutput = null;
        ByteDataOutput output = null;
        try {
            dataOutput = connection.writeMessage();
            output = new SimpleByteDataOutput(dataOutput);

            // Now write the response header
            final ManagementResponseHeader responseHeader = new ManagementResponseHeader(version, responseId);
            responseHeader.write(output);

            output.close();
            dataOutput.close();
        } catch (IOException e) {
            throw e;
        } catch (Throwable t) {
            throw new IOException("Failed to write management response headers", t);
        } finally {
            safeClose(output);
            safeClose(dataOutput);
        }
    }

    protected abstract MessageHandler getHandlerForId(byte handlerId);

}
