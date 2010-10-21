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

import java.io.IOException;
import java.io.InputStream;

import org.jboss.as.protocol.Connection;
import org.jboss.as.protocol.MessageHandler;
import static org.jboss.as.protocol.StreamUtils.safeClose;
import org.jboss.logging.Logger;

/**
 * Abstract message handler use to provide default implementations for management message handlers.
 *
 * @author John Bailey
 */
public abstract class AbstractMessageHandler implements MessageHandler {
    private static final Logger log = Logger.getLogger("org.jboss.as.management");

    /** {@inheritDoc} */
    public void handleShutdown(Connection connection) throws IOException {
        connection.shutdownWrites();
    }

    /** {@inheritDoc} */
    public void handleFailure(Connection connection, IOException e) throws IOException {
        log.error(e);
        connection.shutdownWrites();
    }

    /** {@inheritDoc} */
    public void handleFinished(Connection connection) throws IOException {
    }

    /** {@inheritDoc} */
    public void handleMessage(final Connection connection, final InputStream inputStream) throws IOException {
        try {
            handle(connection, inputStream);
        } catch (Exception e) {
            throw new IOException("Failed to handle management operation", e);
        } finally {
            safeClose(inputStream);
        }
    }

    /**
     * Handle the request using the provided input and output
     *
     * @param connection The connection
     * @param inputStream The request input
     * @throws Exception If any problems occur handling the request
     */
    public abstract void handle(final Connection connection, final InputStream inputStream) throws IOException;
}
