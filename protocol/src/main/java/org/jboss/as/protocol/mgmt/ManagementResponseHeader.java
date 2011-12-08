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

import static org.jboss.as.protocol.ProtocolMessages.MESSAGES;

/**
 * ManagementProtocol header used for management operation responses. Provides the default header fields from
 * {@link ManagementProtocolHeader}.
 *
 * @author John Bailey
 * @author Kabir Khan
 */
public class ManagementResponseHeader extends ManagementProtocolHeader {

    private int responseId;
    private boolean failed = false;
    private String error;

    /**
     * Construct an instance with the protocol version for the header.
     *
     * @param version The protocol version
     * @param responseId The response id
     */
    public ManagementResponseHeader(final int version, final int responseId, final String error) {
        super(version);
        this.responseId = responseId;
        this.error = error;
        this.failed = error != null;
    }

    ManagementResponseHeader(final int version, final DataInput input) throws IOException {
        super(version);
        read(input);
    }

    public void read(final DataInput input) throws IOException {
        ProtocolUtils.expectHeader(input, ManagementProtocol.RESPONSE_ID);
        this.responseId = input.readInt();
        ProtocolUtils.expectHeader(input, ManagementProtocol.RESPONSE_TYPE);
        byte type = input.readByte();
        if (type == ManagementProtocol.RESPONSE_ERROR) {
            this.failed = true;
            error = input.readUTF();
        } else if (type != ManagementProtocol.RESPONSE_BODY) {
            throw MESSAGES.invalidType("RESPONSE_ERROR", "RESPONSE_BODY", type);
        }
    }

    public void write(DataOutput output) throws IOException {
        super.write(output);
        output.write(ManagementProtocol.RESPONSE_ID);
        output.writeInt(responseId);
        output.write(ManagementProtocol.RESPONSE_TYPE);
        if (error != null) {
            output.write(ManagementProtocol.RESPONSE_ERROR);
            output.writeUTF(error);
        } else {
            output.write(ManagementProtocol.RESPONSE_BODY);
        }
    }

    /**
     * Whether this is an error response.
     *
     * @return {@code true} if the request failed, {@code false} otherwise
     */
    public boolean isFailed() {
        return failed;
    }

    /**
     * The response id.  This should correspond to the id of the request.
     *
     * @return The responseId
     */
    public int getResponseId() {
        return responseId;
    }

    /**
     * Gets any error that happened on the server trying to initialize the request
     *
     * @return the error
     */
    public String getError() {
        return error;
    }

    @Override
    public byte getType() {
        return ManagementProtocol.TYPE_RESPONSE;
    }

    public static ManagementResponseHeader create(final ManagementProtocolHeader header) {
        return create(ManagementRequestHeader.class.cast(header));
    }

    public static ManagementResponseHeader create(final ManagementRequestHeader header) {
        final int workingVersion = Math.min(ManagementProtocol.VERSION, header.getVersion());
        return new ManagementResponseHeader(workingVersion, header.getRequestId(), null);
    }

    public static ManagementResponseHeader create(final ManagementRequestHeader header, Exception error) {
        final int workingVersion = Math.min(ManagementProtocol.VERSION, header.getVersion());
        return new ManagementResponseHeader(workingVersion, header.getRequestId(), error != null ? error.getMessage() : null);
    }

}
