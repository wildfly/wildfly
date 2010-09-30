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
 * Management operation interface used to establish a general contract for operation handlers to conform to.
 *
 * @author John Bailey
 */
public interface ManagementOperation {
    /**
     * Code to identify this operation
     *
     * @return The request code
     */
    byte getRequestCode();

    /**
     * Handle the request using the provided input and output
     *
     * @param input The request input
     * @param output The request output
     * @throws ManagementException If any problems occur handling the request
     */
    void handle(final ByteDataInput input, final ByteDataOutput output) throws ManagementException;
}
