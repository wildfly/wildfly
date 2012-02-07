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

import org.jboss.as.controller.client.MessageSeverity;
import org.jboss.as.controller.client.OperationMessageHandler;
import org.jboss.as.controller.client.impl.ModelControllerProtocol;
import org.jboss.as.protocol.StreamUtils;
import org.jboss.as.protocol.mgmt.FlushableDataOutput;
import org.jboss.as.protocol.mgmt.ManagementChannelAssociation;
import org.jboss.as.protocol.mgmt.ManagementProtocol;
import org.jboss.as.protocol.mgmt.ManagementRequestHeader;
import org.jboss.as.protocol.mgmt.ProtocolUtils;
import org.jboss.remoting3.MessageOutputStream;

/**
 * A proxy to the operation message handler on the remote caller.
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
class OperationMessageHandlerProxy implements OperationMessageHandler {

    final ManagementChannelAssociation channelAssociation;
    final int batchId;

    public OperationMessageHandlerProxy(final ManagementChannelAssociation channelAssociation, final int batchId) {
        this.channelAssociation = channelAssociation;
        this.batchId = batchId;
    }

    @Override
    public void handleReport(final MessageSeverity severity, final String message) {
        if(true) {
            return;
        }
        try {
            // We don't expect any response, so just write the message
            final MessageOutputStream os = channelAssociation.getChannel().writeMessage();
            try {
                final FlushableDataOutput output = ProtocolUtils.wrapAsDataOutput(os);
                final ManagementRequestHeader header = new ManagementRequestHeader(ManagementProtocol.VERSION, -1, batchId, ModelControllerProtocol.HANDLE_REPORT_REQUEST);
                header.write(output);
                output.write(ModelControllerProtocol.PARAM_MESSAGE_SEVERITY);
                output.writeUTF(severity.toString());
                output.write(ModelControllerProtocol.PARAM_MESSAGE);
                output.writeUTF(message);
                output.writeByte(ManagementProtocol.REQUEST_END);
                output.close();
            } finally {
                StreamUtils.safeClose(os);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
