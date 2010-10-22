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

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

/**
 * DomainClientProtocol header used for management operation responses. Provides the default header fields from
 * {@link ManagementProtocolHeader}.
 *
 * @author John Bailey
 */
public class ManagementResponseHeader extends ManagementProtocolHeader {
    private int responseId;

    /**
     * Construct a new instance and read the header information from the input provided.
     *
     * @param input The input to read the header information from
     * @throws IOException If any problem occur reading from the input
     * @throws ManagementException If any information read is invalid.
     */
    public ManagementResponseHeader(final DataInput input) throws IOException, ManagementException {
        super(input);
    }

    /**
     * Construct an instance with the protocol version for the header.
     *
     * @param version The protocol version
     * @param responseId The response id
     */
    public ManagementResponseHeader(final int version, final int responseId) {
        super(version);
        this.responseId = responseId;
    }

    public void read(final DataInput input) throws IOException, ManagementException {
        super.read(input);
        this.responseId = input.readInt();
    }

    public void write(DataOutput output) throws IOException, ManagementException {
        super.write(output);
        output.writeInt(responseId);
    }

    /**
     * The response id.  This should correspond to the id of the request.
     *
     * @return The responseId
     */
    public int getResponseId() {
        return responseId;
    }
}
