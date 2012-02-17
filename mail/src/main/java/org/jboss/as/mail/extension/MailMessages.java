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

package org.jboss.as.mail.extension;

import org.jboss.logging.Cause;
import org.jboss.logging.Message;
import org.jboss.logging.MessageBundle;
import org.jboss.logging.Messages;
import org.jboss.msc.service.StartException;

import java.net.UnknownHostException;

/**
 * Date: 05.11.2011
 *
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
@MessageBundle(projectCode = "JBAS")
interface MailMessages {

    /**
     * The messages
     */
    MailMessages MESSAGES = Messages.getBundle(MailMessages.class);

    /**
     * Creates an exception indicating the outgoing socket binding, represented by the {@code outgoingSocketBindingRef}
     * parameter, could not be found.
     *
     * @param outgoingSocketBindingRef the name of the socket binding configuration.
     * @return a {@link StartException} for the error.
     */
    @Message(id = 15450, value = "No outbound socket binding configuration '%s' is available.")
    StartException outboundSocketBindingNotAvailable(String outgoingSocketBindingRef);

    /**
     * Creates an exception indicating that the destination address of the outgoing socket binding, that the
     * mail service depends on, could not be resolved
     *
     * @param outgoingSocketBindingRef the name of the socket binding configuration.
     * @return a {@link RuntimeException} for the error.
     */
    @Message(id = 15451, value = "Unknown host for outbound socket binding configuration '%s'.")
    RuntimeException unknownOutboundSocketBindingDestination(@Cause UnknownHostException cause, String outgoingSocketBindingRef);
}
