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

package org.jboss.as.process.protocol;

import java.io.IOException;
import java.io.InputStream;

/**
 * A message handler for asynchronous protocol messages.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public interface MessageHandler {

    /**
     * Handle a message.
     *
     * @param connection the connection to the remote side
     * @param dataStream the message bytes
     * @throws IOException if an I/O error occurs
     */
    void handleMessage(Connection connection, InputStream dataStream) throws IOException;

    /**
     * Handle the end-of-input condition.  The write side will still be writable.
     *
     * @param connection the connection to the remote side
     * @throws IOException if an I/O error occurs
     */
    void handleShutdown(Connection connection) throws IOException;

    /**
     * Handle an input failure condition.  The write side is unlikely to be writable.  When a
     * failure is encountered, the socket should usually be {@link Connection#close()}d.
     *
     * @param connection the connection to the remote side
     * @param e the read error received
     * @throws IOException if an I/O error occurs
     */
    void handleFailure(Connection connection, IOException e) throws IOException;

    /**
     * Handle the condition where a connection is completely finished (both reads and writes).  This is where any
     * reconnect logic should live.
     *
     * @param connection the connection that ended
     * @throws IOException if an I/O error occurs
     */
    void handleFinished(Connection connection) throws IOException;

    MessageHandler NULL = new MessageHandler() {
        public void handleMessage(final Connection connection, final InputStream dataStream) throws IOException {
            dataStream.close();
        }

        public void handleShutdown(final Connection connection) throws IOException {
        }

        public void handleFailure(final Connection connection, final IOException e) throws IOException {
        }

        public void handleFinished(final Connection connection) throws IOException {
        }
    };
}
