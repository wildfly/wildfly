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

package org.jboss.as.ejb3.remote.protocol.versionone;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.IOException;

import org.jboss.ejb.client.remoting.PackedInteger;
import org.jboss.ejb.client.remoting.RemotingAttachments;
import org.jboss.remoting3.Channel;


/**
 * User: jpai
 */
abstract class AbstractMessageHandler implements MessageHandler {

    protected final String marshallingStrategy;

    protected static final byte HEADER_NO_SUCH_EJB_FAILURE = 0x0A;
    protected static final byte HEADER_NO_SUCH_EJB_METHOD_FAILURE = 0x0B;
    protected static final byte HEADER_SESSION_NOT_ACTIVE_FAILURE = 0x0C;
    private static final byte HEADER_INVOCATION_EXCEPTION = 0x06;


    AbstractMessageHandler(final String marshallingStrategy) {
        this.marshallingStrategy = marshallingStrategy;
    }

    protected RemotingAttachments readAttachments(final DataInput input) throws IOException {
        int numAttachments = input.readByte();
        if (numAttachments == 0) {
            return null;
        }
        final RemotingAttachments attachments = new RemotingAttachments();
        for (int i = 0; i < numAttachments; i++) {
            // read attachment id
            final short attachmentId = input.readShort();
            // read attachment data length
            final int dataLength = PackedInteger.readPackedInteger(input);
            // read the data
            final byte[] data = new byte[dataLength];
            input.readFully(data);

            attachments.putPayloadAttachment(attachmentId, data);
        }
        return attachments;
    }

    protected void writeAttachments(final DataOutput output, final RemotingAttachments attachments) throws IOException {
        if (attachments == null) {
            output.writeByte(0);
            return;
        }
        // write attachment count
        output.writeByte(attachments.size());
        for (RemotingAttachments.RemotingAttachment attachment : attachments.entries()) {
            // write attachment id
            output.writeShort(attachment.getKey());
            final byte[] data = attachment.getValue();
            // write data length
            PackedInteger.writePackedInteger(output, data.length);
            // write the data
            output.write(data);

        }
    }

    protected void writeException(final Channel channel, final short invocationId, final Throwable t, final RemotingAttachments attachments) throws IOException {
        final DataOutputStream outputStream = new DataOutputStream(channel.writeMessage());
        try {
            // write the header
            outputStream.write(HEADER_INVOCATION_EXCEPTION);
            // write the invocation id
            outputStream.writeShort(invocationId);
            // write the attachments
            this.writeAttachments(outputStream, attachments);
            // write out the exception
            final Marshaller marshaller = MarshallerFactory.createMarshaller(this.marshallingStrategy);
            marshaller.start(outputStream);
            marshaller.writeObject(t);
            marshaller.finish();
        } finally {
            outputStream.close();
        }
    }

    protected void writeInvocationFailure(final Channel channel, final byte messageHeader, final short invocationId, final String failureMessage) throws IOException {
        final DataOutputStream dataOutputStream = new DataOutputStream(channel.writeMessage());
        try {
            // write header
            dataOutputStream.writeByte(messageHeader);
            // write invocation id
            dataOutputStream.writeShort(invocationId);
            // write the failure message
            dataOutputStream.writeUTF(failureMessage);
        } finally {
            dataOutputStream.close();
        }

    }

    protected void writeNoSuchEJBFailureMessage(final Channel channel, final short invocationId, final String appName, final String moduleName,
                                                final String distinctname, final String beanName, final String viewClassName) throws IOException {
        final StringBuffer sb = new StringBuffer("No such EJB[");
        sb.append("appname=").append(appName).append(", ");
        sb.append("modulename=").append(moduleName).append(", ");
        sb.append("distinctname=").append(distinctname).append(", ");
        sb.append("beanname=").append(beanName).append(", ");
        sb.append("viewclassname=").append(viewClassName).append("]");
        this.writeInvocationFailure(channel, HEADER_NO_SUCH_EJB_FAILURE, invocationId, sb.toString());
    }

    protected void writeSessionNotActiveFailureMessage(final Channel channel, final short invocationId, final String appName, final String moduleName,
                                                       final String distinctname, final String beanName) throws IOException {
        final StringBuffer sb = new StringBuffer("Session not active for EJB[");
        sb.append("appname=").append(appName).append(", ");
        sb.append("modulename=").append(moduleName).append(", ");
        sb.append("distinctname=").append(distinctname).append(", ");
        sb.append("beanname=").append(beanName).append("]");
        this.writeInvocationFailure(channel, HEADER_SESSION_NOT_ACTIVE_FAILURE, invocationId, sb.toString());
    }

    protected void writeNoSuchEJBMethodFailureMessage(final Channel channel, final short invocationId, final String appName, final String moduleName,
                                                      final String distinctname, final String beanName, final String viewClassName,
                                                      final String methodName, final String[] methodParamTypes) throws IOException {
        final StringBuffer sb = new StringBuffer("No such method ");
        sb.append(methodName).append("(");
        if (methodParamTypes != null) {
            for (int i = 0; i < methodParamTypes.length; i++) {
                if (i != 0) {
                    sb.append(",");
                }
                sb.append(methodParamTypes[i]);
            }
        }
        sb.append(") on EJB[");
        sb.append("appname=").append(appName).append(", ");
        sb.append("modulename=").append(moduleName).append(", ");
        sb.append("distinctname=").append(distinctname).append(", ");
        sb.append("beanname=").append(beanName).append(", ");
        sb.append("viewclassname=").append(viewClassName).append("]");
        this.writeInvocationFailure(channel, HEADER_NO_SUCH_EJB_METHOD_FAILURE, invocationId, sb.toString());
    }

}
