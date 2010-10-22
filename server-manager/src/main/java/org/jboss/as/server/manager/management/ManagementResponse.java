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

package org.jboss.as.server.manager.management;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import org.jboss.as.protocol.ByteDataOutput;

import org.jboss.as.protocol.Connection;
import org.jboss.as.protocol.MessageHandler;
import org.jboss.as.protocol.SimpleByteDataOutput;
import static org.jboss.as.protocol.StreamUtils.safeClose;
import static org.jboss.as.server.manager.management.ManagementUtils.expectHeader;

/**
 * Base class for management operations that are performed by reading a request and sending a response.
 *
 * @author John Bailey
 */
public abstract class ManagementResponse extends ManagementOperation {

    /** {@inheritDoc} */
    public void handle(final Connection connection, final InputStream input) throws ManagementException {
        try {
            expectHeader(input, ManagementProtocol.REQUEST_START);
            connection.setMessageHandler(requestBodyHandler);
        } catch (IOException e) {
            throw new ManagementException("Failed to read request.", e);
        }
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
     * @throws ManagementException If any problems occur reading the request information
     */
    protected void readRequest(final InputStream input) throws ManagementException {
    }

    /**
     * Write the response information.
     *
     * @param output The output
     * @throws ManagementException If any problems occur writing the response information
     */
    protected void sendResponse(final OutputStream output) throws IOException, ManagementException {
    }

    final MessageHandler requestBodyHandler = new AbstractMessageHandler() {
        final void handle(final Connection connection, final InputStream input) throws ManagementException {
            connection.setMessageHandler(requestEndHandler);
            readRequest(input);
        }
    };

    final MessageHandler requestEndHandler = new AbstractMessageHandler() {
        final void handle(final Connection connection, final InputStream input) throws ManagementException {
            try {
                expectHeader(input, ManagementProtocol.REQUEST_END);
            } catch (IOException e) {
                throw new ManagementException("Failed to read request end", e);
            }

            try {
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
                    sendResponse(outputStream);
                    outputStream.close();
                } finally {
                    safeClose(outputStream);
                }

                try {
                    outputStream = connection.writeMessage();
                    output = new SimpleByteDataOutput(outputStream);
                    output.writeByte(ManagementProtocol.RESPONSE_END);
                    output.close();
                    outputStream.close();
                } finally {
                    safeClose(output);
                    safeClose(outputStream);
                }
            } catch (IOException e) {
                throw new ManagementException("Failed to send response", e);
            }
        }
    };
}
