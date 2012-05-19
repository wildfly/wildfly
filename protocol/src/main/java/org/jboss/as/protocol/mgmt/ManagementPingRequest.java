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

import static org.jboss.as.protocol.mgmt.ProtocolUtils.expectHeader;

import java.io.DataInput;
import java.io.IOException;

/**
 * {@link ManagementRequest} that sends a {@link ManagementProtocol#TYPE_PING} header.
 * Note that this is distinct from the top-level sending of {@link ManagementPingHeader} used
 * by legacy (community 7.0.x) clients.
 *
 * @author Brian Stansberry (c) 2012 Red Hat Inc.
 */
public class ManagementPingRequest extends AbstractManagementRequest<Long, Void> {

    public static final ManagementPingRequest INSTANCE = new ManagementPingRequest();

    @Override
    public byte getOperationType() {
        return ManagementProtocol.TYPE_PING;
    }

    @Override
    protected void sendRequest(ActiveOperation.ResultHandler<Long> resultHandler, ManagementRequestContext<Void> context, FlushableDataOutput output) throws IOException {
        // nothing besides the header
    }

    @Override
    public void handleRequest(DataInput input, ActiveOperation.ResultHandler<Long> resultHandler, ManagementRequestContext<Void> managementRequestContext) throws IOException {
        expectHeader(input, ManagementProtocol.TYPE_PONG);
        long instanceID = input.readLong();
        resultHandler.done(instanceID);
        expectHeader(input, ManagementProtocol.RESPONSE_END);
    }
}
