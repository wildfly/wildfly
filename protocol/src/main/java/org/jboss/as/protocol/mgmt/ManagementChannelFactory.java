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

import org.jboss.as.protocol.ProtocolChannelFactory;
import org.jboss.remoting3.Channel;

/**
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 * @version $Revision: 1.1 $
 */
public class ManagementChannelFactory extends ProtocolChannelFactory <ManagementChannel> {

    private final Channel.Receiver receiver;

    public ManagementChannelFactory(Channel.Receiver receiver) {
        this.receiver = receiver;
    }

    public ManagementChannel create(String name, Channel channel) {
        ManagementChannel createdChannel = new ManagementChannel(name, channel);
        if(receiver != null) {
            createdChannel.setReceiver(receiver);
        }
        return createdChannel;
    }

}
