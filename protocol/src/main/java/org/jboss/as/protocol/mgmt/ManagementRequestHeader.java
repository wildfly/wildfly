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

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

/**
 * ManagementProtocol header used for management requests.  Provides the default header fields from
 * {@link ManagementProtocolHeader} as well as a field to identify who the
 * request should be handled by.
 *
 * @author John Bailey
 * @author Kabir Khan
 */
public class ManagementRequestHeader extends ManagementProtocolHeader {

    private int requestId;
    private int batchId;
    private byte operationId;
    // Actually not needed
    private boolean oneWay;

    /**
     * Construct an instance with the protocol version and operation handler for the header.
     *
     * @param version The protocol version
     * @param requestId The request id
     * @param batchId The batch id
     * @param operationId The operation to invoke on the server
     */
    public ManagementRequestHeader(final int version, final  int requestId, final int batchId, final byte operationId) {
        super(version);
        this.requestId = requestId;
        this.batchId = batchId;
        this.operationId = operationId;
    }

    ManagementRequestHeader(final int version, final DataInput input) throws IOException {
        super(version);
        read(input);
    }

    /** {@inheritDoc} */
    public void read(final DataInput input) throws IOException {
        ProtocolUtils.expectHeader(input, ManagementProtocol.REQUEST_ID);
        requestId = input.readInt();
        ProtocolUtils.expectHeader(input, ManagementProtocol.BATCH_ID);
        batchId = input.readInt();
        ProtocolUtils.expectHeader(input, ManagementProtocol.OPERATION_ID);
        operationId = input.readByte();
        ProtocolUtils.expectHeader(input, ManagementProtocol.ONE_WAY);
        oneWay = input.readBoolean();
        ProtocolUtils.expectHeader(input, ManagementProtocol.REQUEST_BODY);
    }

    /** {@inheritDoc} */
    public void write(final DataOutput output) throws IOException {
        super.write(output);
        output.write(ManagementProtocol.REQUEST_ID);
        output.writeInt(requestId);
        output.write(ManagementProtocol.BATCH_ID);
        output.writeInt(batchId);
        output.write(ManagementProtocol.OPERATION_ID);
        output.write(operationId);
        output.write(ManagementProtocol.ONE_WAY);
        output.writeBoolean(oneWay);
        output.write(ManagementProtocol.REQUEST_BODY);
    }

    /**
     * The ID of this request.
     *
     * @return The request id
     */
    public int getRequestId() {
        return requestId;
    }

    /**
     * The ID of the batch this request belongs to
     *
     * @return the batch id
     */
    public int getBatchId() {
        return batchId;
    }

    /**
     * The id of the operation to be executed by this request
     *
     * @return the operation id;
     */
    public byte getOperationId() {
        return operationId;
    }

    @Override
    public byte getType() {
        return ManagementProtocol.TYPE_REQUEST;
    }
}
