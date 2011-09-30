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

import java.io.Closeable;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;

/**
 * A peer-to-peer connection with another participant in the management protocol.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public interface Connection extends Closeable {

    /**
     * Write a protocol message.  Returns a stream which can be written to, to transmit the
     * data.  When the stream is closed, the message is concluded.
     *
     * @return the stream to which the message should be written
     * @throws IOException if an I/O error occurs
     */
    OutputStream writeMessage() throws IOException;

    /**
     * Shut down writes once all messages are sent.  This will cause the reading side's {@link MessageHandler#handleShutdown(Connection)}
     * method to be called.
     *
     * @throws IOException if an I/O error occurs
     */
    void shutdownWrites() throws IOException;

    /**
     * Close the connection.  This will interrupt both reads and writes and so should only be
     * done in the event of an unrecoverable failure of the connection.
     *
     * @throws IOException if the close fails
     */
    void close() throws IOException;

    /**
     * Change the current message handler.
     *
     * @param messageHandler the new message handler to use
     */
    void setMessageHandler(MessageHandler messageHandler);

    /**
     * Get the remote peer address.
     *
     * @return the peer address
     */
    InetAddress getPeerAddress();

    void attach(Object attachment);

    Object getAttachment();

    /**
     * Records the current message handler, which can be reset using
     * {@link #restoreMessageHandler()}
     */
    void backupMessageHandler();

    /**
     * Resets the message handler to any that was backed up using
     * {@link #backupMessageHandler()}. If no backup was done, {@link MessageHandler#NULL}
     * is used
     */
    void restoreMessageHandler();


    /**
     * A callback that will be triggered once the connection is closed
     */
    public interface ClosedCallback {
        void connectionClosed();
    }
}
