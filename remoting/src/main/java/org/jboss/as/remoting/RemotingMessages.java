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

package org.jboss.as.remoting;

import java.io.IOException;
import java.net.BindException;
import java.net.URISyntaxException;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.sasl.SaslException;

import org.jboss.as.controller.OperationFailedException;
import org.jboss.logging.Cause;
import org.jboss.logging.Message;
import org.jboss.logging.MessageBundle;
import org.jboss.logging.Messages;
import org.jboss.logging.Param;
import org.jboss.msc.service.StartException;

/**
 * Date: 05.11.2011
 *
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 * @author Tomaz Cerar
 */
@MessageBundle(projectCode = "JBAS")
public interface RemotingMessages {

    /**
     * The messages
     */
    RemotingMessages MESSAGES = Messages.getBundle(RemotingMessages.class);


    @Message(id = 17110, value = "Could not start channel listener")
    StartException couldNotStartChanelListener(@Cause Exception e);

    @Message(id = 17111, value = "Shutting down")
    IllegalStateException channelShuttingDown();

    @Message(id = 17112, value = "%s")
    StartException couldNotBindToSocket(String message, @Cause BindException e);

    @Message(id = 17113, value = "Failed to start service")
    StartException couldNotStart(@Cause Exception e);

    @Message(id = 17114, value = "Endpoint is null")
    IllegalStateException endpointEmpty();

    @Message(id = 17115, value = "Connection name cannot be null or empty")
    IllegalStateException connectionNameEmpty();

    @Message(id = 17116, value = "Connection URI cannot be null for connection named: %s")
    IllegalStateException connectionUriEmpty(String connectionName);

    @Message(id = 17117, value = "Outbound socket binding reference cannot be null or empty for connection named: %s")
    IllegalStateException outboundSocketBindingEmpty(String connectionName);

    @Message(id = 17118, value = "Destination URI cannot be null while creating a outbound remote connection service")
    IllegalStateException destinationUriEmpty();

    @Message(id = 17119, value = "A security realm has been specified but no supported mechanism identified")
    IllegalStateException noSupportingMechanismsForRealm();

    @Message(id = 17120, value = "Only %s user is acceptable.")
    SaslException onlyLocalUserIsAcceptable(String local);

    @Message(id = 17121, value = "ANONYMOUS mechanism so not expecting a callback")
    UnsupportedCallbackException anonymousMechanismNotExpected(@Param Callback current);

    @Message(id = 17122, value = "Unable to create tmp dir for auth tokens as file already exists.")
    StartException unableToCreateTempDirForAuthTokensFileExists();

    @Message(id = 17123, value = "Unable to create auth dir %s.")
    StartException unableToCreateAuthDir(String dir);

    @Message(id = 17124, value = "Could not register a connection provider factory for %s uri scheme")
    StartException couldNotRegisterConnectionProvider(String remoteUriScheme, @Cause IOException ioe);

    @Message(id = 17125, value = "Could not connect")
    RuntimeException couldNotConnect(@Cause URISyntaxException e);

    @Message(id = 17126, value = "Invalid QOP value: %s")
    IllegalStateException invalidQOPV(String qop);

    @Message(id = 17127, value = "Invalid Strength value: %s")
    IllegalStateException invalidStrength(String strengthValue);

    @Message(id = 17128, value = "Cannot create a valid URI from %s -- %s")
    OperationFailedException couldNotCreateURI(String uri, String msg);

    @Message(id = 17129, value = "")
    UnsupportedCallbackException unsupportedCallback(@Param Callback current);

}
