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
import org.jboss.remoting3.Connection;
import org.jboss.remoting3.MessageInputStream;
import org.jboss.remoting3.MessageOutputStream;

import static org.jboss.as.protocol.ProtocolLogger.ROOT_LOGGER;
import static org.jboss.as.protocol.ProtocolMessages.MESSAGES;

/**
 * A wrapper around the {@link Channel} that handles repeated receives on the Channel.
 * The standard close, shutdownWrites and awaitClosed methods are hacked to work around
 * issues in remoting closing down and so try another approach.
 * TODO: we should not subclass {@link Channel}; that interface may change without notice.  Let's refactor
 * this to use composition sometime soon.
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 * @version $Revision: 1.1 $
 */
public abstract class ProtocolChannel implements Channel {

    private final String name;
    private final Channel channel;
    private boolean start;

    protected ProtocolChannel(String name, Channel channel) {
        this.name = name;
        this.channel = channel;
    }

    /**
     * TODO The ProtocolChannel most likely should not be needed
     * or at least not have to do anything with the remoting Channel
     * rather just the setup part
     *
     * @return
     */
    protected abstract Channel.Receiver getReceiver();

    public synchronized void startReceiving() {
        if (!start) {
            start = true;
        } else {
            throw MESSAGES.alreadyStarted();
        }

        channel.receiveMessage(getReceiver());
    }

    public String getName() {
        return name;
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
        return;
        //channel.awaitClosed();
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

    public void closeAsync() {
        channel.closeAsync();
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
        channel.close();
    }

    @Override
    public Connection getConnection() {
        return channel.getConnection();
    }


}
