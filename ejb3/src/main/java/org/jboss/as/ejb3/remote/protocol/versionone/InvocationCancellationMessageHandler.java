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

import org.jboss.as.ejb3.component.interceptors.CancellationFlag;
import org.jboss.as.ejb3.remote.RemoteAsyncInvocationCancelStatusService;
import org.jboss.logging.Logger;
import org.jboss.remoting3.MessageInputStream;

import java.io.DataInputStream;
import java.io.IOException;

/**
 * Handles a message corresponding to a invocation cancellation request
 *
 * @author Jaikiran Pai
 */
public class InvocationCancellationMessageHandler extends AbstractMessageHandler {

    private static final Logger logger = Logger.getLogger(InvocationCancellationMessageHandler.class);

    private final RemoteAsyncInvocationCancelStatusService remoteAsyncInvocationCancelStatus;

    public InvocationCancellationMessageHandler(final RemoteAsyncInvocationCancelStatusService asyncInvocationCancelStatus) {
        this.remoteAsyncInvocationCancelStatus = asyncInvocationCancelStatus;
    }

    @Override
    public void processMessage(ChannelAssociation channelAssociation, MessageInputStream messageInputStream) throws IOException {
        final DataInputStream input = new DataInputStream(messageInputStream);
        // read the id of the invocation which needs to be cancelled
        final short invocationToCancel = input.readShort();
        // get the cancellation flag (if any) for the invocation id
        final CancellationFlag cancellationFlag = this.remoteAsyncInvocationCancelStatus.getCancelStatus(invocationToCancel);
        if (cancellationFlag == null) {
            return;
        }
        // mark it as cancelled
        cancellationFlag.set(true);
        logger.debug("Invocation with id " + invocationToCancel + " has been marked as cancelled, as requested");
    }
}
