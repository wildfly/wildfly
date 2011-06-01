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
import java.util.concurrent.atomic.AtomicBoolean;

import org.jboss.remoting3.Channel;
import org.jboss.remoting3.Channel.Receiver;
import org.jboss.remoting3.MessageInputStream;
import org.xnio.IoUtils;

/**
 * Reads messages off a {@link Channel}.
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 * @version $Revision: 1.1 $
 */
public abstract class ProtocolChannelReceiver implements Receiver {
    private volatile ProtocolChannel channel;
    private final AtomicBoolean stopped = new AtomicBoolean();

    protected ProtocolChannelReceiver() {
    }

    void setChannel(ProtocolChannel channel) {
        this.channel = channel;
    }

    protected ProtocolChannel getChannel() {
        return channel;
    }

    @Override
    public void handleError(Channel channel, IOException error) {
        //TODO handle this?
        error.printStackTrace();
    }

    @Override
    public void handleEnd(Channel channel) {
        //TODO handle this?
    }

    void receive() {
        channel.receiveMessage(ProtocolChannelReceiver.this);
    }

    public void stop() {
        stopped.set(true);
        try {
            channel.writeShutdown();
        } catch (IOException e) {
        }
    }

    @Override
    public void handleMessage(final Channel channel, final MessageInputStream message) {
        channel.receiveMessage(this);
        boolean ok = false;
        try {
            doHandle(channel, message);
            ok = true;
        } finally {
            if (stopped.get()) {
                IoUtils.safeClose(channel);
            }
        }
    }

    protected abstract void doHandle(final Channel channel, final MessageInputStream message);
}
