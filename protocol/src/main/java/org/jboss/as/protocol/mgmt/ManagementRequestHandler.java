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

import static org.jboss.as.protocol.ProtocolLogger.ROOT_LOGGER;
import static org.jboss.as.protocol.ProtocolMessages.MESSAGES;
import org.xnio.IoUtils;

import java.io.DataInput;
import java.io.DataOutput;
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
     * Check whether the response should be written after as part of the ManagementChannel.doHandle
     *
     * TODO We should rather use a sort of callback to determine when the processRequest() operation was finished
     *
     * @return
     */
    protected boolean isWriteResponse() {
        return true;
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

    protected void writeResponseHeader(final ManagementRequestHeader header, DataOutput output, Exception exception) throws IOException {
        final int workingVersion = Math.min(ManagementProtocol.VERSION, header.getVersion());
        try {
            // Now write the response header
            final ManagementResponseHeader responseHeader = new ManagementResponseHeader(workingVersion, header.getRequestId(), formatException(exception));
            responseHeader.write(output);
        } catch (IOException e) {
            throw e;
        } catch (Throwable t) {
            throw MESSAGES.failedToWriteManagementResponseHeaders(t);
        }
    }

    protected void writeMessageResponse(final ManagementWriteResponseCallback writerResponeCallback) {
        ROOT_LOGGER.tracef("%s writing response %d", channel, requestHeader.getBatchId());
        final FlushableDataOutputImpl output;
        try {
            output = FlushableDataOutputImpl.create(channel.writeMessage());
        } catch (Exception e) {
            ROOT_LOGGER.tracef(e, "%s could not open output stream for request %d", channel, requestHeader.getBatchId());
            return;
        }
        try {
            writeResponseHeader(requestHeader, output, null);
            writerResponeCallback.writeResponse(output);
            output.writeByte(ManagementProtocol.RESPONSE_END);
        } catch (Exception e) {
            ROOT_LOGGER.tracef(e, "%s finished writing response %d with error", channel, requestHeader.getBatchId());
        } finally {
            ROOT_LOGGER.tracef("%s finished writing response %d", channel, requestHeader.getBatchId());
            IoUtils.safeClose(output);
        }
    }

    private String formatException(Exception exception) {
        if (exception == null) {
            return null;
        }
        return exception.getMessage();
    }

    public static interface ManagementWriteResponseCallback {

        /**
        * Write the response body for this management response
        *
        * @param output the data output
        */
        void writeResponse(FlushableDataOutput output) throws IOException;

    }

}
