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

package org.jboss.as.controller.remote;

import org.jboss.as.controller.client.Notification;
import org.jboss.as.controller.client.NotificationHandler;
import org.jboss.as.controller.client.impl.ModelControllerProtocol;
import org.jboss.as.protocol.StreamUtils;
import org.jboss.as.protocol.mgmt.FlushableDataOutput;
import org.jboss.as.protocol.mgmt.ManagementChannelAssociation;
import org.jboss.as.protocol.mgmt.ManagementProtocol;
import org.jboss.as.protocol.mgmt.ManagementRequestHeader;
import org.jboss.as.protocol.mgmt.ProtocolUtils;
import org.jboss.remoting3.MessageOutputStream;

/**
 * A proxy to the notification handler on the remote caller.
 *
 * @author <a href="http://jmesnil.net/">Jeff Mesnil</a> (c) 2013 Red Hat, inc
 */
class NotificationHandlerProxy implements NotificationHandler {

    final ManagementChannelAssociation channelAssociation;
    final int batchId;

    public NotificationHandlerProxy(final ManagementChannelAssociation channelAssociation, final int batchId) {
        this.channelAssociation = channelAssociation;
        this.batchId = batchId;
    }

    @Override
    public void handleNotification(Notification notification) {
        try {
            // we just write the response to the client based on the batchID
            final MessageOutputStream os = channelAssociation.getChannel().writeMessage();
            try {
                final FlushableDataOutput output = ProtocolUtils.wrapAsDataOutput(os);
                final ManagementRequestHeader header = new ManagementRequestHeader(ManagementProtocol.VERSION, -1, batchId, ModelControllerProtocol.HANDLE_NOTIFICATION_REQUEST);
                header.write(output);
                notification.toModelNode().writeExternal(output);
                output.writeByte(ManagementProtocol.REQUEST_END);
                output.close();
            } finally {
                StreamUtils.safeClose(os);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        NotificationHandlerProxy that = (NotificationHandlerProxy) o;

        if (batchId != that.batchId) return false;
        if (channelAssociation != null ? !channelAssociation.equals(that.channelAssociation) : that.channelAssociation != null)
            return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = channelAssociation != null ? channelAssociation.hashCode() : 0;
        result = 31 * result + batchId;
        return result;
    }
}
