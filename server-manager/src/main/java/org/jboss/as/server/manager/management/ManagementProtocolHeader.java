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
 * Protocol header used to send the required information to establish a request with a remote server manager.  The primary
 * pieces of the request are the protocol signature and the protocol version being used.
 *
 * @author John Bailey
 */
public interface ManagementProtocolHeader {
    /**
     * Read the header information from the provided {@link java.io.DataInput}.
     *
     * @param input The input to read from
     * @throws IOException If any problems occur reading from the input
     * @throws ManagementOperationException If any of the input values are incorrect
     */
    void read(final DataInput input) throws IOException, ManagementOperationException;

    /**
     * Write the header information to the provided {@link java.io.DataOutput}.
     *
     * @param output The output to write to
     * @throws IOException If any problems occur writing to the output
     * @throws ManagementOperationException If any of the output values are incorrect
     */
    void write(final DataOutput output) throws IOException, ManagementOperationException;

    /**
     * The protocol version for the current communication.
     *
     * @return The protocol version
     */
    int getVersion();
}
