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

/**
 * Interface for handling management operations coming into a server manager process.  Each handler
 * is identified by a single byte that will be used to route the operation request to the correct handler.
 *
 * @author John Bailey
 */
public interface ManagementOperationHandler {

    /**
     * The identifier for this handler.
     *
     * @return the identifier
     */
    byte getIdentifier();

    /**
     * Handle the incoming management operation.
     *
     * @param protocolVersion The working protocol version
     * @param input  The request input
     * @param output The request output
     * @throws ManagementException If any problems occur performing the operation
     */
    void handleRequest(final int protocolVersion, final ByteDataInput input, final ByteDataOutput output) throws ManagementException;
}
