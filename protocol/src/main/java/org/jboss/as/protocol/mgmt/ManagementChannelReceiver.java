/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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

import static org.jboss.as.protocol.ProtocolLogger.ROOT_LOGGER;
import org.jboss.as.protocol.StreamUtils;
import org.jboss.marshalling.Marshalling;
import org.jboss.marshalling.SimpleDataInput;
import org.jboss.remoting3.Channel;
import org.jboss.remoting3.MessageInputStream;
import org.jboss.remoting3.MessageOutputStream;

import java.io.DataInput;
import java.io.IOException;
import java.io.OutputStream;
import java.util.concurrent.TimeUnit;

/**
 * Base receiver class for the management protocol support.
 *
 * @author Emanuel Muckenhuber
 */
public abstract class ManagementChannelReceiver implements ManagementMessageHandler, Channel.Receiver {

    /**
     * Create a {@code ManagementChannelReceiver} which is delegating protocol messages to
     * a {@code ManagementMessageHandler}.
     *
     * @param handler the handler
     * @return the receiver
     */
    public static Channel.Receiver createDelegating(final ManagementMessageHandler handler) {
        assert handler != null;
        return new ManagementChannelReceiver() {
            @Override
            public void handleMessage(Channel channel, DataInput input, ManagementProtocolHeader header) throws IOException {
                handler.handleMessage(channel, input, header);
            }
        };
    }

    @Override
    public void handleMessage(final Channel channel, final MessageInputStream message) {
        try {
            ROOT_LOGGER.tracef("%s handling incoming data", this);
            final DataInput input = new SimpleDataInput(Marshalling.createByteInput(message));
            final ManagementProtocolHeader header = ManagementProtocolHeader.parse(input);
            final byte type = header.getType();
            if(type == ManagementProtocol.TYPE_PING) {
                // Handle legacy ping/pong directly
                ROOT_LOGGER.tracef("Received ping on %s", this);
                handlePing(channel, header);
            } else if (type == ManagementProtocol.TYPE_BYE_BYE) {
                // Close the channel
                ROOT_LOGGER.tracef("Received bye bye on %s, closing", this);
                handleChannelReset(channel);
            } else {
                // Handle a message
                handleMessage(channel, input, header);
            }
            message.close();
        } catch(IOException e) {
            handleError(channel, e);
        } catch (Exception e) {
            handleError(channel, new IOException(e));
        } finally {
            StreamUtils.safeClose(message);
            ROOT_LOGGER.tracef("%s done handling incoming data", this);
        }
        final Channel.Receiver next = next();
        if(next != null) {
            channel.receiveMessage(next);
        }
    }

    /**
     * Get the next receiver.
     *
     * @return the receiver
     */
    protected Channel.Receiver next() {
        return this;
    }

    @Override
    public void handleError(final Channel channel, final IOException error) {
        ROOT_LOGGER.tracef(error, "%s error handling incoming data", this);
        try {
            channel.close();
        } catch (IOException e) {
            ROOT_LOGGER.errorClosingChannel(e.getMessage());
        }
    }

    @Override
    public void handleEnd(final Channel channel) {
        try {
            channel.close();
        } catch (IOException e) {
            ROOT_LOGGER.errorClosingChannel(e.getMessage());
        }
    }

    /**
     * Handle the legacy bye-bye notification.
     *
     * @param channel the channel the bye-bye message was received
     */
    protected void handleChannelReset(Channel channel) {
        //
    }

    @Override
    public void shutdown() {
        //
    }

    @Override
    public void shutdownNow() {
        //
    }

    @Override
    public boolean awaitCompletion(long timeout, TimeUnit unit) throws InterruptedException {
        return false;
    }

    /**
     * Handle a simple ping request.
     *
     * @param channel the channel
     * @param header the protocol header
     * @throws IOException for any error
     */
    protected static void handlePing(final Channel channel, final ManagementProtocolHeader header) throws IOException {
        final ManagementProtocolHeader response = new ManagementPongHeader(header.getVersion());
        final MessageOutputStream output = channel.writeMessage();
        try {
            writeHeader(response, output);
            output.close();
        } finally {
            StreamUtils.safeClose(output);
        }
    }

    /**
     * Write the management protocol header.
     *
     * @param header the mgmt protocol header
     * @param os the output stream
     * @throws IOException
     */
    protected static void writeHeader(final ManagementProtocolHeader header, final OutputStream os) throws IOException {
        final FlushableDataOutput output = FlushableDataOutputImpl.create(os);
        header.write(output);
        output.flush();
    }

}
