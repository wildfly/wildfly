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
import java.util.Arrays;
import static org.jboss.as.protocol.ProtocolUtils.expectHeader;

/**
 * DomainClientProtocol header used to send the required information to establish a request with a remote host controller.  The primary
 * pieces of the request are the protocol signature and the protocol version being used.
 *
 * @author John Bailey
 */
public abstract class ManagementProtocolHeader {

    protected int version;

    /**
     * Construct a new instance without setting the version.  Should be used if the input is not available at the time
     * of creation.
     */
    protected ManagementProtocolHeader() {
    }

    /**
     * Construct a new instance and read the header information from the input provided.
     *
     * @param input The input to read the header information from
     * @throws IOException If any problem occur reading from the input
     */
    protected ManagementProtocolHeader(final DataInput input) throws IOException {
        read(input);
    }

    /**
     * Construct an instance with the protocol version for the header.
     *
     * @param version The protocol version
     */
    protected ManagementProtocolHeader(int version) {
        this.version = version;
    }

    /**
     * Read the header information from the provided {@link java.io.DataInput}.
     *
     * @param input The input to read from
     * @throws IOException If any problems occur reading from the input
     */
    public void read(final DataInput input) throws IOException {
        validateSignature(input);
        expectHeader(input, ManagementProtocol.VERSION_FIELD);
        this.version = input.readInt();
    }

    /**
     * Write the header information to the provided {@link java.io.DataOutput}.
     *
     * @param output The output to write to
     * @throws IOException If any problems occur writing to the output
     */
    public void write(final DataOutput output) throws IOException {
        output.write(ManagementProtocol.SIGNATURE);
        output.writeByte(ManagementProtocol.VERSION_FIELD);
        output.writeInt(getVersion());
    }

    /**
     * The protocol version for the current communication.
     *
     * @return The protocol version
     */
    public int getVersion() {
        return version;
    }

    /**
     * Validate the header signature.
     *
     * @param input The input to read the signature from
     * @throws IOException If any read problems occur
     */
    protected void validateSignature(final DataInput input) throws IOException {
        final byte[] signatureBytes = new byte[4];
        input.readFully(signatureBytes);
        if (!Arrays.equals(ManagementProtocol.SIGNATURE, signatureBytes)) {
            throw new IOException("Invalid signature [" + Arrays.toString(signatureBytes) + "]");
        }
    }
}
