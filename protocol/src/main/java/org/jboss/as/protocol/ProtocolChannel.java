/*
* JBoss, Home of Professional Open Source.
* Copyright 2006, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.as.protocol;

import java.io.IOException;

import org.jboss.remoting3.Attachments;
import org.jboss.remoting3.Channel;
import org.jboss.remoting3.CloseHandler;
import org.jboss.remoting3.MessageOutputStream;

/**
 * A wrapper around the Channel to make sure the receiver only gets set once
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 * @version $Revision: 1.1 $
 */
public class ProtocolChannel implements Channel {
    private final String name;
    private final Channel channel;
    private final ProtocolChannelReceiver channelReceiver;
    private boolean start;

    private ProtocolChannel(String name, Channel channel, ProtocolChannelReceiver channelReceiver) {
        this.name = name;
        this.channel = channel;
        this.channelReceiver = channelReceiver;
    }

    public static ProtocolChannel create(String name, Channel channel, ProtocolChannelReceiverFactory factory) {
        ProtocolChannelReceiver receiver = factory.create();
        ProtocolChannel protocolChannel = new ProtocolChannel(name, channel, receiver);
        receiver.setChannel(protocolChannel);
        return protocolChannel;
    }

    public void startReceiving() {
        if (!start) {
            start = true;
        } else {
            throw new IllegalStateException("Channel and receiver already started");
        }

        channelReceiver.receive();
    }

    public String getName() {
        return name;
    }

    public <T extends ProtocolChannelReceiver> T getReceiver(Class<T> clazz) {
        return clazz.cast(channelReceiver);
    }

    /**
     * {@inheritDoc}
     */
    public Attachments getAttachments() {
        return channel.getAttachments();
    }

    /**
     * {@inheritDoc}
     */
    public void awaitClosed() throws InterruptedException {
        channel.awaitClosed();
    }

    /**
     * {@inheritDoc}
     */
    public MessageOutputStream writeMessage() throws IOException {
        return channel.writeMessage();
    }

    /**
     * {@inheritDoc}
     */
    public void awaitClosedUninterruptibly() {
        channel.awaitClosedUninterruptibly();
    }

    /**
     * {@inheritDoc}
     */
    public org.jboss.remoting3.HandleableCloseable.Key addCloseHandler(CloseHandler<? super Channel> handler) {
        return channel.addCloseHandler(handler);
    }

    /**
     * {@inheritDoc}
     */
    public void writeShutdown() throws IOException {
        channel.writeShutdown();
    }

    /**
     * {@inheritDoc}
     */
    public void receiveMessage(Receiver handler) {
        channel.receiveMessage(handler);
    }

    /**
     * {@inheritDoc}
     */
    public void close() throws IOException {
        channelReceiver.stop();
        channel.close();
    }
}
