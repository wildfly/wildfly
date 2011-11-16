/*
 * JBoss, Home of Professional Open Source
 * Copyright 2011 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU Lesser General Public License, v. 2.1.
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License,
 * v.2.1 along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA  02110-1301, USA.
 */
package org.jboss.as.protocol.mgmt;

import static org.jboss.as.protocol.ProtocolLogger.ROOT_LOGGER;

import java.io.IOException;

import org.jboss.as.protocol.ProtocolChannel;
import org.jboss.marshalling.Marshalling;
import org.jboss.marshalling.SimpleDataOutput;
import org.jboss.remoting3.Channel;
import org.xnio.IoUtils;

/**
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
public class ManagementChannel extends ProtocolChannel {

    private volatile Receiver receiver;

    public ManagementChannel(String name, Channel channel) {
        super(name, channel);
    }

    public void setReceiver(final ManagementMessageHandler handler) {
        final Receiver receiver = ManagementChannelReceiver.createDelegating(handler);
        setReceiver(receiver);
    }

    public void setReceiver(Receiver receiver) {
        this.receiver = receiver;
    }

    @Override
    protected Receiver getReceiver() {
        if(receiver == null) {
            throw new IllegalStateException();
        }
        return receiver;
    }

    public void sendByeBye() throws IOException {
        ROOT_LOGGER.tracef("Closing %s by sending bye bye", this);
        ManagementByeByeHeader byeByeHeader = new ManagementByeByeHeader(ManagementProtocol.VERSION);
        try {
            SimpleDataOutput out = new SimpleDataOutput(Marshalling.createByteOutput(writeMessage()));
            try {
                byeByeHeader.write(out);
            } finally {
                IoUtils.safeClose(out);
            }
        } catch (IOException ignore) {
            //
        } finally {
            ROOT_LOGGER.tracef("Invoking close on %s", this);
        }

    }
}