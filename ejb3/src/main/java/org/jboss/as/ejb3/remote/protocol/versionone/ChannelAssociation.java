/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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

package org.jboss.as.ejb3.remote.protocol.versionone;

import org.jboss.remoting3.Channel;
import org.jboss.remoting3.MessageOutputStream;
import org.jboss.remoting3.RemotingOptions;

import java.io.IOException;
import java.util.concurrent.Semaphore;

/**
 * @author Jaikiran Pai
 */
public class ChannelAssociation {

    private final Channel channel;

    private static final int DEFAULT_MAX_OUTBOUND_MESSAGES = 80;

    // A semaphore which will be used to acquire a lock while writing out to a channel
    // to make sure that only a limited number of simultaneous writes are allowed
    private final Semaphore channelWriteSemaphore;

    public ChannelAssociation(final Channel channel) {
        this.channel = channel;

        // write semaphore
        Integer maxOutboundWrites = this.channel.getOption(RemotingOptions.MAX_OUTBOUND_MESSAGES);
        if (maxOutboundWrites == null) {
            maxOutboundWrites = DEFAULT_MAX_OUTBOUND_MESSAGES;
        }
        this.channelWriteSemaphore = new Semaphore(maxOutboundWrites, true);
    }

    /**
     * Opens and returns a new {@link org.jboss.remoting3.MessageOutputStream} for the {@link Channel} represented
     * by this {@link ChannelAssociation}. Before opening the message outputstream, this method acquires a permit
     * to make sure only a limited number of simultaneous writes are allowed on the channel. The permit acquistion
     * is a blocking wait.
     *
     * @return
     * @throws Exception
     */
    public MessageOutputStream acquireChannelMessageOutputStream() throws Exception {
        this.channelWriteSemaphore.acquire();
        try {
            return this.channel.writeMessage();
        } catch (Exception e) {
            // release
            this.channelWriteSemaphore.release();
            throw e;
        }
    }

    /**
     * Releases a previously held permit/lock on a message outputstream of a channel and also closes
     * the <code>messageOutputStream</code>
     *
     * @param messageOutputStream The message outputstream
     * @throws java.io.IOException
     */
    public void releaseChannelMessageOutputStream(final MessageOutputStream messageOutputStream) throws IOException {
        try {
            messageOutputStream.close();
        } finally {
            this.channelWriteSemaphore.release();
        }
    }

    public Channel getChannel() {
        return this.channel;
    }

}
