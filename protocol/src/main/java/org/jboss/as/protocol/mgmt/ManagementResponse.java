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

package org.jboss.as.protocol.mgmt;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.jboss.as.protocol.ByteDataOutput;
import org.jboss.as.protocol.Connection;
import org.jboss.as.protocol.MessageHandler;
import static org.jboss.as.protocol.ProtocolUtils.expectHeader;
import org.jboss.as.protocol.SimpleByteDataOutput;
import static org.jboss.as.protocol.StreamUtils.safeClose;

/**
 * Base class for management operations that are performed by reading a request and sending a response.
 *
 * @author John Bailey
 */
public abstract class ManagementResponse extends AbstractMessageHandler {

    private final MessageHandler finalMessageHandler;

    protected ManagementResponse() {
        this(MessageHandler.NULL);
    }

    protected ManagementResponse(final MessageHandler finalMessageHandler) {
        if (finalMessageHandler == null)
            throw new IllegalArgumentException("finalMessageHandler is null");
        this.finalMessageHandler = finalMessageHandler;
    }

    /** {@inheritDoc} */
    @Override
    public void handle(final Connection connection, final InputStream input) throws IOException {
        connection.setMessageHandler(requestBodyHandler);
        expectHeader(input, ManagementProtocol.REQUEST_START);
    }

    /**
     * Get the expected response code.
     *
     * @return The response code this operation expects
     */
    protected abstract byte getResponseCode();

    /**
     * Read the request information.
     *
     * @param input The request input
     * @throws IOException If any problems occur reading the request information
     */
    protected void readRequest(final InputStream input) throws IOException {
    }

    /**
     * Write the response information.
     *
     * @param output The output
     * @throws IOException If any problems occur writing the response information
     */
    protected void sendResponse(final OutputStream output) throws IOException {
    }

    final MessageHandler requestBodyHandler = new ReportExceptionHandler() {
        @Override
        public final void handle(final Connection connection, final InputStream input) throws IOException {
            connection.setMessageHandler(requestEndHandler);
            expectHeader(input, ManagementProtocol.REQUEST_BODY);
            readRequest(input);
        }
    };

    final MessageHandler requestEndHandler = new AbstractMessageHandler() {
        @Override
        public final void handle(final Connection connection, final InputStream input) throws IOException {
            connection.setMessageHandler(finalMessageHandler);
            try {
                expectHeader(input, ManagementProtocol.REQUEST_END);
            }
            catch (IOException e) {
                writeExceptionHeader(connection);
                throw e;
            }

            OutputStream outputStream = null;
            ByteDataOutput output = null;
            try {
                outputStream = connection.writeMessage();
                output = new SimpleByteDataOutput(outputStream);
                output.writeByte(ManagementProtocol.RESPONSE_START);
                output.writeByte(getResponseCode());
                output.close();
                outputStream.close();
            } finally {
                safeClose(output);
                safeClose(outputStream);
            }

            try {
                outputStream = connection.writeMessage();
                outputStream.write(ManagementProtocol.RESPONSE_BODY);
                sendResponse(outputStream);
                outputStream.close();
            } finally {
                safeClose(outputStream);
            }

            try {
                outputStream = connection.writeMessage();
                outputStream.write(ManagementProtocol.RESPONSE_END);
                outputStream.close();
            } finally {
                safeClose(outputStream);
            }
        }
    };

    private abstract class ReportExceptionHandler extends AbstractMessageHandler {

        @Override
        public void handleMessage(Connection connection, InputStream inputStream) throws IOException {
            try {
                super.handleMessage(connection, inputStream);
            }
            catch (IOException e) {
                writeExceptionHeader(connection);
                throw e;
            }
            catch (RuntimeException e) {
                writeExceptionHeader(connection);
                throw e;
            }
        }

    }

    private static void writeExceptionHeader(Connection connection) throws IOException {

        OutputStream outputStream = null;
        try {
            outputStream = connection.writeMessage();
            outputStream.write(ManagementProtocol.REMOTE_EXCEPTION);
            outputStream.close();
        } catch (Exception e) {
            ManagementResponse.log.errorf(e, "Failed to write REMOTE_EXCEPTION response header to notify requestor of exception on this side");
        }
        finally {
            safeClose(outputStream);
        }
    }
}
