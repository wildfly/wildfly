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

import org.jboss.as.ee.component.Component;
import org.jboss.as.ejb3.component.stateful.StatefulSessionComponent;
import org.jboss.as.ejb3.deployment.DeploymentRepository;
import org.jboss.as.ejb3.deployment.EjbDeploymentInformation;
import org.jboss.ejb.client.SessionID;
import org.jboss.ejb.client.remoting.PackedInteger;
import org.jboss.ejb.client.remoting.RemotingAttachments;
import org.jboss.remoting3.Channel;
import org.jboss.remoting3.MessageInputStream;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

/**
 * User: jpai
 */
class SessionOpenRequestHandler extends AbstractMessageHandler {

    private static final byte HEADER_SESSION_OPEN_RESPONSE = 0x02;

    SessionOpenRequestHandler(final DeploymentRepository deploymentRepository, final String marshallingStrategy) {
        super(deploymentRepository, marshallingStrategy);
    }

    @Override
    public void processMessage(Channel channel, MessageInputStream messageInputStream) throws IOException {
        if (messageInputStream == null) {
            throw new IllegalArgumentException("Cannot read from null message inputstream");
        }
        final DataInputStream dataInputStream = new DataInputStream(messageInputStream);
        // read invocation id
        final short invocationId = dataInputStream.readShort();
        String appName = dataInputStream.readUTF();
        if (appName.isEmpty()) {
            appName = null;
        }
        final String moduleName = dataInputStream.readUTF();
        final String distinctName = dataInputStream.readUTF();
        final String beanName = dataInputStream.readUTF();
        final String viewClassName = dataInputStream.readUTF();

        final EjbDeploymentInformation ejbDeploymentInformation = this.findEJB(appName, moduleName, distinctName, beanName);
        if (ejbDeploymentInformation == null) {
            this.writeNoSuchEJBFailureMessage(channel, invocationId, appName, moduleName, distinctName, beanName, viewClassName);
            return;
        }
        final Component component = ejbDeploymentInformation.getEjbComponent();
        if (!(component instanceof StatefulSessionComponent)) {
            final String failureMessage = "EJB " + beanName + " is not a Stateful Session bean in app: " + appName + " module: " + moduleName + " distinct name:" + distinctName;
            this.writeInvocationFailure(channel, invocationId, failureMessage);
            return;
        }
        // read the attachments
        final RemotingAttachments attachments = this.readAttachments(dataInputStream);

        final StatefulSessionComponent statefulSessionComponent = (StatefulSessionComponent) component;
        // TODO: Session generation and writing the result, should happen on a separate thread to
        // let the channel to be freed for further incoming message receipt(s)
        final SessionID sessionID = statefulSessionComponent.createSession();

        try {
            this.writeSessionId(channel, invocationId, sessionID, attachments);
        } catch (IOException ioe) {
            // write out invocation failure
            this.writeInvocationFailure(channel, invocationId, "Server failed to send back session id");
        }
    }

    private void writeSessionId(final Channel channel, final short invocationId, final SessionID sessionID, final RemotingAttachments attachments) throws IOException {
        final byte[] sessionIdBytes = sessionID.getEncodedForm();
        final DataOutputStream dataOutputStream = new DataOutputStream(channel.writeMessage());
        try {
            // write out header
            dataOutputStream.writeByte(HEADER_SESSION_OPEN_RESPONSE);
            // write out invocation id
            dataOutputStream.writeShort(invocationId);
            // session id byte length
            PackedInteger.writePackedInteger(dataOutputStream, sessionIdBytes.length);
            // write out the session id bytes
            dataOutputStream.write(sessionIdBytes);
            // write out the attachments
            this.writeAttachments(dataOutputStream, attachments);
        } finally {
            dataOutputStream.close();
        }
    }
}
