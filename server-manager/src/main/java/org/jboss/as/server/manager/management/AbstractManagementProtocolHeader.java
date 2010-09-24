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
import java.util.Arrays;
import static org.jboss.as.server.manager.management.ManagementProtocolUtils.expectHeader;

/**
 * Abstract base class used for {@link org.jboss.as.server.manager.management.ManagementProtocolHeader}
 * implementations.
 *
 * @author John Bailey
 */
public abstract class AbstractManagementProtocolHeader implements ManagementProtocolHeader {

    protected int version;

    /**
     * Construct a new instance without setting the version.  Should be used if the input is not available at the time
     * of creation.
     */
    protected AbstractManagementProtocolHeader() {
    }

    /**
     * Construct a new instance and read the header information from the input provided.
     *
     * @param input The input to read the header information from
     * @throws IOException If any problem occur reading from the input
     * @throws ManagementOperationException If any information read is invalid.
     */
    protected AbstractManagementProtocolHeader(final DataInput input) throws IOException, ManagementOperationException {
        read(input);
    }

    /**
     * Construct an instance with the protocol version for the header.
     *
     * @param version The protocol version
     */
    protected AbstractManagementProtocolHeader(int version) {
        this.version = version;
    }

    /** {@inheritDoc} */
    public void read(final DataInput input) throws IOException, ManagementOperationException {
        validateSignature(input);
        expectHeader(input, ManagementProtocol.VERSION_FIELD);
        setVersion(input.readInt());
    }

    /** {@inheritDoc} */
    public void write(final DataOutput output) throws IOException, ManagementOperationException {
        output.write(ManagementProtocol.SIGNATURE);
        output.writeByte(ManagementProtocol.VERSION_FIELD);
        output.writeInt(getVersion());
    }

    /** {@inheritDoc} */
    public int getVersion() {
        return version;
    }

    /** {@inheritDoc} */
    protected void setVersion(final int version) {
        this.version = version;
    }


    /**
     * Validate the header signature.
     *
     * @param input The input to read the signature from
     * @throws IOException If any read problems occur
     * @throws ManagementOperationException If the signature information is invalid
     */
    protected void validateSignature(final DataInput input) throws IOException, ManagementOperationException {
        final byte[] signatureBytes = new byte[4];
        input.readFully(signatureBytes);
        if (!Arrays.equals(ManagementProtocol.SIGNATURE, signatureBytes)) {
            throw new ManagementOperationException("Invalid signature [" + Arrays.toString(signatureBytes) + "]");
        }
    }
}
