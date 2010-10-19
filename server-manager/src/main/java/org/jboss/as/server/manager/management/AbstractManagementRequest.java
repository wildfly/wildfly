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
import java.net.InetAddress;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import org.jboss.as.protocol.ByteDataInput;
import org.jboss.as.protocol.ByteDataOutput;

import org.jboss.as.protocol.Connection;
import org.jboss.as.protocol.SimpleByteDataInput;
import org.jboss.as.protocol.SimpleByteDataOutput;
import static org.jboss.as.protocol.StreamUtils.safeClose;
import static org.jboss.as.server.manager.management.ManagementUtils.expectHeader;

/**
 * Base class for management operations that are performed by sending a request and reading a response.
 *
 * @author John Bailey
 */
public abstract class AbstractManagementRequest<T> extends ManagementRequest<T> {

    protected AbstractManagementRequest(final InetAddress address, final int port, final long connectTimeout, final ScheduledExecutorService executorService, final ThreadFactory threadFactory) {
        super(address, port, connectTimeout, executorService, threadFactory);
    }

    protected void sendRequest(final int protocolVersion, final Connection connection) throws ManagementException {
        OutputStream outputStream = null;
        ByteDataOutput output = null;
        try {
            outputStream = connection.writeMessage();
            output = new SimpleByteDataOutput(outputStream);
            // First send request
            output.writeByte(ManagementProtocol.REQUEST_OPERATION);
            output.writeByte(getRequestCode());
            output.writeByte(ManagementProtocol.REQUEST_START);
            sendRequest(protocolVersion, output);
            output.writeByte(ManagementProtocol.REQUEST_END);
        } catch (ManagementException e) {
            throw e;
        } catch (Throwable t) {
            throw new ManagementException("Failed to send management request", t);
        } finally {
            safeClose(output);
            safeClose(outputStream);
        }
    }

    protected abstract byte getRequestCode();

    protected abstract byte getResponseCode();

    protected void sendRequest(final int protocolVersion, final ByteDataOutput output) throws ManagementException {
    }

    protected T receiveResponse(Connection connection, InputStream dataStream) throws ManagementException {
        final ByteDataInput input = new SimpleByteDataInput(dataStream);
        try {
            // Now process the response
            expectHeader(input, ManagementProtocol.RESPONSE_START);
            byte responseCode = input.readByte();
            if (responseCode != getResponseCode()) {
                throw new ManagementException("Invalid response code.  Expecting '" + getResponseCode() + "' received '" + responseCode + "'");
            }
            final T result = receiveResponse(input);
            expectHeader(input, ManagementProtocol.RESPONSE_END);
            return result;
        } catch(ManagementException e) {
            throw e;
        } catch (Throwable t) {
            throw new ManagementException("Failed to receive management response", t);
        }
    }

    protected T receiveResponse(final ByteDataInput input) throws ManagementException {
        return null;
    }
}
