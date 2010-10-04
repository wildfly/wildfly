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
import java.net.InetAddress;
import java.util.concurrent.ScheduledExecutorService;
import static org.jboss.as.server.manager.management.ManagementUtils.expectHeader;

/**
 * Base class for management operations that are performed by sending a request and reading a response.
 *
 * @author John Bailey
 */
public abstract class AbstractManagementRequest<T> extends ManagementRequest<T> {

    protected AbstractManagementRequest(InetAddress address, int port, int connectionRetryLimit, long connectionRetryInterval, long connectTimeout, ScheduledExecutorService executorService) {
        super(address, port, connectionRetryLimit, connectionRetryInterval, connectTimeout, executorService);
    }

    public final T execute(final int protocolVersion, final ByteDataOutput output, final ByteDataInput input) throws ManagementException {
        try {
            // First send request
            output.writeByte(ManagementProtocol.REQUEST_OPERATION);
            output.writeByte(getRequestCode());
            output.writeByte(ManagementProtocol.REQUEST_START);
            sendRequest(protocolVersion, output);
            output.writeByte(ManagementProtocol.REQUEST_END);
            output.flush();

            // Now process the response
            expectHeader(input, ManagementProtocol.RESPONSE_START);
            byte responseCode = input.readByte();
            if (responseCode != getResponseCode()) {
                throw new ManagementException("Invalid response code.  Expecting '" + getResponseCode() + "' received '" + responseCode + "'");
            }
            final T result = receiveResponse(protocolVersion, input);
            expectHeader(input, ManagementProtocol.RESPONSE_END);
            return result;
        } catch (IOException e) {
            throw new ManagementException("Failed to execute remote domain controller operation", e);
        }
    }

    protected abstract byte getRequestCode();

    protected abstract byte getResponseCode();

    protected void sendRequest(final int protocolVersion, final ByteDataOutput output) throws ManagementException {
    }

    protected T receiveResponse(final int protocolVersion, final ByteDataInput input) throws ManagementException {
        return null;
    }
}
