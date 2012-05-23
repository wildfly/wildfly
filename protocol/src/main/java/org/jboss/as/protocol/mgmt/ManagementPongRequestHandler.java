/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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

import org.jboss.as.protocol.StreamUtils;

/**
 * {@link ManagementRequestHandlerFactory} for dealing with a {@link ManagementPingRequest}.
 *
 * @author Brian Stansberry (c) 2012 Red Hat Inc.
 */
public class ManagementPongRequestHandler implements ManagementRequestHandlerFactory, ManagementRequestHandler<Void, Void> {

    private volatile long connectionId = System.currentTimeMillis();

    @Override
    public ManagementRequestHandler<?, ?> resolveHandler(RequestHandlerChain handlers, ManagementRequestHeader header) {
        final byte operationId = header.getOperationId();
        switch (operationId) {
            case ManagementProtocol.TYPE_PING:
                handlers.registerActiveOperation(header.getBatchId(), null);
                return this;
        }
        return handlers.resolveNext();
    }

    @Override
    public void handleRequest(final DataInput input, final ActiveOperation.ResultHandler<Void> resultHandler,
                              final ManagementRequestContext<Void> context) throws IOException {

        final ManagementResponseHeader response = ManagementResponseHeader.create(context.getRequestHeader());

        final FlushableDataOutput output = context.writeMessage(response);
        try {
            output.write(ManagementProtocol.TYPE_PONG);
            output.writeLong(connectionId);
            output.writeByte(ManagementProtocol.RESPONSE_END);
            output.close();
        } finally {
            StreamUtils.safeClose(output);
        }
        resultHandler.done(null);
    }

    /**
     * Update the id we send in pong responses
     */
    public void resetConnectionId() {
        connectionId = System.currentTimeMillis();
    }

    /**
     * Gets the current id we send in pong responses
     * @return
     */
    public long getConnectionId() {
        return connectionId;
    }
}
