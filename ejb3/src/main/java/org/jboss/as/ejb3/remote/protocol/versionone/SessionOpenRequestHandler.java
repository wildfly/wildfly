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

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.concurrent.ExecutorService;

import org.jboss.as.ee.component.Component;
import org.jboss.as.ejb3.component.stateful.StatefulSessionComponent;
import org.jboss.as.ejb3.deployment.DeploymentRepository;
import org.jboss.as.ejb3.deployment.EjbDeploymentInformation;
import org.jboss.ejb.client.SessionID;
import org.jboss.ejb.client.remoting.PackedInteger;
import org.jboss.logging.Logger;
import org.jboss.marshalling.MarshallerFactory;
import org.jboss.remoting3.Channel;
import org.jboss.remoting3.MessageInputStream;
import org.xnio.IoUtils;

/**
 * @author Jaikiran Pai
 */
class SessionOpenRequestHandler extends EJBIdentifierBasedMessageHandler {

    private static final Logger logger = Logger.getLogger(SessionOpenRequestHandler.class);

    private static final byte HEADER_SESSION_OPEN_RESPONSE = 0x02;
    private static final byte HEADER_EJB_NOT_STATEFUL = 0x0D;

    private final ExecutorService executorService;
    private final MarshallerFactory marshallerFactory;

    SessionOpenRequestHandler(final DeploymentRepository deploymentRepository, final MarshallerFactory marshallerFactory,
                              final ExecutorService executorService) {
        super(deploymentRepository);
        this.marshallerFactory = marshallerFactory;
        this.executorService = executorService;
    }

    @Override
    public void processMessage(Channel channel, MessageInputStream messageInputStream) throws IOException {
        if (messageInputStream == null) {
            throw new IllegalArgumentException("Cannot read from null message inputstream");
        }
        final DataInputStream dataInputStream = new DataInputStream(messageInputStream);
        // read invocation id
        final short invocationId = dataInputStream.readShort();
        final String appName = dataInputStream.readUTF();
        final String moduleName = dataInputStream.readUTF();
        final String distinctName = dataInputStream.readUTF();
        final String beanName = dataInputStream.readUTF();

        final EjbDeploymentInformation ejbDeploymentInformation = this.findEJB(appName, moduleName, distinctName, beanName);
        if (ejbDeploymentInformation == null) {
            this.writeNoSuchEJBFailureMessage(channel, invocationId, appName, moduleName, distinctName, beanName, null);
            return;
        }
        final Component component = ejbDeploymentInformation.getEjbComponent();
        if (!(component instanceof StatefulSessionComponent)) {
            final String failureMessage = "EJB " + beanName + " is not a Stateful Session bean in app: " + appName + " module: " + moduleName + " distinct name:" + distinctName;
            this.writeInvocationFailure(channel, HEADER_EJB_NOT_STATEFUL, invocationId, failureMessage);
            return;
        }
        final StatefulSessionComponent statefulSessionComponent = (StatefulSessionComponent) component;
        // generate the session id and write out the response on a separate thread
        executorService.submit(new SessionIDGeneratorTask(statefulSessionComponent, channel, invocationId));

    }

    private void writeSessionId(final Channel channel, final short invocationId, final SessionID sessionID) throws IOException {
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
        } finally {
            dataOutputStream.close();
        }
    }

    /**
     * Task for generation a session id when a session open request is received
     */
    private class SessionIDGeneratorTask implements Runnable {

        private final StatefulSessionComponent statefulSessionComponent;
        private final Channel channel;
        private final short invocationId;


        SessionIDGeneratorTask(final StatefulSessionComponent statefulSessionComponent, final Channel channel, final short invocationId) {
            this.statefulSessionComponent = statefulSessionComponent;
            this.invocationId = invocationId;
            this.channel = channel;
        }

        @Override
        public void run() {
            final SessionID sessionID;
            try {
                try {
                    sessionID = statefulSessionComponent.createSession();
                } catch (Throwable t) {
                    SessionOpenRequestHandler.this.writeException(channel, SessionOpenRequestHandler.this.marshallerFactory, invocationId, t, null);
                    return;
                }
                SessionOpenRequestHandler.this.writeSessionId(channel, invocationId, sessionID);
            } catch (IOException ioe) {
                logger.error("IOException while generating session id for invocation id: " + invocationId + " on channel " + channel, ioe);
                // close the channel
                IoUtils.safeClose(this.channel);
                return;
            }
        }
    }
}